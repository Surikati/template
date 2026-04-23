import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { AuthService } from '@tmpmgmt/core';

import { SearchBarComponent } from './search-bar.component';

@Component({
  selector: 'tm-app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, MenuModule, SearchBarComponent],
  template: `
    <header class="topbar">
      <div class="brand">Template Management</div>
      <nav class="nav">
        <a routerLink="/templates" routerLinkActive="active">Šablony</a>
        <a routerLink="/clauses" routerLinkActive="active">Doložky</a>
        @if (auth.hasRole('ADMIN')) {
          <a routerLink="/admin" routerLinkActive="active">Administrace</a>
        }
      </nav>
      <tm-search-bar class="search-slot" />
      <div class="user">
        @if (auth.user(); as user) {
          <span class="username">{{ user.username }}</span>
          <p-menu #menu [model]="userMenu" [popup]="true" appendTo="body"></p-menu>
          <p-button
            icon="pi pi-user"
            severity="secondary"
            [text]="true"
            (onClick)="menu.toggle($event)"
          />
        } @else {
          <p-button label="Přihlásit" (onClick)="auth.login()" />
        }
      </div>
    </header>

    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      :host { display: flex; flex-direction: column; height: 100vh; }
      .topbar {
        display: flex; align-items: center; gap: 1.5rem;
        padding: 0.5rem 1.5rem;
        border-bottom: 1px solid #e4e4e7;
        background: #ffffff;
      }
      .brand { font-weight: 600; font-size: 1.1rem; }
      .nav { display: flex; gap: 1rem; flex: 1; }
      .nav a {
        color: #52525b; text-decoration: none; padding: 0.25rem 0.5rem;
        border-radius: 4px;
      }
      .nav a.active { background: #eef2ff; color: #3730a3; }
      .user { display: flex; align-items: center; gap: 0.5rem; }
      .username { font-size: 0.9rem; color: #52525b; }
      .content { flex: 1; overflow: auto; padding: 1.5rem; background: #fafafa; }
    `,
  ],
})
export class AppShellComponent {
  protected readonly auth = inject(AuthService);

  protected readonly userMenu: MenuItem[] = [
    {
      label: 'Odhlásit',
      icon: 'pi pi-sign-out',
      command: () => this.auth.logout(),
    },
  ];
}
