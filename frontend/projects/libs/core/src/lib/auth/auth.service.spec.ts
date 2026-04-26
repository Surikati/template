import { TestBed } from '@angular/core/testing';
import { OAuthEvent, OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { AuthService } from './auth.service';

interface OAuthMock {
  loadDiscoveryDocumentAndTryLogin: jasmine.Spy;
  setupAutomaticSilentRefresh: jasmine.Spy;
  hasValidAccessToken: jasmine.Spy;
  getIdentityClaims: jasmine.Spy;
  initLoginFlow: jasmine.Spy;
  logOut: jasmine.Spy;
  events: Subject<OAuthEvent>;
}

describe('AuthService', () => {
  let service: AuthService;
  let oauth: OAuthMock;

  beforeEach(async () => {
    oauth = {
      loadDiscoveryDocumentAndTryLogin: jasmine
        .createSpy('loadDiscoveryDocumentAndTryLogin')
        .and.returnValue(Promise.resolve(true)),
      setupAutomaticSilentRefresh: jasmine.createSpy('setupAutomaticSilentRefresh'),
      hasValidAccessToken: jasmine.createSpy('hasValidAccessToken').and.returnValue(false),
      getIdentityClaims: jasmine.createSpy('getIdentityClaims').and.returnValue(null),
      initLoginFlow: jasmine.createSpy('initLoginFlow'),
      logOut: jasmine.createSpy('logOut'),
      events: new Subject<OAuthEvent>(),
    };
    TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: oauth }],
    });
    service = TestBed.inject(AuthService);
    await service.init();
  });

  function emitTokenReceived(): void {
    oauth.events.next({ type: 'token_received' } as OAuthEvent);
  }

  it('starts unauthenticated when no valid token exists', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
  });

  it('populates user from identity claims after a token_received event', () => {
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.getIdentityClaims.and.returnValue({
      sub: 'user-123',
      preferred_username: 'alice',
      email: 'alice@example.com',
      realm_access: { roles: ['admin', 'editor'] },
    });
    emitTokenReceived();

    const user = service.user();
    expect(user).not.toBeNull();
    expect(user?.id).toBe('user-123');
    expect(user?.username).toBe('alice');
    expect(user?.email).toBe('alice@example.com');
    expect(user?.roles).toEqual(['admin', 'editor']);
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('falls back to sub when preferred_username is missing and defaults roles to []', () => {
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.getIdentityClaims.and.returnValue({ sub: 'user-456' });
    emitTokenReceived();

    expect(service.user()?.username).toBe('user-456');
    expect(service.user()?.roles).toEqual([]);
  });

  it('hasRole reports true only for assigned roles', () => {
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.getIdentityClaims.and.returnValue({
      sub: 'u',
      realm_access: { roles: ['editor'] },
    });
    emitTokenReceived();

    expect(service.hasRole('editor')).toBeTrue();
    expect(service.hasRole('admin')).toBeFalse();
  });

  it('clears the user when an event arrives with no valid token', () => {
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.getIdentityClaims.and.returnValue({ sub: 'u', realm_access: { roles: [] } });
    emitTokenReceived();
    expect(service.user()).not.toBeNull();

    oauth.hasValidAccessToken.and.returnValue(false);
    emitTokenReceived();
    expect(service.user()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('login() delegates to oauth.initLoginFlow', () => {
    service.login();
    expect(oauth.initLoginFlow).toHaveBeenCalledTimes(1);
  });

  it('logout() clears the user and calls oauth.logOut', () => {
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.getIdentityClaims.and.returnValue({ sub: 'u', realm_access: { roles: [] } });
    emitTokenReceived();
    expect(service.user()).not.toBeNull();

    service.logout();
    expect(oauth.logOut).toHaveBeenCalledTimes(1);
    expect(service.user()).toBeNull();
  });
});
