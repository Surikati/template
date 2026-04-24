import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragHandle,
  CdkDropList,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { MessageService } from 'primeng/api';

import {
  QuestionInputDto,
  QuestionType,
  QuestionnaireApiService,
  QuestionnaireResponse,
  SectionInputDto,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

interface SectionDraft {
  /** Local stable key for *ngFor tracking — sections from backend keep their UUID, new ones get a temp id. */
  localId: string;
  title: string;
  visibilityRule: string;
  questions: QuestionDraft[];
}

interface QuestionDraft {
  localId: string;
  variablePath: string;
  label: string;
  questionType: QuestionType;
  visibilityRule: string;
  required: boolean;
  optionsRaw: string;
}

const QUESTION_TYPES: { label: string; value: QuestionType }[] = [
  { label: 'Text', value: 'TEXT' },
  { label: 'Číslo', value: 'NUMBER' },
  { label: 'Datum', value: 'DATE' },
  { label: 'Ano/Ne', value: 'BOOLEAN' },
  { label: 'Výběr (jeden)', value: 'SELECT' },
  { label: 'Výběr (více)', value: 'MULTISELECT' },
  { label: 'Skupina', value: 'GROUP' },
];

@Component({
  selector: 'tm-questionnaire-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FormsModule,
    CdkDropList,
    CdkDrag,
    CdkDragHandle,
    ButtonModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
  ],
  template: `
    @if (questionnaire() || mode() === 'new') {
      <div class="page-head">
        <div>
          <h1>
            @if (mode() === 'new') { Nový dotazník }
            @else { Úprava dotazníku }
          </h1>
          @if (questionnaire(); as q) {
            <div class="meta">
              Šablona <a [routerLink]="['/templates', q.templateId, 'edit']">{{ q.templateId }}</a>
              · verze v{{ q.templateVersionNumber }}
            </div>
          } @else {
            <div class="meta">Šablona {{ templateId() }} · verze v{{ versionNumberInt() }}</div>
          }
        </div>
        <div class="actions">
          @if (questionnaire(); as q) {
            <p-button
              label="Spustit"
              icon="pi pi-play"
              severity="secondary"
              [routerLink]="['/questionnaires', q.id, 'run']"
            />
          }
          <p-button
            label="Uložit"
            icon="pi pi-save"
            [loading]="saving()"
            [disabled]="!canSave()"
            (onClick)="save()"
          />
        </div>
      </div>

      <div class="form">
        <label class="name-field">
          <span>Název dotazníku</span>
          <input pInputText [value]="name()" (input)="name.set($any($event.target).value)" />
        </label>

        <div cdkDropList (cdkDropListDropped)="dropSection($event)" class="sections">
          @for (section of sections(); let si = $index; track section.localId) {
            <div class="section" cdkDrag>
              <div class="section-head">
                <button cdkDragHandle class="drag-handle" type="button" aria-label="Přesunout sekci">
                  <i class="pi pi-bars"></i>
                </button>
                <input
                  pInputText
                  class="section-title"
                  [value]="section.title"
                  (input)="updateSection(si, { title: $any($event.target).value })"
                  placeholder="Název sekce"
                />
                <p-button
                  icon="pi pi-trash"
                  severity="danger"
                  [text]="true"
                  (onClick)="deleteSection(si)"
                  [attr.aria-label]="'Smazat sekci'"
                />
              </div>
              <label class="rule-field">
                <span>Pravidlo viditelnosti (volitelné)</span>
                <input
                  pInputText
                  [value]="section.visibilityRule"
                  (input)="updateSection(si, { visibilityRule: $any($event.target).value })"
                  placeholder="např. answers.contractType == 'B2B'"
                />
              </label>

              <div
                cdkDropList
                [cdkDropListData]="section.questions"
                (cdkDropListDropped)="dropQuestion(si, $event)"
                class="questions"
              >
                @for (q of section.questions; let qi = $index; track q.localId) {
                  <div class="question" cdkDrag>
                    <div class="q-head">
                      <button cdkDragHandle type="button" class="drag-handle" aria-label="Přesunout otázku">
                        <i class="pi pi-bars"></i>
                      </button>
                      <input
                        pInputText
                        class="q-label"
                        [value]="q.label"
                        (input)="updateQuestion(si, qi, { label: $any($event.target).value })"
                        placeholder="Text otázky"
                      />
                      <p-button
                        icon="pi pi-trash"
                        severity="danger"
                        [text]="true"
                        (onClick)="deleteQuestion(si, qi)"
                        [attr.aria-label]="'Smazat otázku'"
                      />
                    </div>
                    <div class="q-grid">
                      <label>
                        <span>Cesta k proměnné</span>
                        <input
                          pInputText
                          [value]="q.variablePath"
                          (input)="updateQuestion(si, qi, { variablePath: $any($event.target).value })"
                          placeholder="např. customer.name"
                        />
                      </label>
                      <label>
                        <span>Typ</span>
                        <p-dropdown
                          [options]="questionTypes"
                          optionLabel="label"
                          optionValue="value"
                          [ngModel]="q.questionType"
                          [ngModelOptions]="{ standalone: true }"
                          (onChange)="updateQuestion(si, qi, { questionType: $event.value })"
                          appendTo="body"
                        />
                      </label>
                      <label class="required-field">
                        <input
                          type="checkbox"
                          [checked]="q.required"
                          (change)="updateQuestion(si, qi, { required: $any($event.target).checked })"
                        />
                        <span>Povinné</span>
                      </label>
                      <label class="full">
                        <span>Pravidlo viditelnosti (volitelné)</span>
                        <input
                          pInputText
                          [value]="q.visibilityRule"
                          (input)="updateQuestion(si, qi, { visibilityRule: $any($event.target).value })"
                          placeholder="např. answers.hasGuarantor == true"
                        />
                      </label>
                      @if (needsOptions(q.questionType)) {
                        <label class="full">
                          <span>Možnosti (jedna na řádku, formát <code>hodnota|popisek</code>)</span>
                          <textarea
                            pInputTextarea
                            rows="3"
                            [value]="q.optionsRaw"
                            (input)="updateQuestion(si, qi, { optionsRaw: $any($event.target).value })"
                            placeholder="yes|Ano&#10;no|Ne"
                          ></textarea>
                        </label>
                      }
                    </div>
                  </div>
                }
                <p-button
                  label="Přidat otázku"
                  icon="pi pi-plus"
                  [text]="true"
                  size="small"
                  (onClick)="addQuestion(si)"
                />
              </div>
            </div>
          }
        </div>

        <p-button
          label="Přidat sekci"
          icon="pi pi-plus"
          severity="secondary"
          (onClick)="addSection()"
        />
      </div>
    } @else {
      <p>Načítám…</p>
    }
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem; }
      .page-head h1 { margin: 0; font-size: 1.4rem; }
      .meta { color: #71717a; font-size: 0.9rem; margin-top: 0.25rem; }
      .actions { display: flex; gap: 0.5rem; }
      .form { display: flex; flex-direction: column; gap: 1rem; }
      .name-field { display: flex; flex-direction: column; gap: 0.25rem; max-width: 600px; }
      .name-field span { font-size: 0.85rem; color: #52525b; }
      .sections { display: flex; flex-direction: column; gap: 1rem; }
      .section { background: #ffffff; border: 1px solid #e4e4e7; border-radius: 6px; padding: 1rem; display: flex; flex-direction: column; gap: 0.75rem; }
      .section-head { display: flex; align-items: center; gap: 0.5rem; }
      .section-title { flex: 1; font-weight: 600; }
      .drag-handle { background: transparent; border: none; cursor: grab; color: #71717a; padding: 0.25rem 0.5rem; }
      .drag-handle:active { cursor: grabbing; }
      .rule-field { display: flex; flex-direction: column; gap: 0.25rem; }
      .rule-field span { font-size: 0.8rem; color: #71717a; }
      .questions { display: flex; flex-direction: column; gap: 0.5rem; padding-left: 1rem; border-left: 2px solid #f4f4f5; }
      .question { background: #fafafa; border: 1px solid #e4e4e7; border-radius: 4px; padding: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; }
      .q-head { display: flex; align-items: center; gap: 0.5rem; }
      .q-label { flex: 1; }
      .q-grid { display: grid; grid-template-columns: 1fr 1fr auto; gap: 0.75rem; align-items: end; }
      .q-grid label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.85rem; color: #52525b; }
      .q-grid label.full { grid-column: 1 / -1; }
      .required-field { flex-direction: row !important; align-items: center; gap: 0.5rem !important; padding-bottom: 0.5rem; }
      .cdk-drag-preview { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15); border-radius: 6px; }
      .cdk-drag-placeholder { opacity: 0.3; }
      code { background: #f4f4f5; padding: 0 0.25rem; border-radius: 3px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
    `,
  ],
})
export class QuestionnaireEditorPageComponent {
  private readonly api = inject(QuestionnaireApiService);
  private readonly messages = inject(MessageService);
  private readonly router = inject(Router);

  /** Route input — present in `/questionnaires/:id/edit`. */
  readonly id = input<string>();
  /** Query params — present in `/questionnaires/new?templateId=…&versionNumber=…`. */
  readonly templateId = input<string>();
  readonly versionNumber = input<string | number>();

  protected readonly mode = computed<'edit' | 'new'>(() => (this.id() ? 'edit' : 'new'));
  protected readonly versionNumberInt = computed<number>(() => {
    const v = this.versionNumber();
    return typeof v === 'string' ? parseInt(v, 10) : (v ?? 0);
  });
  protected readonly questionnaire = signal<QuestionnaireResponse | null>(null);
  protected readonly name = signal('');
  protected readonly sections = signal<SectionDraft[]>([]);
  protected readonly saving = signal(false);

  protected readonly questionTypes = QUESTION_TYPES;

  protected readonly canSave = computed(
    () => this.name().trim().length > 0 && this.allQuestionsValid(),
  );

  private nextLocalId = 1;

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) {
        this.api.get(id).subscribe((q) => this.applyLoaded(q));
        return;
      }
      // New mode — if a questionnaire already exists for this template+version, redirect to its editor
      // so the user doesn't accidentally try to create a duplicate (backend would reject anyway).
      const tplId = this.templateId();
      const ver = this.versionNumberInt();
      if (tplId && ver > 0) {
        this.api.findByTemplateVersion(tplId, ver).subscribe({
          next: (q) => this.router.navigate(['/questionnaires', q.id, 'edit']),
          error: () => {
            // 404 means no questionnaire yet — start with one empty section to bootstrap the editor.
            if (this.sections().length === 0) this.addSection();
          },
        });
      }
    });
  }

  protected addSection(): void {
    this.sections.update((arr) => [
      ...arr,
      { localId: this.localId(), title: '', visibilityRule: '', questions: [] },
    ]);
  }

  protected updateSection(index: number, patch: Partial<SectionDraft>): void {
    this.sections.update((arr) => arr.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  }

  protected deleteSection(index: number): void {
    this.sections.update((arr) => arr.filter((_, i) => i !== index));
  }

  protected dropSection(event: CdkDragDrop<SectionDraft[]>): void {
    this.sections.update((arr) => {
      const next = [...arr];
      moveItemInArray(next, event.previousIndex, event.currentIndex);
      return next;
    });
  }

  protected addQuestion(sectionIndex: number): void {
    this.sections.update((arr) =>
      arr.map((s, i) =>
        i === sectionIndex
          ? {
              ...s,
              questions: [
                ...s.questions,
                {
                  localId: this.localId(),
                  variablePath: '',
                  label: '',
                  questionType: 'TEXT',
                  visibilityRule: '',
                  required: false,
                  optionsRaw: '',
                },
              ],
            }
          : s,
      ),
    );
  }

  protected updateQuestion(
    sectionIndex: number,
    questionIndex: number,
    patch: Partial<QuestionDraft>,
  ): void {
    this.sections.update((arr) =>
      arr.map((s, i) =>
        i === sectionIndex
          ? {
              ...s,
              questions: s.questions.map((q, j) =>
                j === questionIndex ? { ...q, ...patch } : q,
              ),
            }
          : s,
      ),
    );
  }

  protected deleteQuestion(sectionIndex: number, questionIndex: number): void {
    this.sections.update((arr) =>
      arr.map((s, i) =>
        i === sectionIndex
          ? { ...s, questions: s.questions.filter((_, j) => j !== questionIndex) }
          : s,
      ),
    );
  }

  protected dropQuestion(sectionIndex: number, event: CdkDragDrop<QuestionDraft[]>): void {
    this.sections.update((arr) =>
      arr.map((s, i) => {
        if (i !== sectionIndex) return s;
        const next = [...s.questions];
        moveItemInArray(next, event.previousIndex, event.currentIndex);
        return { ...s, questions: next };
      }),
    );
  }

  protected needsOptions(type: QuestionType): boolean {
    return type === 'SELECT' || type === 'MULTISELECT';
  }

  protected save(): void {
    if (!this.canSave() || this.saving()) return;
    this.saving.set(true);
    const sectionInputs = this.toSectionInputs();
    const existing = this.questionnaire();

    const observable = existing
      ? this.api.replaceStructure(existing.id, { name: this.name().trim(), sections: sectionInputs })
      : this.api.create({
          templateId: this.templateId()!,
          templateVersionNumber: this.versionNumberInt(),
          name: this.name().trim(),
          sections: sectionInputs,
        });

    observable.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.applyLoaded(saved);
        this.messages.add({ severity: 'success', summary: 'Dotazník uložen', detail: saved.name });
        if (!existing) {
          this.router.navigate(['/questionnaires', saved.id, 'edit']);
        }
      },
      error: (err: ProblemDetail) => {
        this.saving.set(false);
        this.messages.add({
          severity: 'error',
          summary: 'Uložení selhalo',
          detail: err.detail ?? err.title,
        });
      },
    });
  }

  private allQuestionsValid(): boolean {
    return this.sections().every(
      (s) =>
        s.title.trim().length > 0 &&
        s.questions.every((q) => q.label.trim().length > 0 && q.variablePath.trim().length > 0),
    );
  }

  private toSectionInputs(): SectionInputDto[] {
    return this.sections().map((s, idx) => ({
      ordinal: idx,
      title: s.title.trim(),
      visibilityRule: s.visibilityRule.trim() || null,
      questions: s.questions.map((q, qIdx) => this.toQuestionInput(q, qIdx)),
    }));
  }

  private toQuestionInput(q: QuestionDraft, ordinal: number): QuestionInputDto {
    const validation = q.required ? { required: true } : null;
    const options = this.needsOptions(q.questionType) ? this.parseOptions(q.optionsRaw) : null;
    return {
      ordinal,
      variablePath: q.variablePath.trim(),
      label: q.label.trim(),
      questionType: q.questionType,
      validation,
      visibilityRule: q.visibilityRule.trim() || null,
      options,
    };
  }

  private parseOptions(raw: string): { value: string; label: string }[] {
    return raw
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)
      .map((line) => {
        const [value, label] = line.split('|').map((s) => s.trim());
        return { value, label: label || value };
      });
  }

  private applyLoaded(q: QuestionnaireResponse): void {
    this.questionnaire.set(q);
    this.name.set(q.name);
    this.sections.set(q.sections.map((s) => this.toDraftSection(s)));
  }

  private toDraftSection(s: QuestionnaireResponse['sections'][number]): SectionDraft {
    return {
      localId: this.localId(),
      title: s.title,
      visibilityRule: s.visibilityRule ?? '',
      questions: s.questions.map((q) => ({
        localId: this.localId(),
        variablePath: q.variablePath,
        label: q.label,
        questionType: q.questionType,
        visibilityRule: q.visibilityRule ?? '',
        required: Boolean((q.validation as { required?: boolean } | null | undefined)?.required),
        optionsRaw: (q.options ?? [])
          .map((opt) => `${opt.value}|${opt.label}`)
          .join('\n'),
      })),
    };
  }

  private localId(): string {
    return `local-${this.nextLocalId++}`;
  }
}
