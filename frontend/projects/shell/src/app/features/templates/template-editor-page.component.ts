import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { TagModule } from 'primeng/tag';
import {
  EMPTY,
  Subject,
  Subscription,
  catchError,
  debounceTime,
  switchMap,
} from 'rxjs';

import {
  TemplateApiService,
  TemplateDraftResponse,
  TemplateResponse,
  TemplateVersionResponse,
} from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';
import {
  TemplateDocument,
  TemplateEditorComponent,
} from '@tmpmgmt/template-editor';

import { PublishVersionDialogComponent } from './publish-version-dialog.component';

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

@Component({
  selector: 'tm-template-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ButtonModule,
    TagModule,
    TemplateEditorComponent,
    PublishVersionDialogComponent,
  ],
  template: `
    @if (template(); as tpl) {
      <div class="page-head">
        <div>
          <h1>{{ tpl.name }}</h1>
          <div class="meta">
            {{ tpl.slug }} · {{ tpl.category || '—' }}
            <span class="save-state" [class.saving]="saveState() === 'saving'"
                  [class.saved]="saveState() === 'saved'"
                  [class.error]="saveState() === 'error'">
              @switch (saveState()) {
                @case ('saving') { <i class="pi pi-spin pi-spinner"></i> Ukládám… }
                @case ('saved') { <i class="pi pi-check"></i> Uloženo }
                @case ('error') { <i class="pi pi-times"></i> Chyba uložení }
              }
            </span>
          </div>
        </div>
        <div class="actions">
          <p-button
            label="Publikovat verzi"
            icon="pi pi-upload"
            (onClick)="publishDialogVisible.set(true)"
          />
        </div>
      </div>

      @if (initialContent(); as content) {
        <tme-template-editor
          [initialContent]="content"
          (contentChanged)="onContentChanged($event)"
        />
      }

      <section class="versions">
        <h2>Publikované verze</h2>
        @if (versions(); as list) {
          @if (list.length === 0) {
            <p class="muted">Zatím žádná verze.</p>
          } @else {
            <ul>
              @for (v of list; track v.id) {
                <li>
                  <strong>v{{ v.versionNumber }}</strong>
                  — {{ v.publishedAt | date: 'medium' }}
                  @if (v.changeNote) { <span class="note">· {{ v.changeNote }}</span> }
                </li>
              }
            </ul>
          }
        }
      </section>

      <tm-publish-version-dialog
        [visible]="publishDialogVisible()"
        (visibleChange)="publishDialogVisible.set($event)"
        [templateId]="tpl.id"
        (published)="onPublished($event)"
      />
    } @else {
      <p>Načítám šablonu…</p>
    }
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: flex-start;
                    margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      h2 { font-size: 1.1rem; margin-top: 2rem; }
      .meta { color: #71717a; font-size: 0.9rem; margin-top: 0.25rem;
              display: flex; gap: 1rem; align-items: center; }
      .save-state.saving { color: #2563eb; }
      .save-state.saved  { color: #059669; }
      .save-state.error  { color: #dc2626; }
      .versions ul { list-style: none; padding: 0; margin: 0; }
      .versions li { padding: 0.5rem 0; border-bottom: 1px solid #f4f4f5; }
      .note { color: #71717a; font-size: 0.9rem; }
      .muted { color: #71717a; }
    `,
  ],
})
export class TemplateEditorPageComponent implements OnDestroy {
  private readonly api = inject(TemplateApiService);
  private readonly messages = inject(MessageService);

  /** Router component input — populated from `:id` path param via withComponentInputBinding(). */
  readonly id = input.required<string>();

  protected readonly template = signal<TemplateResponse | undefined>(undefined);
  protected readonly draft = signal<TemplateDraftResponse | undefined>(undefined);
  protected readonly versions = signal<TemplateVersionResponse[] | undefined>(undefined);
  protected readonly saveState = signal<SaveState>('idle');
  protected readonly publishDialogVisible = signal(false);

  protected readonly initialContent = computed<TemplateDocument | null>(() => {
    const d = this.draft();
    return d ? (d.content as TemplateDocument) : null;
  });

  private readonly changes$ = new Subject<TemplateDocument>();
  private readonly saveSub: Subscription;

  constructor() {
    this.saveSub = this.changes$
      .pipe(
        debounceTime(1000),
        switchMap((content) => {
          this.saveState.set('saving');
          const schema = this.draft()?.variablesSchema ?? { type: 'object' };
          return this.api.saveDraft(this.id(), { content, variablesSchema: schema }).pipe(
            catchError((err: ProblemDetail) => {
              this.saveState.set('error');
              this.messages.add({
                severity: 'error',
                summary: 'Autosave selhal',
                detail: err.detail ?? err.title,
              });
              return EMPTY;
            }),
          );
        }),
      )
      .subscribe((saved) => {
        this.draft.set(saved);
        this.saveState.set('saved');
      });

    // Load data when id() changes.
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.get(id).subscribe((t) => this.template.set(t));
      this.api.getDraft(id).subscribe((d) => this.draft.set(d));
      this.refreshVersions(id);
    });
  }

  ngOnDestroy(): void {
    this.saveSub.unsubscribe();
    this.changes$.complete();
  }

  protected onContentChanged(content: TemplateDocument): void {
    this.changes$.next(content);
  }

  protected onPublished(version: TemplateVersionResponse): void {
    this.messages.add({
      severity: 'success',
      summary: 'Verze publikována',
      detail: `v${version.versionNumber}`,
    });
    this.refreshVersions(this.id());
  }

  private refreshVersions(id: string): void {
    this.api.listVersions(id).subscribe((list) => this.versions.set(list));
  }
}
