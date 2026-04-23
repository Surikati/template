import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

export interface AuthUser {
  id: string;
  username: string;
  email?: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly _user = signal<AuthUser | null>(null);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  /** Invoked once at app startup. Loads issuer metadata then resolves the current session. */
  async init(): Promise<void> {
    await this.oauth.loadDiscoveryDocumentAndTryLogin();
    this.oauth.setupAutomaticSilentRefresh();
    this.refreshUser();

    this.oauth.events.subscribe(() => this.refreshUser());
  }

  login(): void {
    this.oauth.initLoginFlow();
  }

  logout(): void {
    this.oauth.logOut();
    this._user.set(null);
  }

  hasRole(role: string): boolean {
    return this._user()?.roles.includes(role) ?? false;
  }

  private refreshUser(): void {
    if (!this.oauth.hasValidAccessToken()) {
      this._user.set(null);
      return;
    }
    const claims = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) {
      this._user.set(null);
      return;
    }
    const realmAccess = claims['realm_access'] as { roles?: string[] } | undefined;
    this._user.set({
      id: String(claims['sub'] ?? ''),
      username: String(claims['preferred_username'] ?? claims['sub'] ?? 'unknown'),
      email: claims['email'] ? String(claims['email']) : undefined,
      roles: realmAccess?.roles ?? [],
    });
  }
}
