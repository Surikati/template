import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';

import { TemplateApiService, TemplateVersionDiffResponse } from '@tmpmgmt/api-client';
import { ProblemDetail } from '@tmpmgmt/core';

@Component({
  selector: 'tm-template-version-diff-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, ButtonModule, TagModule],
  template: `
    <div class="page-head">
      <div>
        <h1>
          Porovnání verzí
          @if (diff(); as head) {
            <span class="versions">v{{ head.from.versionNumber }} → v{{ head.to.versionNumber }}</span>
          }
        </h1>
      </div>
      <p-button
        label="Zpět na šablonu"
        icon="pi pi-arrow-left"
        severity="secondary"
        [text]="true"
        [routerLink]="['/templates', id(), 'edit']"
      />
    </div>

    @if (loading()) {
      <p class="muted">Načítám…</p>
    } @else if (errorMsg()) {
      <div class="error">
        <i class="pi pi-exclamation-triangle"></i> {{ errorMsg() }}
      </div>
    } @else if (diff()) {
      @if (diff(); as d) {
      <section class="summary">
        <p-tag
          [value]="d.summary.contentChanged ? 'Obsah se změnil' : 'Obsah beze změny'"
          [severity]="d.summary.contentChanged ? 'warning' : 'success'"
        />
        <p-tag
          [value]="
            d.summary.variablesSchemaChanged
              ? 'Schéma proměnných se změnilo'
              : 'Schéma beze změny'
          "
          [severity]="d.summary.variablesSchemaChanged ? 'warning' : 'success'"
        />
      </section>

      <div class="grid">
        <article class="pane">
          <header>
            <h2>v{{ d.from.versionNumber }}</h2>
            <div class="meta">
              {{ d.from.publishedAt | date: 'medium' }}
              @if (d.from.changeNote) { <em>· {{ d.from.changeNote }}</em> }
            </div>
          </header>
          <h3>Obsah</h3>
          <div class="json">{{ format(d.from.content) }}</div>
          <h3>Schéma proměnných</h3>
          <div class="json">{{ format(d.from.variablesSchema) }}</div>
        </article>

        <article class="pane">
          <header>
            <h2>v{{ d.to.versionNumber }}</h2>
            <div class="meta">
              {{ d.to.publishedAt | date: 'medium' }}
              @if (d.to.changeNote) { <em>· {{ d.to.changeNote }}</em> }
            </div>
          </header>
          <h3>Obsah</h3>
          <div class="json">{{ format(d.to.content) }}</div>
          <h3>Schéma proměnných</h3>
          <div class="json">{{ format(d.to.variablesSchema) }}</div>
        </article>
      </div>
      }
    }
  `,
  styles: [
    `
      .page-head {
        display: flex; justify-content: space-between; align-items: flex-start;
        margin-bottom: 1rem;
      }
      h1 { margin: 0; font-size: 1.4rem; display: flex; align-items: baseline; gap: 0.75rem; }
      h1 .versions { font-size: 0.9rem; color: #71717a; font-weight: 500; }
      h2 { margin: 0; font-size: 1.1rem; }
      h3 { margin: 1rem 0 0.4rem; font-size: 0.85rem; color: #52525b;
           text-transform: uppercase; letter-spacing: 0.05em; }
      .summary { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
      .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; align-items: start; }
      .pane { background: #fafafa; border: 1px solid #e4e4e7; border-radius: 6px;
              padding: 1rem 1.25rem; }
      .pane header { border-bottom: 1px solid #e4e4e7; padding-bottom: 0.5rem; margin-bottom: 0.5rem; }
      .meta { color: #71717a; font-size: 0.85rem; margin-top: 0.25rem; }
      .json { background: #ffffff; border: 1px solid #e4e4e7; padding: 0.75rem 1rem;
              border-radius: 4px; max-height: 36rem; overflow: auto;
              font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
              font-size: 0.8rem; line-height: 1.45; margin: 0;
              white-space: pre; }
      .error { color: #b91c1c; padding: 1rem; background: #fef2f2;
               border-left: 3px solid #dc2626; }
      .muted { color: #71717a; }
    `,
  ],
})
export class TemplateVersionDiffPageComponent {
  private readonly api = inject(TemplateApiService);
  private readonly messages = inject(MessageService);

  readonly id = input.required<string>();
  readonly from = input.required<string>();
  readonly to = input.required<string>();

  protected readonly diff = signal<TemplateVersionDiffResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMsg = signal<string | null>(null);

  /** Pretty-print arbitrary JSON branches with stable indentation for the side-by-side view. */
  protected readonly format = (value: unknown): string => {
    if (value === null || value === undefined) return 'null';
    return JSON.stringify(value, null, 2);
  };

  constructor() {
    // Resolve query params on input change. Inputs come from withComponentInputBinding +
    // bindToQueryParams in the route definition.
    const fromN = Number.parseInt(this.from(), 10);
    const toN = Number.parseInt(this.to(), 10);
    if (!Number.isFinite(fromN) || !Number.isFinite(toN)) {
      this.errorMsg.set("Parametry 'from' a 'to' musí být celá čísla.");
      this.loading.set(false);
      return;
    }
    this.api.diffVersions(this.id(), fromN, toN).subscribe({
      next: (d) => {
        this.diff.set(d);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.loading.set(false);
        const msg = err.detail ?? err.title ?? 'Nepodařilo se načíst rozdíly.';
        this.errorMsg.set(msg);
        this.messages.add({ severity: 'error', summary: 'Chyba', detail: msg });
      },
    });
  }

}
