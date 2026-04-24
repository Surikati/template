import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { MessageService } from 'primeng/api';

import {
  AssembleResponse,
  AssemblyApiService,
  OutputFormat,
  QuestionnaireApiService,
  QuestionnaireResponse,
  RuleInput,
  SectionResponse,
  SessionResponse,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';
import { QuestionnaireRunnerComponent } from '@tmpmgmt/questionnaire-runner';

type RunState = 'loading' | 'in-progress' | 'completing' | 'assembling' | 'done' | 'error';

@Component({
  selector: 'tm-questionnaire-runner-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, ButtonModule, SelectButtonModule, QuestionnaireRunnerComponent],
  template: `
    @switch (state()) {
      @case ('loading') {
        <p>Načítám…</p>
      }
      @case ('in-progress') {
        @if (questionnaire(); as q) {
          <div class="page-head">
            <div>
              <h1>{{ q.name }}</h1>
              <div class="meta">
                Šablona <a [routerLink]="['/templates', q.templateId, 'edit']">{{ q.templateId }}</a>
                · verze v{{ q.templateVersionNumber }}
                @if (saveIndicator(); as si) {
                  <span class="save-state" [class.saving]="si === 'saving'" [class.saved]="si === 'saved'">
                    @switch (si) {
                      @case ('saving') { <i class="pi pi-spin pi-spinner"></i> Ukládám… }
                      @case ('saved') { <i class="pi pi-check"></i> Uloženo }
                    }
                  </span>
                }
              </div>
            </div>
            <div class="format-pick">
              <span class="format-label">Formát:</span>
              <p-selectButton
                [options]="formatOptions"
                optionLabel="label"
                optionValue="value"
                [(ngModel)]="format"
                [allowEmpty]="false"
              />
            </div>
          </div>
          <tmq-questionnaire-runner
            [questionnaire]="q"
            [initialAnswers]="initialAnswers()"
            [visibility]="visibility()"
            (answersChanged)="onAnswersChanged($event)"
            (completed)="onCompleted($event)"
          />
        }
      }
      @case ('completing') {
        <p><i class="pi pi-spin pi-spinner"></i> Dokončuji dotazník…</p>
      }
      @case ('assembling') {
        <p><i class="pi pi-spin pi-spinner"></i> Generuji dokument…</p>
      }
      @case ('done') {
        <div class="success">
          <i class="pi pi-check-circle"></i>
          <h2>Hotovo</h2>
          @if (assembled(); as a) {
            <ul class="file-list">
              @for (f of a.files; track f.format) {
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
            <div class="actions">
              <p-button
                label="Spustit znovu"
                icon="pi pi-refresh"
                severity="secondary"
                (onClick)="restart()"
              />
            </div>
          }
        </div>
      }
      @case ('error') {
        <div class="error">
          <i class="pi pi-times-circle"></i>
          <h2>Něco se nepovedlo</h2>
          <p class="muted">{{ errorMessage() }}</p>
          <p-button label="Spustit znovu" icon="pi pi-refresh" (onClick)="restart()" />
        </div>
      }
    }
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.4rem; }
      .meta { color: #71717a; font-size: 0.9rem; margin-top: 0.25rem; display: flex; gap: 1rem; align-items: center; }
      .save-state.saving { color: #2563eb; }
      .save-state.saved { color: #059669; }
      .format-pick { display: flex; align-items: center; gap: 0.5rem; }
      .format-label { font-size: 0.85rem; color: #52525b; }
      .success, .error { text-align: center; padding: 3rem 1rem; }
      .success i { font-size: 4rem; color: #059669; }
      .error i { font-size: 4rem; color: #dc2626; }
      .success h2, .error h2 { margin-top: 1rem; }
      .muted { color: #71717a; }
      .file-list { list-style: none; padding: 0; margin: 1rem auto; max-width: 28rem; display: flex; flex-direction: column; gap: 0.5rem; }
      .file-list li { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.5rem 0.75rem; background: #f9fafb; border: 1px solid #e4e4e7; border-radius: 4px; text-align: left; }
      .file-name { font-size: 0.9rem; color: #27272a; word-break: break-all; }
      .actions { display: flex; gap: 0.5rem; justify-content: center; margin-top: 1.5rem; }
      a { color: #3730a3; text-decoration: none; }
      a:hover { text-decoration: underline; }
    `,
  ],
})
export class QuestionnaireRunnerPageComponent {
  private readonly api = inject(QuestionnaireApiService);
  private readonly assembly = inject(AssemblyApiService);
  private readonly messages = inject(MessageService);

  readonly id = input.required<string>();
  /** Optional query param — if present, runner uses the immutable version snapshot instead
   *  of the current draft. Compliance flows should always pass a versionNumber. */
  readonly versionNumber = input<string | number>();

  protected readonly questionnaire = signal<QuestionnaireResponse | null>(null);
  protected readonly session = signal<SessionResponse | null>(null);
  protected readonly state = signal<RunState>('loading');
  protected readonly assembled = signal<AssembleResponse | null>(null);
  protected readonly errorMessage = signal<string>('');
  protected readonly saveIndicator = signal<'saving' | 'saved' | null>(null);
  protected readonly visibility = signal<Record<string, boolean>>({});

  protected format: OutputFormat = 'DOCX';
  protected readonly formatOptions: { label: string; value: OutputFormat }[] = [
    { label: 'DOCX', value: 'DOCX' },
    { label: 'PDF', value: 'PDF' },
  ];

  protected readonly initialAnswers = computed<Record<string, unknown>>(
    () => (this.session()?.answers as Record<string, unknown>) ?? {},
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) this.bootstrap(id);
    });
  }

  protected onAnswersChanged(answers: Record<string, unknown>): void {
    const session = this.session();
    if (!session) return;
    this.saveIndicator.set('saving');
    this.api.submitAnswers(session.id, { answers }).subscribe({
      next: (updated) => {
        this.session.set(updated);
        this.saveIndicator.set('saved');
      },
      error: (err: ProblemDetail) => {
        this.saveIndicator.set(null);
        this.messages.add({
          severity: 'error',
          summary: 'Uložení odpovědí selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
    this.refreshVisibility(answers);
  }

  protected onCompleted(answers: Record<string, unknown>): void {
    const session = this.session();
    const q = this.questionnaire();
    if (!session || !q) return;

    this.state.set('completing');

    // Submit final answers (in case the debounce hasn't flushed) before completing.
    this.api.submitAnswers(session.id, { answers }).subscribe({
      next: () => {
        this.api.completeSession(session.id).subscribe({
          next: (done) => {
            this.session.set(done);
            this.assembleDocument(q, answers);
          },
          error: (err: ProblemDetail) => this.failWith(err),
        });
      },
      error: (err: ProblemDetail) => this.failWith(err),
    });
  }

  protected downloadFile(url: string | undefined): void {
    if (!url) return;
    window.open(this.assembly.resolveDownloadUrl(url), '_blank');
  }

  protected restart(): void {
    this.assembled.set(null);
    this.errorMessage.set('');
    this.bootstrap(this.id());
  }

  private bootstrap(id: string): void {
    this.state.set('loading');
    this.saveIndicator.set(null);
    this.visibility.set({});

    const ver = this.versionNumberInt();
    if (ver > 0) {
      // Versioned mode: parent for templateId/timestamps + snapshot for structure.
      this.api.get(id).subscribe({
        next: (root) => {
          this.api.getVersion(id, ver).subscribe({
            next: (v) => {
              this.questionnaire.set({ ...root, name: v.name, sections: v.structure });
              this.startSession(id);
            },
            error: (err: ProblemDetail) => this.failWith(err),
          });
        },
        error: (err: ProblemDetail) => this.failWith(err),
      });
    } else {
      this.api.get(id).subscribe({
        next: (q) => {
          this.questionnaire.set(q);
          this.startSession(id);
        },
        error: (err: ProblemDetail) => this.failWith(err),
      });
    }
  }

  private startSession(questionnaireId: string): void {
    this.api.startSession({ questionnaireId }).subscribe({
      next: (s) => {
        this.session.set(s);
        this.state.set('in-progress');
        this.refreshVisibility((s.answers as Record<string, unknown>) ?? {});
      },
      error: (err: ProblemDetail) => this.failWith(err),
    });
  }

  private versionNumberInt(): number {
    const v = this.versionNumber();
    if (v === undefined) return 0;
    return typeof v === 'string' ? parseInt(v, 10) : v;
  }

  private refreshVisibility(context: Record<string, unknown>): void {
    const q = this.questionnaire();
    if (!q) return;
    const rules = collectRules(q);
    if (rules.length === 0) {
      this.visibility.set({});
      return;
    }
    this.api.evaluateRules({ rules, context }).subscribe({
      next: (resp) => {
        const next: Record<string, boolean> = {};
        for (const [k, r] of Object.entries(resp.results)) next[k] = r.value;
        this.visibility.set(next);
      },
      // Eval failures are non-fatal — keep prior visibility, surface as info toast.
      error: (err: ProblemDetail) => {
        this.messages.add({
          severity: 'warn',
          summary: 'Pravidla viditelnosti nelze vyhodnotit',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  private assembleDocument(q: QuestionnaireResponse, answers: Record<string, unknown>): void {
    this.state.set('assembling');
    this.assembly
      .assemble({
        templateId: q.templateId,
        templateVersionNumber: q.templateVersionNumber,
        data: answers,
        formats: [this.format],
      })
      .subscribe({
        next: (res) => {
          this.assembled.set(res);
          this.state.set('done');
        },
        error: (err: ProblemDetail) => this.failWith(err),
      });
  }

  private failWith(err: ProblemDetail): void {
    this.errorMessage.set(err.detail ?? err.title ?? 'Neznámá chyba');
    this.state.set('error');
  }
}

function collectRules(q: QuestionnaireResponse): RuleInput[] {
  const rules: RuleInput[] = [];
  for (const s of q.sections) {
    if (s.visibilityRule) rules.push({ key: s.id, expression: s.visibilityRule });
    for (const question of s.questions) {
      if (question.visibilityRule) {
        rules.push({ key: question.id, expression: question.visibilityRule });
      }
    }
  }
  return rules;
}
