import { InjectionToken } from '@angular/core';

/** Runtime configuration injected at bootstrap. Provides API base URL. */
export interface AppConfig {
  apiBase: string;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
