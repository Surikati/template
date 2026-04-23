import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
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
    <p-autoComplete
      [(ngModel)]="query"
      [suggestions]="results()"
      (completeMethod)="onComplete($event)"
      (onSelect)="onSelect($event.value)"
      [forceSelection]="false"
      [minLength]="2"
      [showEmptyMessage]="true"
      emptyMessage="Nic nenalezeno"
      placeholder="Hledat šablony a doložky…"
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
            <div class="name">{{ hit.name || '(bez názvu)' }}</div>
            <div class="slug">{{ hit.slug }}</div>
          </div>
        </div>
      </ng-template>
    </p-autoComplete>
  `,
  styles: [
    `
      :host ::ng-deep .search-input input {
        width: 24rem;
        padding: 0.4rem 0.75rem;
      }
      .hit { display: flex; gap: 0.75rem; align-items: center; }
      .hit-text .name { font-weight: 500; }
      .hit-text .slug { color: #71717a; font-size: 0.8rem; }
    `,
  ],
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private readonly api = inject(SearchApiService);
  private readonly router = inject(Router);
  private readonly queries$ = new Subject<string>();
  private sub?: Subscription;

  protected query = '';
  protected readonly results = signal<SearchHit[]>([]);

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
}
