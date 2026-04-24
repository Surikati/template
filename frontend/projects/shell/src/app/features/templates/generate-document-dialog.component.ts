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
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

import {
  AssembleResponse,
  AssemblyApiService,
  OutputFormat,
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
    MultiSelectModule,
    ButtonModule,
  ],
  template: `
    <p-dialog
      header="Generovat dokument"
      [visible]="visible()"
      (visibleChange)="close($event)"
      [modal]="true"
      [style]="{ width: '800px' }"
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
            <span>Formáty <em>(generuje vše najednou)</em></span>
            <p-multiSelect
              formControlName="formats"
              [options]="formatOptions"
              optionLabel="label"
              optionValue="value"
              appendTo="body"
              display="chip"
            />
          </label>
          <label>
            <span>Data proměnných (JSON)</span>
            <textarea
              pInputTextarea
              formControlName="dataJson"
              rows="8"
              spellcheck="false"
              class="json-area"
            ></textarea>
            @if (jsonError()) {
              <small class="error">{{ jsonError() }}</small>
            }
          </label>
        </form>

        @if (previewHtml()) {
          <div class="preview-section">
            <div class="preview-head">
              <strong>Náhled</strong>
              <p-button
                icon="pi pi-times"
                [text]="true"
                severity="secondary"
                size="small"
                (onClick)="previewHtml.set(null)"
              />
            </div>
            <div class="preview-body" [innerHTML]="sanitizedPreview()"></div>
          </div>
        }

        <ng-template pTemplate="footer">
          <p-button
            label="Zrušit"
            severity="secondary"
            [text]="true"
            (onClick)="close(false)"
          />
          <p-button
            label="Náhled"
            icon="pi pi-eye"
            severity="secondary"
            [disabled]="form.invalid || previewing() || jsonError() !== null"
            [loading]="previewing()"
            (onClick)="preview()"
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
          <ul class="file-list">
            @for (f of result()!.files; track f.format) {
              <li>
                <span class="file-name">{{ f.filename }}</span>
                <p-button
                  [label]="f.format"
                  icon="pi pi-download"
                  size="small"
                  severity="secondary"
                  [disabled]="!f.downloadUrl"
                  (onClick)="downloadFile(f.downloadUrl)"
                />
              </li>
            }
          </ul>
        </div>

        <ng-template pTemplate="footer">
          <p-button
            label="Zavřít"
            severity="secondary"
            [text]="true"
            (onClick)="close(false)"
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
      .success h3 { margin: 0 0 1rem; }
      .muted { color: #71717a; margin-top: 0.25rem; }
      .file-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem; max-width: 28rem; margin-inline: auto; }
      .file-list li { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.5rem 0.75rem; background: #f9fafb; border: 1px solid #e4e4e7; border-radius: 4px; }
      .file-name { font-size: 0.9rem; color: #27272a; word-break: break-all; }
      .preview-section {
        margin-top: 1rem;
        border: 1px solid #e4e4e7;
        border-radius: 4px;
        overflow: hidden;
      }
      .preview-head {
        display: flex; justify-content: space-between; align-items: center;
        padding: 0.5rem 0.75rem;
        background: #f4f4f5;
        border-bottom: 1px solid #e4e4e7;
      }
      .preview-body {
        max-height: 400px; overflow: auto;
        padding: 1rem;
        background: #ffffff;
        font-size: 0.9rem;
      }
    `,
  ],
})
export class GenerateDocumentDialogComponent {
  private readonly api = inject(AssemblyApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly visible = input(false);
  readonly templateId = input.required<string>();
  readonly versions = input<TemplateVersionResponse[]>([]);

  readonly visibleChange = output<boolean>();

  protected readonly submitting = signal(false);
  protected readonly previewing = signal(false);
  protected readonly result = signal<AssembleResponse | null>(null);
  protected readonly previewHtml = signal<string | null>(null);

  protected readonly versionOptions = computed(() =>
    (this.versions() ?? []).map((v) => ({
      versionNumber: v.versionNumber,
      label: `v${v.versionNumber} — ${new Date(v.publishedAt).toLocaleString()}`,
    })),
  );

  // Backend is trusted (runs inside our gateway); still pass through DomSanitizer for safety.
  protected readonly sanitizedPreview = computed<SafeHtml | null>(() => {
    const html = this.previewHtml();
    return html ? this.sanitizer.bypassSecurityTrustHtml(html) : null;
  });

  protected readonly formatOptions: { label: string; value: OutputFormat }[] = [
    { label: 'DOCX', value: 'DOCX' },
    { label: 'PDF', value: 'PDF' },
  ];

  protected readonly form = this.fb.nonNullable.group({
    versionNumber: [0, Validators.required],
    formats: [['DOCX'] as OutputFormat[], [Validators.required, Validators.minLength(1)]],
    dataJson: ['{\n  \n}', Validators.required],
  });

  constructor() {
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

  protected preview(): void {
    if (this.form.invalid || this.previewing() || this.jsonError()) return;
    this.previewing.set(true);
    const raw = this.form.getRawValue();
    const data = JSON.parse(raw.dataJson) as Record<string, unknown>;

    this.api
      .preview({
        templateId: this.templateId(),
        templateVersionNumber: raw.versionNumber,
        data,
      })
      .subscribe({
        next: (html) => {
          this.previewing.set(false);
          this.previewHtml.set(html);
        },
        error: (err: ProblemDetail) => {
          this.previewing.set(false);
          this.messages.add({
            severity: 'error',
            summary: 'Náhled selhal',
            detail: err.detail ?? err.title,
          });
        },
      });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) return;
    if (this.jsonError()) return;

    this.submitting.set(true);
    const raw = this.form.getRawValue();
    const data = JSON.parse(raw.dataJson) as Record<string, unknown>;

    this.api
      .assemble({
        templateId: this.templateId(),
        templateVersionNumber: raw.versionNumber,
        data,
        formats: raw.formats,
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

  protected downloadFile(url: string | undefined): void {
    if (!url) return;
    window.open(this.api.resolveDownloadUrl(url), '_blank');
  }

  protected close(next: boolean): void {
    if (!next) {
      this.result.set(null);
      this.previewHtml.set(null);
      this.form.controls.dataJson.setValue('{\n  \n}');
    }
    this.visibleChange.emit(next);
  }
}
