import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import { ClauseApiService, ClauseResponse } from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-create-clause-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    InputTextareaModule,
    ButtonModule,
  ],
  template: `
    <p-dialog
      header="Nová doložka"
      [visible]="visible()"
      (visibleChange)="visibleChange.emit($event)"
      [modal]="true"
      [style]="{ width: '500px' }"
      [draggable]="false"
      [resizable]="false"
    >
      <form [formGroup]="form" class="form" (ngSubmit)="submit()">
        <label>
          <span>Slug <em>(a-z, 0-9, pomlčky)</em></span>
          <input pInputText formControlName="slug" autocomplete="off" />
        </label>
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
      </form>

      <ng-template pTemplate="footer">
        <p-button
          label="Zrušit"
          severity="secondary"
          [text]="true"
          (onClick)="visibleChange.emit(false)"
        />
        <p-button
          label="Vytvořit"
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
    `,
  ],
})
export class CreateClauseDialogComponent {
  private readonly api = inject(ClauseApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  readonly visible = input(false);
  readonly visibleChange = output<boolean>();
  readonly created = output<ClauseResponse>();

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9][a-z0-9-]*$/)]],
    name: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    category: [''],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    this.api
      .create({
        slug: raw.slug,
        name: raw.name,
        description: raw.description || undefined,
        category: raw.category || undefined,
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.form.reset();
          this.visibleChange.emit(false);
          this.created.emit(created);
        },
        error: (err: ProblemDetail) => {
          this.submitting.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Vytvoření selhalo',
            detail: err.detail ?? err.title,
          });
        },
      });
  }
}
