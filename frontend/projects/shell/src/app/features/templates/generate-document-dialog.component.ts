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
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import {
  AssembleResponse,
  AssemblyApiService,
  TemplateVersionResponse,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-generate-document-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    InputTextareaModule,
    ButtonModule,
  ],
  template: `
    <p-dialog
      header="Generovat dokument"
      [visible]="visible()"
      (visibleChange)="close($event)"
      [modal]="true"
      [style]="{ width: '600px' }"
      [draggable]="false"
    >
      @if (result() === null) {
        <form [formGroup]="form" class="form">
          <label>
            <span>Verze šablony</span>
            <p-dropdown
              formControlName="versionNumber"
              [options]="versionOptions()"
              optionLabel="label"
              optionValue="versionNumber"
              appendTo="body"
              placeholder="Vyberte verzi"
            />
          </label>
          <label>
            <span>Data proměnných (JSON)</span>
            <textarea
              pInputTextarea
              formControlName="dataJson"
              rows="10"
              spellcheck="false"
              class="json-area"
            ></textarea>
            @if (jsonError()) {
              <small class="error">{{ jsonError() }}</small>
            }
          </label>
        </form>

        <ng-template pTemplate="footer">
          <p-button
            label="Zrušit"
            severity="secondary"
            [text]="true"
            (onClick)="close(false)"
          />
          <p-button
            label="Vygenerovat"
            icon="pi pi-play"
            [disabled]="form.invalid || submitting() || jsonError() !== null"
            [loading]="submitting()"
            (onClick)="submit()"
          />
        </ng-template>
      } @else {
        <div class="success">
          <i class="pi pi-check-circle success-icon"></i>
          <h3>Dokument vygenerován</h3>
          <p class="muted">{{ result()!.filename }}</p>
        </div>

        <ng-template pTemplate="footer">
          <p-button
            label="Zavřít"
            severity="secondary"
            [text]="true"
            (onClick)="close(false)"
          />
          <p-button
            label="Stáhnout"
            icon="pi pi-download"
            [disabled]="!result()!.downloadUrl"
            (onClick)="download()"
          />
        </ng-template>
      }
    </p-dialog>
  `,
  styles: [
    `
      .form { display: flex; flex-direction: column; gap: 1rem; padding-top: 0.5rem; }
      label { display: flex; flex-direction: column; gap: 0.25rem; }
      label span { font-size: 0.9rem; color: #52525b; }
      .json-area { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 0.85rem; }
      .error { color: #dc2626; font-size: 0.8rem; }
      .success { text-align: center; padding: 1.5rem 0.5rem; }
      .success-icon { font-size: 3rem; color: #059669; margin-bottom: 0.75rem; }
      .success h3 { margin: 0; }
      .muted { color: #71717a; margin-top: 0.25rem; }
    `,
  ],
})
export class GenerateDocumentDialogComponent {
  private readonly api = inject(AssemblyApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  readonly visible = input(false);
  readonly templateId = input.required<string>();
  readonly versions = input<TemplateVersionResponse[]>([]);

  readonly visibleChange = output<boolean>();

  protected readonly submitting = signal(false);
  protected readonly result = signal<AssembleResponse | null>(null);

  protected readonly versionOptions = computed(() =>
    (this.versions() ?? []).map((v) => ({
      versionNumber: v.versionNumber,
      label: `v${v.versionNumber} — ${new Date(v.publishedAt).toLocaleString()}`,
    })),
  );

  protected readonly form = this.fb.nonNullable.group({
    versionNumber: [0, Validators.required],
    dataJson: ['{\n  \n}', Validators.required],
  });

  constructor() {
    // Default to the newest version when the dialog opens.
    effect(() => {
      if (this.visible()) {
        const versions = this.versions();
        if (versions.length > 0 && this.form.controls.versionNumber.value === 0) {
          this.form.controls.versionNumber.setValue(versions[0].versionNumber);
        }
      }
    });
  }

  protected jsonError(): string | null {
    const raw = this.form.controls.dataJson.value;
    if (!raw.trim()) return null;
    try {
      const parsed = JSON.parse(raw);
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        return 'Očekávám JSON objekt (složené závorky).';
      }
      return null;
    } catch (e) {
      return 'Neplatný JSON: ' + (e as Error).message;
    }
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const jsonErr = this.jsonError();
    if (jsonErr) return;

    this.submitting.set(true);
    const raw = this.form.getRawValue();
    const data = JSON.parse(raw.dataJson) as Record<string, unknown>;

    this.api
      .assemble({
        templateId: this.templateId(),
        templateVersionNumber: raw.versionNumber,
        data,
      })
      .subscribe({
        next: (res) => {
          this.submitting.set(false);
          this.result.set(res);
        },
        error: (err: ProblemDetail) => {
          this.submitting.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Generování selhalo',
            detail: err.detail ?? err.title,
          });
        },
      });
  }

  protected download(): void {
    const res = this.result();
    if (!res?.downloadUrl) return;
    const absolute = this.api.resolveDownloadUrl(res.downloadUrl);
    // Open in a new tab — the backend sets Content-Disposition: attachment so the browser downloads.
    window.open(absolute, '_blank');
  }

  protected close(next: boolean): void {
    if (!next) {
      // Reset state so next open starts fresh.
      this.result.set(null);
      this.form.controls.dataJson.setValue('{\n  \n}');
    }
    this.visibleChange.emit(next);
  }
}
