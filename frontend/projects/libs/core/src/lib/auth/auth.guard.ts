import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/** Route guard — triggers login flow if the user is not authenticated. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) return true;
  auth.login();
  return false;
};
