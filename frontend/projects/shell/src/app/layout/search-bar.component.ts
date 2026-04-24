import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AutoCompleteModule, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { TagModule } from 'primeng/tag';
import {
  Subject,
  Subscription,
  debounceTime,
  distinctUntilChanged,
  switchMap,
  of,
  catchError,
} from 'rxjs';

import { SearchApiService, SearchHit } from '@tmpmgmt/api-client';

@Component({
  selector: 'tm-search-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, AutoCompleteModule, TagModule],
  template: `
    <div class="search-wrap">
      <p-autoComplete
        [(ngModel)]="query"
        [suggestions]="results()"
        (completeMethod)="onComplete($event)"
        (onSelect)="onSelect($event.value)"
        [forceSelection]="false"
        [minLength]="2"
        [showEmptyMessage]="true"
        emptyMessage="Nic nenalezeno"
        [placeholder]="placeholder()"
        styleClass="search-input"
        appendTo="body"
      >
        <ng-template let-hit pTemplate="item">
          <div class="hit">
            <p-tag
              [value]="hit.type === 'template' ? 'Šablona' : 'Doložka'"
              [severity]="hit.type === 'template' ? 'info' : 'warning'"
            />
            <div class="hit-text">
              <div class="hit-line1">
                <span class="name">{{ hit.name || '(bez názvu)' }}</span>
                @if (hit.category) {
                  <span class="category">{{ hit.category }}</span>
                }
              </div>
              @if (hit.description) {
                <div class="description">{{ hit.description }}</div>
              }
              <div class="hit-line3">
                <span class="slug">{{ hit.slug }}</span>
                @for (tag of hit.tags ?? []; track tag) {
                  <span class="tag">#{{ tag }}</span>
                }
              </div>
            </div>
          </div>
        </ng-template>
      </p-autoComplete>
      <kbd class="kbd-hint" [attr.aria-hidden]="true">{{ shortcutLabel() }}</kbd>
    </div>
  `,
  styles: [
    `
      .search-wrap { position: relative; display: inline-block; }
      :host ::ng-deep .search-input input {
        width: 24rem;
        padding: 0.4rem 3.25rem 0.4rem 0.75rem;
      }
      .kbd-hint {
        position: absolute;
        top: 50%;
        right: 0.5rem;
        transform: translateY(-50%);
        font-family: ui-sans-serif, system-ui, sans-serif;
        font-size: 0.7rem;
        line-height: 1;
        padding: 0.2rem 0.4rem;
        border: 1px solid #d4d4d8;
        border-bottom-width: 2px;
        border-radius: 4px;
        background: #fafafa;
        color: #71717a;
        pointer-events: none;
        user-select: none;
      }
      .hit { display: flex; gap: 0.75rem; align-items: flex-start; padding: 0.25rem 0; }
      .hit-text { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
      .hit-line1 { display: flex; align-items: baseline; gap: 0.5rem; }
      .hit-text .name { font-weight: 500; }
      .hit-text .category {
        font-size: 0.75rem;
        color: #6366f1;
        background: #eef2ff;
        padding: 0.05rem 0.4rem;
        border-radius: 3px;
      }
      .hit-text .description {
        color: #52525b;
        font-size: 0.85rem;
        max-width: 28rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .hit-line3 { display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap; }
      .hit-text .slug { color: #71717a; font-size: 0.8rem; }
      .hit-text .tag { color: #71717a; font-size: 0.75rem; }
    `,
  ],
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private readonly api = inject(SearchApiService);
  private readonly router = inject(Router);
  private readonly host: ElementRef<HTMLElement> = inject(ElementRef);
  private readonly queries$ = new Subject<string>();
  private sub?: Subscription;

  protected query = '';
  protected readonly results = signal<SearchHit[]>([]);

  /** Mac uses ⌘, others use Ctrl. Detected once at init from userAgent. */
  private readonly isMac = signal(
    typeof navigator !== 'undefined' && /Mac|iPhone|iPad/i.test(navigator.userAgent),
  );

  protected readonly shortcutLabel = computed(() => (this.isMac() ? '⌘K' : 'Ctrl+K'));
  protected readonly placeholder = computed(
    () => `Hledat šablony a doložky… (${this.shortcutLabel()})`,
  );

  ngOnInit(): void {
    this.sub = this.queries$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) =>
          q.length < 2
            ? of<SearchHit[]>([])
            : this.api.search(q, 'all', 8).pipe(catchError(() => of<SearchHit[]>([]))),
        ),
      )
      .subscribe((hits) => this.results.set(hits));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.queries$.complete();
  }

  protected onComplete(event: AutoCompleteCompleteEvent): void {
    this.queries$.next(event.query);
  }

  protected onSelect(hit: SearchHit): void {
    const path = hit.type === 'template' ? '/templates' : '/clauses';
    this.router.navigate([path, hit.id, 'edit']);
    this.query = '';
    this.results.set([]);
  }

  /**
   * Global Cmd/Ctrl+K — focus and select the search input. Bound at the document level so it
   * works regardless of which field currently has focus (matches the convention used by
   * GitHub, Slack, Linear, etc.).
   */
  @HostListener('document:keydown', ['$event'])
  protected onGlobalKeydown(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      const input = this.host.nativeElement.querySelector<HTMLInputElement>('input');
      input?.focus();
      input?.select();
    }
  }
}
