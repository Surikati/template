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
import { Router } from '@angular/router';
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
  QuestionnaireApiService,
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
import { GenerateDocumentDialogComponent } from './generate-document-dialog.component';
import { EditMetadataDialogComponent } from '../../shared/edit-metadata-dialog.component';

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
    GenerateDocumentDialogComponent,
    EditMetadataDialogComponent,
  ],
  template: `
    @if (template(); as tpl) {
      <div class="page-head">
        <div>
          <h1>
            {{ tpl.name }}
            <p-button
              icon="pi pi-pencil"
              [text]="true"
              severity="secondary"
              size="small"
              (onClick)="metadataDialogVisible.set(true)"
              ariaLabel="Upravit metadata"
            />
          </h1>
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
            label="Generovat dokument"
            icon="pi pi-play"
            severity="secondary"
            [disabled]="!hasPublishedVersions()"
            (onClick)="generateDialogVisible.set(true)"
          />
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
                <li class="version-row">
                  <div class="version-meta">
                    <strong>v{{ v.versionNumber }}</strong>
                    — {{ v.publishedAt | date: 'medium' }}
                    @if (v.changeNote) { <span class="note">· {{ v.changeNote }}</span> }
                  </div>
                  <p-button
                    label="Dotazník"
                    icon="pi pi-list"
                    [text]="true"
                    severity="secondary"
                    size="small"
                    (onClick)="openQuestionnaire(tpl.id, v.versionNumber)"
                  />
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

      <tm-generate-document-dialog
        [visible]="generateDialogVisible()"
        (visibleChange)="generateDialogVisible.set($event)"
        [templateId]="tpl.id"
        [versions]="versions() ?? []"
      />

      <tm-edit-metadata-dialog
        [visible]="metadataDialogVisible()"
        (visibleChange)="metadataDialogVisible.set($event)"
        header="Upravit šablonu"
        [initial]="{
          name: tpl.name,
          description: tpl.description,
          category: tpl.category,
          tags: tpl.tags
        }"
        [updater]="updateMetadata"
        (saved)="onMetadataSaved($any($event))"
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
      .version-row { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
      .note { color: #71717a; font-size: 0.9rem; }
      .muted { color: #71717a; }
    `,
  ],
})
export class TemplateEditorPageComponent implements OnDestroy {
  private readonly api = inject(TemplateApiService);
  private readonly questionnaireApi = inject(QuestionnaireApiService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);

  /** Router component input — populated from `:id` path param via withComponentInputBinding(). */
  readonly id = input.required<string>();

  protected readonly template = signal<TemplateResponse | undefined>(undefined);
  protected readonly draft = signal<TemplateDraftResponse | undefined>(undefined);
  protected readonly versions = signal<TemplateVersionResponse[] | undefined>(undefined);
  protected readonly saveState = signal<SaveState>('idle');
  protected readonly publishDialogVisible = signal(false);
  protected readonly generateDialogVisible = signal(false);
  protected readonly metadataDialogVisible = signal(false);

  /** Bound to <tm-edit-metadata-dialog> [updater]. Arrow form keeps `this` lexical. */
  protected readonly updateMetadata = (payload: {
    name: string;
    description?: string;
    category?: string;
    tags?: string[];
  }) => this.api.updateMetadata(this.id(), payload);

  protected readonly initialContent = computed<TemplateDocument | null>(() => {
    const d = this.draft();
    return d ? (d.content as TemplateDocument) : null;
  });

  protected readonly hasPublishedVersions = computed(() => (this.versions()?.length ?? 0) > 0);

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

  protected onMetadataSaved(updated: TemplateResponse): void {
    this.template.set(updated);
    this.messages.add({
      severity: 'success',
      summary: 'Metadata uložena',
      detail: updated.name,
    });
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

  protected openQuestionnaire(templateId: string, versionNumber: number): void {
    this.questionnaireApi.findByTemplateVersion(templateId, versionNumber).subscribe({
      next: (q) => this.router.navigate(['/questionnaires', q.id, 'edit']),
      error: () => {
        this.router.navigate(['/questionnaires', 'new'], {
          queryParams: { templateId, versionNumber },
        });
      },
    });
  }
}
