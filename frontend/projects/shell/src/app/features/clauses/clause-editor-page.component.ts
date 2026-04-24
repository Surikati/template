import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';

import {
  ClauseApiService,
  ClauseResponse,
  ClauseVersionResponse,
} from '@tmpmgmt/api-client';
import {
  TemplateDocument,
  TemplateEditorComponent,
} from '@tmpmgmt/template-editor';

import { PublishClauseVersionDialogComponent } from './publish-clause-version-dialog.component';
import { EditMetadataDialogComponent } from '../../shared/edit-metadata-dialog.component';

/**
 * Clause editor — unlike templates, there is no server-side draft. The editor holds
 * the current content in a local signal; publishing sends it as-is. The stored AST
 * on the backend is rooted {@code {type:'fragment'}}, but TipTap requires {@code 'doc'} —
 * we convert at the page boundary.
 */
@Component({
  selector: 'tm-clause-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ButtonModule,
    TagModule,
    TemplateEditorComponent,
    PublishClauseVersionDialogComponent,
    EditMetadataDialogComponent,
  ],
  template: `
    @if (clause(); as c) {
      <div class="page-head">
        <div>
          <h1>
            {{ c.name }}
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
            {{ c.slug }} · {{ c.category || '—' }}
            <p-tag
              [value]="c.status"
              [severity]="c.status === 'ACTIVE' ? 'success' : 'secondary'"
            />
          </div>
        </div>
        <div class="actions">
          <p-button
            label="Publikovat verzi"
            icon="pi pi-upload"
            [disabled]="!editorContent()"
            (onClick)="publishDialogVisible.set(true)"
          />
        </div>
      </div>

      @if (initialEditorContent(); as content) {
        <tme-template-editor
          [initialContent]="content"
          (contentChanged)="editorContent.set($event)"
        />
      } @else {
        <p>Načítám obsah…</p>
      }

      <section class="versions">
        <h2>Publikované verze</h2>
        @if (versions(); as list) {
          @if (list.length === 0) {
            <p class="muted">Zatím žádná verze — publikujte první.</p>
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

      <tm-publish-clause-version-dialog
        [visible]="publishDialogVisible()"
        (visibleChange)="publishDialogVisible.set($event)"
        [clauseId]="c.id"
        [content]="fragmentContent()"
        (published)="onPublished($event)"
      />

      <tm-edit-metadata-dialog
        [visible]="metadataDialogVisible()"
        (visibleChange)="metadataDialogVisible.set($event)"
        header="Upravit doložku"
        [initial]="{
          name: c.name,
          description: c.description,
          category: c.category,
          tags: c.tags
        }"
        [updater]="updateMetadata"
        (saved)="onMetadataSaved($any($event))"
      />
    } @else {
      <p>Načítám doložku…</p>
    }
  `,
  styles: [
    `
      .page-head { display: flex; justify-content: space-between; align-items: flex-start;
                    margin-bottom: 1rem; }
      h1 { margin: 0; font-size: 1.5rem; }
      h2 { font-size: 1.1rem; margin-top: 2rem; }
      .meta { color: #71717a; font-size: 0.9rem; margin-top: 0.25rem;
              display: flex; gap: 0.75rem; align-items: center; }
      .versions ul { list-style: none; padding: 0; margin: 0; }
      .versions li { padding: 0.5rem 0; border-bottom: 1px solid #f4f4f5; }
      .note { color: #71717a; font-size: 0.9rem; }
      .muted { color: #71717a; }
    `,
  ],
})
export class ClauseEditorPageComponent {
  private readonly api = inject(ClauseApiService);
  private readonly messages = inject(MessageService);

  readonly id = input.required<string>();

  protected readonly clause = signal<ClauseResponse | undefined>(undefined);
  protected readonly versions = signal<ClauseVersionResponse[] | undefined>(undefined);
  protected readonly initialEditorContent = signal<TemplateDocument | null>(null);
  protected readonly editorContent = signal<TemplateDocument | null>(null);
  protected readonly publishDialogVisible = signal(false);
  protected readonly metadataDialogVisible = signal(false);

  protected readonly updateMetadata = (payload: {
    name: string;
    description?: string;
    category?: string;
    tags?: string[];
  }) => this.api.updateMetadata(this.id(), payload);

  /** Current editor state, re-rooted as 'fragment' for API transport. */
  protected readonly fragmentContent = computed<unknown>(() => {
    const doc = this.editorContent();
    if (!doc) return null;
    return { type: 'fragment', content: doc.content ?? [] };
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.api.get(id).subscribe((c) => this.clause.set(c));
      this.loadVersions(id);
    });
  }

  protected onPublished(version: ClauseVersionResponse): void {
    this.messages.add({
      severity: 'success',
      summary: 'Verze publikována',
      detail: `v${version.versionNumber}`,
    });
    this.loadVersions(this.id());
  }

  protected onMetadataSaved(updated: ClauseResponse): void {
    this.clause.set(updated);
    this.messages.add({
      severity: 'success',
      summary: 'Metadata uložena',
      detail: updated.name,
    });
  }

  private loadVersions(id: string): void {
    this.api.listVersions(id).subscribe((list) => {
      this.versions.set(list);
      // Seed editor from the latest version if available; otherwise start empty.
      if (this.initialEditorContent() !== null) return;
      if (list.length > 0) {
        this.initialEditorContent.set(this.toEditorContent(list[0].content));
      } else {
        this.initialEditorContent.set({ type: 'doc', content: [{ type: 'paragraph' }] });
      }
    });
  }

  /** Convert backend {type:'fragment', content: [...]} to TipTap-compatible {type:'doc', ...}. */
  private toEditorContent(stored: unknown): TemplateDocument {
    const s = stored as { type?: string; content?: unknown[] } | null;
    return { type: 'doc', content: (s?.content ?? []) as TemplateDocument['content'] };
  }
}
