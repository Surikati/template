import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';

import {
  AdminApiService,
  AuditApiService,
  AuditEventResponse,
  UserResponse,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

interface ActorOption {
  label: string;
  value: string | null;
}

@Component({
  selector: 'tm-audit-viewer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    JsonPipe,
    FormsModule,
    TableModule,
    ButtonModule,
    DropdownModule,
    InputTextModule,
    InputNumberModule,
    TagModule,
  ],
  template: `
    <div class="page-head">
      <h1>Audit log</h1>
      <p-button
        label="Načíst"
        icon="pi pi-refresh"
        [loading]="loading()"
        (onClick)="refresh()"
      />
    </div>

    <form class="filters" (ngSubmit)="refresh()">
      <label>
        <span>Typ agregátu</span>
        <input
          pInputText
          [(ngModel)]="aggregateType"
          name="aggregateType"
          placeholder="např. template"
        />
      </label>
      <label>
        <span>ID agregátu</span>
        <input
          pInputText
          [(ngModel)]="aggregateId"
          name="aggregateId"
          placeholder="UUID nebo slug"
        />
      </label>
      <label>
        <span>Aktér</span>
        <p-dropdown
          [options]="actorOptions()"
          optionLabel="label"
          optionValue="value"
          [(ngModel)]="actorUserId"
          name="actorUserId"
          appendTo="body"
          [showClear]="true"
          placeholder="Všichni"
          [filter]="true"
          filterBy="label"
        />
      </label>
      <label>
        <span>Limit</span>
        <p-inputNumber
          [(ngModel)]="limit"
          name="limit"
          [min]="1"
          [max]="500"
          [showButtons]="false"
        />
      </label>
      <p-button label="Reset" severity="secondary" [text]="true" type="button" (onClick)="reset()" />
    </form>

    <p-table
      [value]="rows() ?? []"
      [loading]="loading()"
      dataKey="eventId"
      styleClass="p-datatable-sm"
      [paginator]="true"
      [rows]="50"
      [rowsPerPageOptions]="[25, 50, 100, 200]"
      [expandedRowKeys]="expanded()"
      (onRowExpand)="onRowExpand($event.data)"
      (onRowCollapse)="onRowCollapse($event.data)"
    >
      <ng-template pTemplate="header">
        <tr>
          <th style="width: 2.5rem"></th>
          <th>Čas</th>
          <th>Typ události</th>
          <th>Agregát</th>
          <th>Aktér</th>
          <th>Correlation</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row let-expanded="expanded">
        <tr>
          <td>
            <p-button
              [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
              [text]="true"
              severity="secondary"
              size="small"
              (onClick)="toggleRow(row)"
            />
          </td>
          <td><span class="time">{{ row.occurredAt | date: 'medium' }}</span></td>
          <td><p-tag [value]="row.eventType" severity="info" /></td>
          <td>
            <div class="agg-type">{{ row.aggregateType }}</div>
            <div class="agg-id">{{ row.aggregateId }}</div>
          </td>
          <td>
            @if (row.actorUserId) {
              <span class="actor">{{ actorLabel(row.actorUserId) }}</span>
            } @else {
              <span class="muted">—</span>
            }
          </td>
          <td>
            @if (row.correlationId) {
              <code class="corr">{{ row.correlationId }}</code>
            }
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="rowexpansion" let-row>
        <tr>
          <td colspan="6" class="payload">
            <pre>{{ row.payload | json }}</pre>
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr><td colspan="6" class="empty">Žádné události.</td></tr>
      </ng-template>
    </p-table>
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .filters {
        display: grid; grid-template-columns: 1fr 1fr 1fr 100px auto;
        gap: 0.75rem;
        align-items: end;
        background: #ffffff;
        padding: 0.75rem;
        border: 1px solid #e4e4e7;
        border-radius: 6px;
        margin-bottom: 1rem;
      }
      .filters label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.85rem; color: #52525b; }
      .filters p-dropdown, .filters p-inputNumber { width: 100%; }
      .time { white-space: nowrap; }
      .agg-type { font-weight: 500; }
      .agg-id { color: #71717a; font-size: 0.8rem; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
      .actor { font-size: 0.9rem; }
      .muted { color: #71717a; }
      .corr { font-size: 0.75rem; background: #f4f4f5; padding: 0.1rem 0.3rem; border-radius: 3px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
      .payload pre {
        background: #fafafa;
        padding: 0.75rem;
        border-radius: 4px;
        overflow: auto;
        max-height: 400px;
        font-size: 0.8rem;
        margin: 0;
      }
      .empty { text-align: center; color: #71717a; padding: 2rem; }
    `,
  ],
})
export class AuditViewerComponent {
  private readonly api = inject(AuditApiService);
  private readonly admin = inject(AdminApiService);
  private readonly messages = inject(MessageService);

  protected aggregateType = '';
  protected aggregateId = '';
  protected actorUserId: string | null = null;
  protected limit = 100;

  protected readonly rows = signal<AuditEventResponse[] | undefined>(undefined);
  protected readonly loading = signal(false);
  protected readonly expanded = signal<Record<string, boolean>>({});
  protected readonly users = signal<UserResponse[]>([]);

  protected readonly actorOptions = computed<ActorOption[]>(() => {
    const opts: ActorOption[] = this.users().map((u) => ({
      label: u.displayName || u.username,
      value: u.id,
    }));
    return opts;
  });

  private readonly userById = computed(() => {
    const map = new Map<string, UserResponse>();
    for (const u of this.users()) map.set(u.id, u);
    return map;
  });

  constructor() {
    this.admin.listUsers().subscribe({
      next: (list) => this.users.set(list),
      error: () => this.users.set([]),
    });
    this.refresh();
  }

  protected refresh(): void {
    this.loading.set(true);
    this.api
      .list({
        aggregateType: this.aggregateType.trim() || undefined,
        aggregateId: this.aggregateId.trim() || undefined,
        actorUserId: this.actorUserId ?? undefined,
        limit: this.limit,
      })
      .subscribe({
        next: (events) => {
          this.rows.set(events);
          this.loading.set(false);
        },
        error: (err: ProblemDetail) => {
          this.loading.set(false);
          this.rows.set([]);
          this.messages.add({
            severity: 'error',
            summary: 'Načtení auditu selhalo',
            detail: err.detail ?? err.title,
          });
        },
      });
  }

  protected reset(): void {
    this.aggregateType = '';
    this.aggregateId = '';
    this.actorUserId = null;
    this.limit = 100;
    this.refresh();
  }

  protected toggleRow(row: AuditEventResponse): void {
    this.expanded.update((m) => {
      const next = { ...m };
      if (next[row.eventId]) delete next[row.eventId];
      else next[row.eventId] = true;
      return next;
    });
  }

  protected onRowExpand(row: AuditEventResponse): void {
    this.expanded.update((m) => ({ ...m, [row.eventId]: true }));
  }

  protected onRowCollapse(row: AuditEventResponse): void {
    this.expanded.update((m) => {
      const next = { ...m };
      delete next[row.eventId];
      return next;
    });
  }

  protected actorLabel(id: string): string {
    const u = this.userById().get(id);
    return u ? u.displayName || u.username : id.slice(0, 8) + '…';
  }
}
