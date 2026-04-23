import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'tm-root',
  standalone: true,
  imports: [RouterOutlet, ToastModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p-toast position="top-right" />
    <router-outlet />
  `,
})
export class AppComponent {}
