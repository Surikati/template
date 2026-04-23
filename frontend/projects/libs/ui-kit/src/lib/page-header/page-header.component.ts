import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tmu-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-header">
      <h1>{{ title() }}</h1>
      @if (subtitle()) {
        <p class="subtitle">{{ subtitle() }}</p>
      }
      <ng-content></ng-content>
    </header>
  `,
  styles: [
    `.page-header { padding: 1rem 1.5rem; border-bottom: 1px solid #e4e4e7; }
     .subtitle { color: #71717a; margin-top: 0.25rem; }`,
  ],
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | undefined>();
}
