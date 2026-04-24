import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ChipsModule } from 'primeng/chips';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import { ProblemDetail } from '@tmpmgmt/core';

export interface MetadataPayload {
  name: string;
  description?: string;
  category?: string;
  tags?: string[];
}

/**
 * Reusable edit-metadata dialog for templates and clauses. Parent supplies an updater function
 * (e.g. {@code TemplateApiService.updateMetadata.bind(...)}) so the dialog stays decoupled
 * from any one API client.
 */
@Component({
  selector: 'tm-edit-metadata-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    InputTextareaModule,
    ChipsModule,
    ButtonModule,
  ],
  template: `
    <p-dialog
      [header]="header()"
      [visible]="visible()"
      (visibleChange)="visibleChange.emit($event)"
      [modal]="true"
      [style]="{ width: '520px' }"
      [draggable]="false"
      [resizable]="false"
    >
      <form [formGroup]="form" class="form" (ngSubmit)="submit()">
        <label>
          <span>Název</span>
          <input pInputText formControlName="name" autocomplete="off" />
        </label>
        <label>
          <span>Popis</span>
          <textarea pInputTextarea formControlName="description" rows="3"></textarea>
        </label>
        <label>
          <span>Kategorie</span>
          <input pInputText formControlName="category" autocomplete="off" />
        </label>
        <label>
          <span>Tagy <em>(Enter pro přidání)</em></span>
          <p-chips formControlName="tags" separator="," />
        </label>
      </form>

      <ng-template pTemplate="footer">
        <p-button
          label="Zrušit"
          severity="secondary"
          [text]="true"
          (onClick)="visibleChange.emit(false)"
        />
        <p-button
          label="Uložit"
          icon="pi pi-check"
          [disabled]="form.invalid || submitting()"
          [loading]="submitting()"
          (onClick)="submit()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [
    `
      .form { display: flex; flex-direction: column; gap: 1rem; padding-top: 0.5rem; }
      label { display: flex; flex-direction: column; gap: 0.25rem; }
      label span { font-size: 0.9rem; color: #52525b; }
      em { color: #a1a1aa; font-style: normal; font-size: 0.8rem; }
      :host ::ng-deep .p-chips, :host ::ng-deep .p-chips-multiple-container { width: 100%; }
    `,
  ],
})
export class EditMetadataDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  readonly visible = input(false);
  readonly header = input('Upravit metadata');
  readonly initial = input<MetadataPayload | null>(null);
  /** Function called on save; parent supplies the API call. Returns the updated entity. */
  readonly updater = input.required<(payload: MetadataPayload) => Observable<unknown>>();

  readonly visibleChange = output<boolean>();
  readonly saved = output<unknown>();

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    category: [''],
    tags: [[] as string[]],
  });

  constructor() {
    effect(() => {
      const init = this.initial();
      if (init) {
        this.form.reset({
          name: init.name,
          description: init.description ?? '',
          category: init.category ?? '',
          tags: init.tags ?? [],
        });
      }
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    const payload: MetadataPayload = {
      name: raw.name,
      description: raw.description || undefined,
      category: raw.category || undefined,
      tags: raw.tags && raw.tags.length > 0 ? raw.tags : undefined,
    };
    this.updater()(payload).subscribe({
      next: (updated) => {
        this.submitting.set(false);
        this.visibleChange.emit(false);
        this.saved.emit(updated);
      },
      error: (err: ProblemDetail) => {
        this.submitting.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Uložení selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
  }
}
