import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { hasRole } from './role.guard';
import { AuthService } from './auth.service';

describe('hasRole guard', () => {
  let isAuthenticated: ReturnType<typeof signal<boolean>>;
  let userRoles: string[];
  let loginSpy: jasmine.Spy;
  let navigateSpy: jasmine.Spy;

  beforeEach(() => {
    isAuthenticated = signal(false);
    userRoles = [];
    loginSpy = jasmine.createSpy('login');
    navigateSpy = jasmine.createSpy('navigate').and.returnValue(Promise.resolve(true));
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated,
            login: loginSpy,
            hasRole: (role: string) => userRoles.includes(role),
          },
        },
        { provide: Router, useValue: { navigate: navigateSpy } },
      ],
    });
  });

  function runGuard(...roles: string[]): boolean {
    const guard = hasRole(...roles);
    return TestBed.runInInjectionContext(
      () => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean;
  }

  it('triggers login and blocks navigation when not authenticated', () => {
    expect(runGuard('admin')).toBeFalse();
    expect(loginSpy).toHaveBeenCalledTimes(1);
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('allows activation when the user holds at least one required role', () => {
    isAuthenticated.set(true);
    userRoles = ['editor'];
    expect(runGuard('admin', 'editor')).toBeTrue();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('redirects to /templates when authenticated but missing all required roles', () => {
    isAuthenticated.set(true);
    userRoles = ['viewer'];
    expect(runGuard('admin', 'editor')).toBeFalse();
    expect(loginSpy).not.toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledOnceWith(['/templates']);
  });
});
