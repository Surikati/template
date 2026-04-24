import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { Question, Questionnaire, Section } from './model/questionnaire';

/**
 * Reusable presentational runner. Holds answers in a reactive form keyed by question.variablePath.
 *
 * Visibility is driven by the {@link visibility} input — a map keyed by section.id and question.id.
 * Missing keys default to visible (so the runner stays usable before the host has computed any
 * results). The host page is responsible for evaluating each rule (typically via the
 * {@code evaluate-rules} backend endpoint) and pushing the resulting map back in.
 *
 * Answers are emitted as a nested object: flat dotted keys like {@code customer.name} become
 * {@code { customer: { name: ... } }} so that downstream rule and template evaluation walk the
 * same hierarchical paths.
 */
@Component({
  selector: 'tmq-questionnaire-runner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    CheckboxModule,
    DropdownModule,
    InputNumberModule,
    InputTextModule,
    MultiSelectModule,
  ],
  template: `
    <form [formGroup]="form" class="runner">
      <h2 class="title">{{ questionnaire().name }}</h2>

      @for (section of visibleSections(); track section.id) {
        <section class="section">
          <h3>{{ section.title }}</h3>

          @for (q of visibleQuestionsFor(section); track q.id) {
            <div class="question">
              <label [attr.for]="q.id" class="q-label">
                {{ q.label }}
                @if (isRequired(q)) {
                  <span class="req">*</span>
                }
              </label>
              <div class="q-control">
                @switch (q.questionType) {
                  @case ('TEXT') {
                    <input
                      pInputText
                      [id]="q.id"
                      [formControlName]="q.variablePath"
                      class="w-full"
                    />
                  }
                  @case ('NUMBER') {
                    <p-inputNumber
                      [inputId]="q.id"
                      [formControlName]="q.variablePath"
                      [showButtons]="false"
                      mode="decimal"
                      [minFractionDigits]="0"
                      [maxFractionDigits]="6"
                    />
                  }
                  @case ('DATE') {
                    <p-calendar
                      [inputId]="q.id"
                      [formControlName]="q.variablePath"
                      dateFormat="dd.mm.yy"
                      [showIcon]="true"
                      appendTo="body"
                    />
                  }
                  @case ('BOOLEAN') {
                    <p-checkbox
                      [inputId]="q.id"
                      [formControlName]="q.variablePath"
                      [binary]="true"
                    />
                  }
                  @case ('SELECT') {
                    <p-dropdown
                      [inputId]="q.id"
                      [formControlName]="q.variablePath"
                      [options]="q.options ?? []"
                      optionLabel="label"
                      optionValue="value"
                      appendTo="body"
                      [showClear]="true"
                      placeholder="—"
                    />
                  }
                  @case ('MULTISELECT') {
                    <p-multiSelect
                      [inputId]="q.id"
                      [formControlName]="q.variablePath"
                      [options]="q.options ?? []"
                      optionLabel="label"
                      optionValue="value"
                      appendTo="body"
                      placeholder="—"
                    />
                  }
                  @case ('GROUP') {
                    <em class="muted">Skupinové otázky zatím nejsou podporované.</em>
                  }
                }
              </div>
            </div>
          }
        </section>
      }

      <div class="actions">
        <p-button
          label="Dokončit dotazník"
          icon="pi pi-check"
          [disabled]="completing()"
          (onClick)="finish()"
        />
      </div>
    </form>
  `,
  styles: [
    `
      .runner { display: flex; flex-direction: column; gap: 1.5rem; }
      .title { margin: 0; font-size: 1.4rem; }
      .section {
        background: #ffffff;
        border: 1px solid #e4e4e7;
        border-radius: 6px;
        padding: 1rem 1.25rem;
        display: flex; flex-direction: column; gap: 1rem;
      }
      .section h3 { margin: 0 0 0.25rem; font-size: 1.1rem; }
      .question { display: grid; grid-template-columns: 220px 1fr; gap: 1rem; align-items: start; }
      .q-label { color: #27272a; font-size: 0.95rem; padding-top: 0.4rem; }
      .req { color: #dc2626; margin-left: 0.15rem; }
      .q-control { display: flex; flex-direction: column; gap: 0.25rem; }
      .q-control .w-full { width: 100%; }
      .q-control p-inputNumber, .q-control p-dropdown, .q-control p-multiSelect, .q-control p-calendar {
        width: 100%;
      }
      .rule-note { font-size: 0.8rem; color: #71717a; }
      .rule-note code { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
      .muted { color: #71717a; }
      .actions { display: flex; justify-content: flex-end; }
    `,
  ],
})
export class QuestionnaireRunnerComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);

  readonly questionnaire = input.required<Questionnaire>();
  /** Answers as nested object (e.g. {customer: {name: 'X'}}) — see class doc. */
  readonly initialAnswers = input<Record<string, unknown>>({});
  readonly completing = input(false);
  /** Map of section.id / question.id → boolean. Missing keys are treated as visible. */
  readonly visibility = input<Record<string, boolean>>({});

  readonly answersChanged = output<Record<string, unknown>>();
  readonly completed = output<Record<string, unknown>>();

  protected readonly form: FormGroup = this.fb.group({});
  private formSub?: Subscription;

  private readonly allQuestions = computed<Question[]>(() =>
    this.questionnaire().sections.flatMap((s: Section) => s.questions),
  );

  protected readonly visibleSections = computed<Section[]>(() =>
    this.questionnaire().sections.filter((s) => this.isVisible(s.id)),
  );

  protected visibleQuestionsFor(section: Section): Question[] {
    return section.questions.filter((q) => this.isVisible(q.id));
  }

  private isVisible(id: string): boolean {
    const v = this.visibility()[id];
    return v === undefined ? true : v;
  }

  constructor() {
    effect(() => {
      const questions = this.allQuestions();
      const initial = this.initialAnswers();
      this.rebuildForm(questions, initial);
    });
  }

  ngOnDestroy(): void {
    this.formSub?.unsubscribe();
  }

  protected isRequired(q: Question): boolean {
    return Boolean((q.validation as { required?: boolean } | null | undefined)?.required);
  }

  protected finish(): void {
    if (this.completing()) return;
    this.completed.emit(this.collectAnswers());
  }

  private rebuildForm(questions: Question[], initial: Record<string, unknown>): void {
    this.formSub?.unsubscribe();
    Object.keys(this.form.controls).forEach((k) => this.form.removeControl(k));

    for (const q of questions) {
      const seeded = readPath(initial, q.variablePath);
      this.form.addControl(q.variablePath, new FormControl(seeded ?? this.defaultFor(q)));
    }

    this.formSub = this.form.valueChanges
      .pipe(debounceTime(400))
      .subscribe(() => this.answersChanged.emit(this.collectAnswers()));
  }

  private defaultFor(q: Question): unknown {
    switch (q.questionType) {
      case 'BOOLEAN':
        return false;
      case 'MULTISELECT':
        return [];
      default:
        return null;
    }
  }

  private collectAnswers(): Record<string, unknown> {
    const raw = this.form.getRawValue() as Record<string, unknown>;
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(raw)) {
      const value = normalizeValue(v);
      if (value === undefined) continue;
      writePath(out, k, value);
    }
    return out;
  }
}

/** Strips empty/null answers; converts Date → ISO date string. {@code undefined} means skip. */
function normalizeValue(v: unknown): unknown {
  if (v instanceof Date) return v.toISOString().slice(0, 10);
  if (v === null || v === undefined || v === '') return undefined;
  if (Array.isArray(v) && v.length === 0) return undefined;
  return v;
}

/** Walks dotted {@code path} into {@code target}, creating intermediate objects as needed. */
function writePath(target: Record<string, unknown>, path: string, value: unknown): void {
  const segs = path.split('.');
  let cursor = target;
  for (let i = 0; i < segs.length - 1; i++) {
    const seg = segs[i];
    const next = cursor[seg];
    if (typeof next !== 'object' || next === null || Array.isArray(next)) {
      cursor[seg] = {};
    }
    cursor = cursor[seg] as Record<string, unknown>;
  }
  cursor[segs[segs.length - 1]] = value;
}

/** Reads a dotted path from a nested object; returns undefined if any segment is missing. */
function readPath(source: Record<string, unknown>, path: string): unknown {
  let cursor: unknown = source;
  for (const seg of path.split('.')) {
    if (typeof cursor !== 'object' || cursor === null) return undefined;
    cursor = (cursor as Record<string, unknown>)[seg];
    if (cursor === undefined) return undefined;
  }
  return cursor;
}
