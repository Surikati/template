import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let isAuthenticated: ReturnType<typeof signal<boolean>>;
  let loginSpy: jasmine.Spy;

  beforeEach(() => {
    isAuthenticated = signal(false);
    loginSpy = jasmine.createSpy('login');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { isAuthenticated, login: loginSpy },
        },
      ],
    });
  });

  function runGuard(): boolean {
    return TestBed.runInInjectionContext(
      () => authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean;
  }

  it('allows activation when the user is authenticated', () => {
    isAuthenticated.set(true);
    expect(runGuard()).toBeTrue();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('triggers login and blocks navigation when the user is not authenticated', () => {
    expect(runGuard()).toBeFalse();
    expect(loginSpy).toHaveBeenCalledTimes(1);
  });
});
