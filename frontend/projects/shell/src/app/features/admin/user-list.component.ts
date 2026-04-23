import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ChipModule } from 'primeng/chip';
import { MessageService } from 'primeng/api';
import { toSignal } from '@angular/core/rxjs-interop';

import { AdminApiService, UserResponse } from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

import { UserRolesDialogComponent } from './user-roles-dialog.component';

@Component({
  selector: 'tm-user-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    TableModule,
    ButtonModule,
    TagModule,
    ChipModule,
    UserRolesDialogComponent,
  ],
  template: `
    <div class="page-head">
      <h1>Uživatelé</h1>
      <p-button
        label="Synchronizovat z Keycloak"
        icon="pi pi-sync"
        [loading]="syncing()"
        (onClick)="sync()"
      />
    </div>

    <p-table
      [value]="rows() ?? []"
      [loading]="rows() === undefined"
      dataKey="id"
      styleClass="p-datatable-sm"
      [paginator]="true"
      [rows]="25"
    >
      <ng-template pTemplate="header">
        <tr>
          <th>Uživatel</th>
          <th>E-mail</th>
          <th>Stav</th>
          <th>Naposledy sync</th>
          <th></th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-u>
        <tr>
          <td>
            <div class="username">{{ u.displayName || u.username }}</div>
            <div class="meta">{{ u.username }}</div>
          </td>
          <td>{{ u.email }}</td>
          <td>
            <p-tag
              [value]="u.active ? 'Aktivní' : 'Deaktivovaný'"
              [severity]="u.active ? 'success' : 'secondary'"
            />
          </td>
          <td>{{ u.lastSyncedAt | date: 'short' }}</td>
          <td>
            <p-button
              icon="pi pi-shield"
              label="Role"
              [text]="true"
              size="small"
              (onClick)="openRoles(u)"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="5" class="empty">
            Žádní uživatelé. Klikněte na "Synchronizovat z Keycloak".
          </td>
        </tr>
      </ng-template>
    </p-table>

    @if (selectedUser(); as user) {
      <tm-user-roles-dialog
        [visible]="rolesDialogVisible()"
        (visibleChange)="rolesDialogVisible.set($event)"
        [user]="user"
      />
    }
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .username { font-weight: 500; }
      .meta { color: #71717a; font-size: 0.8rem; }
      .empty { text-align: center; color: #71717a; padding: 2rem; }
    `,
  ],
})
export class UserListComponent {
  private readonly api = inject(AdminApiService);
  private readonly messages = inject(MessageService);

  protected readonly syncing = signal(false);
  protected readonly rows = signal<UserResponse[] | undefined>(undefined);
  protected readonly selectedUser = signal<UserResponse | null>(null);
  protected readonly rolesDialogVisible = signal(false);

  constructor() {
    this.refresh();
  }

  protected refresh(): void {
    this.rows.set(undefined);
    this.api.listUsers().subscribe({
      next: (users) => this.rows.set(users),
      error: (err: ProblemDetail) => {
        this.rows.set([]);
        this.messages.add({
          severity: 'error',
          summary: 'Načtení uživatelů selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  protected sync(): void {
    this.syncing.set(true);
    this.api.syncUsers().subscribe({
      next: (res) => {
        this.syncing.set(false);
        this.messages.add({
          severity: 'success',
          summary: 'Synchronizace hotová',
          detail: `${res.created} vytvořeno, ${res.updated} aktualizováno, celkem ${res.totalFetched}.`,
        });
        this.refresh();
      },
      error: (err: ProblemDetail) => {
        this.syncing.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Synchronizace selhala',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  protected openRoles(user: UserResponse): void {
    this.selectedUser.set(user);
    this.rolesDialogVisible.set(true);
  }
}
