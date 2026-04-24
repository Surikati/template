import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard factory — returns a CanActivateFn that requires at least one of the given Keycloak realm roles.
 * Triggers login when unauthenticated; redirects to /templates with a 403-like bounce when the
 * user is authenticated but lacks all required roles.
 */
export function hasRole(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isAuthenticated()) {
      auth.login();
      return false;
    }
    if (roles.some((r) => auth.hasRole(r))) return true;
    router.navigate(['/templates']);
    return false;
  };
}
