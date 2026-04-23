import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { toSignal } from '@angular/core/rxjs-interop';

import { ClauseApiService, ClauseResponse } from '@tmpmgmt/api-client';

import { CreateClauseDialogComponent } from './create-clause-dialog.component';

@Component({
  selector: 'tm-clause-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    TableModule,
    ButtonModule,
    TagModule,
    CreateClauseDialogComponent,
  ],
  template: `
    <div class="page-head">
      <h1>Knihovna doložek</h1>
      <p-button
        label="Nová doložka"
        icon="pi pi-plus"
        (onClick)="dialogVisible.set(true)"
      />
    </div>

    <p-table
      [value]="rows() ?? []"
      [loading]="rows() === undefined"
      dataKey="id"
      styleClass="p-datatable-sm"
      [paginator]="true"
      [rows]="20"
    >
      <ng-template pTemplate="header">
        <tr>
          <th>Název</th>
          <th>Kategorie</th>
          <th>Stav</th>
          <th>Naposledy upraveno</th>
          <th></th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-row>
        <tr>
          <td>
            <a [routerLink]="['/clauses', row.id, 'edit']">{{ row.name }}</a>
            <div class="slug">{{ row.slug }}</div>
          </td>
          <td>{{ row.category || '—' }}</td>
          <td>
            <p-tag
              [value]="row.status"
              [severity]="row.status === 'ACTIVE' ? 'success' : 'secondary'"
            />
          </td>
          <td>{{ row.updatedAt | date: 'medium' }}</td>
          <td>
            <p-button
              icon="pi pi-pencil"
              [text]="true"
              severity="secondary"
              [routerLink]="['/clauses', row.id, 'edit']"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="5" class="empty">Žádné doložky — vytvořte první.</td>
        </tr>
      </ng-template>
    </p-table>

    <tm-create-clause-dialog
      [visible]="dialogVisible()"
      (visibleChange)="dialogVisible.set($event)"
      (created)="onCreated($event)"
    />
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .slug { color: #71717a; font-size: 0.8rem; }
      .empty { text-align: center; color: #71717a; padding: 2rem; }
      a { color: #3730a3; text-decoration: none; }
      a:hover { text-decoration: underline; }
    `,
  ],
})
export class ClauseListComponent {
  private readonly api = inject(ClauseApiService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  protected readonly dialogVisible = signal(false);
  protected readonly rows = toSignal<ClauseResponse[] | undefined>(
    this.api.list(),
    { initialValue: undefined },
  );

  protected onCreated(created: ClauseResponse): void {
    this.messages.add({
      severity: 'success',
      summary: 'Doložka vytvořena',
      detail: created.name,
    });
    this.router.navigate(['/clauses', created.id, 'edit']);
  }
}
