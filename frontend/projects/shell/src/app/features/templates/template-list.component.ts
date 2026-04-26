import { ChangeDetectionStrategy, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { toSignal } from '@angular/core/rxjs-interop';

import { TemplateApiService, TemplateBundle, TemplateResponse } from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

import { CreateTemplateDialogComponent } from './create-template-dialog.component';

@Component({
  selector: 'tm-template-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    TableModule,
    ButtonModule,
    TagModule,
    CreateTemplateDialogComponent,
  ],
  template: `
    <div class="page-head">
      <h1>Šablony</h1>
      <div class="actions">
        <p-button
          label="Importovat"
          icon="pi pi-upload"
          severity="secondary"
          [text]="true"
          [loading]="importing()"
          (onClick)="fileInput.click()"
        />
        <input
          #fileInput
          type="file"
          accept="application/json,.json"
          hidden
          (change)="onFileSelected($event)"
        />
        <p-button
          label="Nová šablona"
          icon="pi pi-plus"
          (onClick)="dialogVisible.set(true)"
        />
      </div>
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
            <a [routerLink]="['/templates', row.id, 'edit']">{{ row.name }}</a>
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
          <td class="row-actions">
            <p-button
              icon="pi pi-download"
              [text]="true"
              severity="secondary"
              ariaLabel="Exportovat"
              pTooltip="Exportovat jako JSON"
              (onClick)="exportTemplate(row)"
            />
            <p-button
              icon="pi pi-pencil"
              [text]="true"
              severity="secondary"
              [routerLink]="['/templates', row.id, 'edit']"
            />
          </td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="5" class="empty">Žádné šablony — vytvořte první.</td>
        </tr>
      </ng-template>
    </p-table>

    <tm-create-template-dialog
      [visible]="dialogVisible()"
      (visibleChange)="dialogVisible.set($event)"
      (created)="onCreated($event)"
    />
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
      .actions { display: flex; gap: 0.5rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      .slug { color: #71717a; font-size: 0.8rem; }
      .empty { text-align: center; color: #71717a; padding: 2rem; }
      .row-actions { display: flex; gap: 0.25rem; justify-content: flex-end; }
      a { color: #3730a3; text-decoration: none; }
      a:hover { text-decoration: underline; }
    `,
  ],
})
export class TemplateListComponent {
  private readonly api = inject(TemplateApiService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  protected readonly fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');
  protected readonly dialogVisible = signal(false);
  protected readonly importing = signal(false);
  protected readonly rows = toSignal<TemplateResponse[] | undefined>(
    this.api.list(),
    { initialValue: undefined },
  );

  protected onCreated(created: TemplateResponse): void {
    this.messages.add({
      severity: 'success',
      summary: 'Šablona vytvořena',
      detail: created.name,
    });
    this.router.navigate(['/templates', created.id, 'edit']);
  }

  protected exportTemplate(row: TemplateResponse): void {
    this.api.exportBundle(row.id).subscribe({
      next: (bundle) => triggerJsonDownload(bundle, `${row.slug}-export.json`),
      error: (err: ProblemDetail) =>
        this.messages.add({
          severity: 'error',
          summary: 'Export selhal',
          detail: err.detail ?? err.title,
        }),
    });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // Reset so picking the same file twice in a row still re-fires change.
    input.value = '';
    if (!file) return;

    this.importing.set(true);
    file
      .text()
      .then((text) => {
        let bundle: TemplateBundle;
        try {
          bundle = JSON.parse(text) as TemplateBundle;
        } catch {
          this.importing.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Neplatný soubor',
            detail: 'Soubor není validní JSON.',
          });
          return;
        }
        if (!bundle.template?.slug || !Array.isArray(bundle.versions)) {
          this.importing.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Neplatný bundle',
            detail: 'JSON neodpovídá očekávané struktuře (chybí template.slug nebo versions).',
          });
          return;
        }
        this.api.importBundle(bundle).subscribe({
          next: (created) => {
            this.importing.set(false);
            this.messages.add({
              severity: 'success',
              summary: 'Šablona naimportována',
              detail: created.name,
            });
            this.router.navigate(['/templates', created.id, 'edit']);
          },
          error: (err: ProblemDetail) => {
            this.importing.set(false);
            this.messages.add({
              severity: 'error',
              summary: 'Import selhal',
              detail: err.detail ?? err.title,
            });
          },
        });
      })
      .catch(() => {
        this.importing.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Nepodařilo se přečíst soubor',
        });
      });
  }
}

function triggerJsonDownload(bundle: TemplateBundle, filename: string): void {
  const blob = new Blob([JSON.stringify(bundle, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
