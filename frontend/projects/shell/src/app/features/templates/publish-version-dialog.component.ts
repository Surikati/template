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
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import { TemplateApiService, TemplateVersionResponse } from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-publish-version-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DialogModule, InputTextareaModule, ButtonModule],
  template: `
    <p-dialog
      header="Publikovat verzi"
      [visible]="visible()"
      (visibleChange)="visibleChange.emit($event)"
      [modal]="true"
      [style]="{ width: '500px' }"
      [draggable]="false"
    >
      <form [formGroup]="form" class="form">
        <label>
          <span>Popis změny (volitelné)</span>
          <textarea
            pInputTextarea
            formControlName="changeNote"
            rows="3"
            placeholder="Co se v této verzi změnilo?"
          ></textarea>
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
          label="Publikovat"
          icon="pi pi-upload"
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
    `,
  ],
})
export class PublishVersionDialogComponent {
  private readonly api = inject(TemplateApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  readonly visible = input(false);
  readonly templateId = input.required<string>();
  readonly visibleChange = output<boolean>();
  readonly published = output<TemplateVersionResponse>();

  protected readonly submitting = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    changeNote: ['', [Validators.maxLength(5000)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.api
      .publishVersion(this.templateId(), {
        changeNote: this.form.getRawValue().changeNote || undefined,
      })
      .subscribe({
        next: (v) => {
          this.submitting.set(false);
          this.form.reset();
          this.visibleChange.emit(false);
          this.published.emit(v);
        },
        error: (err: ProblemDetail) => {
          this.submitting.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Publikace selhala',
            detail: err.detail ?? err.title,
          });
        },
      });
  }
}
