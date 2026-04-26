import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'tm-admin-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <nav class="subnav">
      <a routerLink="users" routerLinkActive="active">
        <i class="pi pi-users"></i> Uživatelé
      </a>
      <a routerLink="audit" routerLinkActive="active">
        <i class="pi pi-history"></i> Audit log
      </a>
      <a routerLink="settings" routerLinkActive="active">
        <i class="pi pi-cog"></i> Nastavení
      </a>
    </nav>
    <router-outlet />
  `,
  styles: [
    `
      .subnav {
        display: flex; gap: 0.25rem;
        border-bottom: 1px solid #e4e4e7;
        margin-bottom: 1.25rem;
      }
      .subnav a {
        padding: 0.5rem 0.9rem;
        color: #52525b;
        text-decoration: none;
        border-bottom: 2px solid transparent;
        font-size: 0.9rem;
      }
      .subnav a.active {
        color: #3730a3;
        border-bottom-color: #3730a3;
      }
      .subnav i { margin-right: 0.4rem; }
    `,
  ],
})
export class AdminShellComponent {}
