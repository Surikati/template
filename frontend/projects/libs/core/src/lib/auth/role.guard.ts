import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard factory — returns a CanActivateFn that requires the given Keycloak realm role.
 * Triggers login when unauthenticated; redirects to /templates with a 403-like bounce when the
 * user is authenticated but lacks the role.
 */
export function hasRole(role: string): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isAuthenticated()) {
      auth.login();
      return false;
    }
    if (auth.hasRole(role)) return true;
    router.navigate(['/templates']);
    return false;
  };
}
