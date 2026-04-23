import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';

import {
  AdminApiService,
  RoleResponse,
  UserResponse,
  UserRoleResponse,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-user-roles-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DialogModule, DropdownModule, ButtonModule, TagModule],
  template: `
    <p-dialog
      [header]="headerText()"
      [visible]="visible()"
      (visibleChange)="close($event)"
      [modal]="true"
      [style]="{ width: '520px' }"
      [draggable]="false"
    >
      <div class="section">
        <strong>Přiřazené role</strong>
        <div class="roles">
          @if ((userRoles() ?? []).length === 0) {
            <span class="muted">Žádné role.</span>
          } @else {
            @for (ur of userRoles(); track ur.roleCode) {
              <p-tag
                [value]="roleLabel(ur.roleCode)"
                severity="info"
                styleClass="role-tag"
              >
                <span class="tag-inner">
                  {{ roleLabel(ur.roleCode) }}
                  <p-button
                    icon="pi pi-times"
                    [text]="true"
                    severity="secondary"
                    size="small"
                    styleClass="remove-btn"
                    [disabled]="busy()"
                    (onClick)="revoke(ur.roleCode)"
                  />
                </span>
              </p-tag>
            }
          }
        </div>
      </div>

      <div class="section">
        <strong>Přidat roli</strong>
        <div class="add-row">
          <p-dropdown
            [options]="grantableOptions()"
            optionLabel="label"
            optionValue="code"
            placeholder="Vyberte roli"
            [(ngModel)]="selectedToGrant"
            appendTo="body"
            [disabled]="busy() || grantableOptions().length === 0"
          />
          <p-button
            label="Přidat"
            icon="pi pi-plus"
            [disabled]="!selectedToGrant || busy()"
            [loading]="granting()"
            (onClick)="grant()"
          />
        </div>
      </div>

      <ng-template pTemplate="footer">
        <p-button label="Zavřít" severity="secondary" [text]="true" (onClick)="close(false)" />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    `
      .section { margin-bottom: 1.25rem; }
      .section strong { display: block; margin-bottom: 0.5rem; color: #52525b; font-size: 0.9rem; }
      .roles { display: flex; flex-wrap: wrap; gap: 0.5rem; }
      :host ::ng-deep .role-tag { padding: 0.25rem 0.5rem; }
      .tag-inner { display: inline-flex; align-items: center; gap: 0.25rem; }
      :host ::ng-deep .remove-btn .p-button-icon { font-size: 0.7rem; }
      .add-row { display: flex; gap: 0.5rem; align-items: flex-start; }
      .add-row p-dropdown { flex: 1; }
      .muted { color: #71717a; font-size: 0.9rem; }
    `,
  ],
})
export class UserRolesDialogComponent {
  private readonly api = inject(AdminApiService);
  private readonly messages = inject(MessageService);

  readonly visible = input(false);
  readonly user = input.required<UserResponse>();
  readonly visibleChange = output<boolean>();

  protected readonly allRoles = signal<RoleResponse[]>([]);
  protected readonly userRoles = signal<UserRoleResponse[] | undefined>(undefined);
  protected readonly granting = signal(false);
  protected readonly revoking = signal<string | null>(null);
  protected selectedToGrant: string | null = null;

  protected readonly busy = computed(() => this.granting() || this.revoking() !== null);

  protected readonly grantableOptions = computed(() => {
    const assigned = new Set((this.userRoles() ?? []).map((r) => r.roleCode));
    return this.allRoles()
        .filter((r) => !assigned.has(r.code))
        .map((r) => ({ code: r.code, label: r.displayName }));
  });

  protected readonly headerText = computed(() => {
    const u = this.user();
    return `Role pro ${u.displayName || u.username}`;
  });

  constructor() {
    effect(() => {
      if (this.visible()) {
        this.loadAll();
      }
    });
  }

  private loadAll(): void {
    this.userRoles.set(undefined);
    forkJoin({
      all: this.api.listRoles(),
      assigned: this.api.listUserRoles(this.user().id),
    }).subscribe({
      next: ({ all, assigned }) => {
        this.allRoles.set(all);
        this.userRoles.set(assigned);
      },
      error: (err: ProblemDetail) => {
        this.messages.add({
          severity: 'error',
          summary: 'Načtení rolí selhalo',
          detail: err.detail ?? err.title,
        });
        this.userRoles.set([]);
      },
    });
  }

  protected roleLabel(code: string): string {
    return this.allRoles().find((r) => r.code === code)?.displayName ?? code;
  }

  protected grant(): void {
    if (!this.selectedToGrant || this.busy()) return;
    const code = this.selectedToGrant;
    this.granting.set(true);
    this.api.grantRole(this.user().id, code).subscribe({
      next: () => {
        this.granting.set(false);
        this.selectedToGrant = null;
        this.messages.add({
          severity: 'success',
          summary: 'Role přidána',
          detail: this.roleLabel(code),
        });
        this.loadAll();
      },
      error: (err: ProblemDetail) => {
        this.granting.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Přidání role selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  protected revoke(code: string): void {
    if (this.busy()) return;
    this.revoking.set(code);
    this.api.revokeRole(this.user().id, code).subscribe({
      next: () => {
        this.revoking.set(null);
        this.messages.add({
          severity: 'success',
          summary: 'Role odebrána',
          detail: this.roleLabel(code),
        });
        this.loadAll();
      },
      error: (err: ProblemDetail) => {
        this.revoking.set(null);
        this.messages.add({
          severity: 'error',
          summary: 'Odebrání role selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  protected close(next: boolean): void {
    this.visibleChange.emit(next);
  }
}
