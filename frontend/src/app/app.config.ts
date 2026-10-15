import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { apiErrorInterceptor } from './core/api/error.interceptor';
import { ClockService } from './core/clock.service';
import { IconService } from './core/icon.service';
import { SettingsStore } from './core/settings.store';
import { ThemeService } from './core/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiErrorInterceptor])),
    provideAnimationsAsync(),
    provideAppInitializer(() => {
      // Creating the clock first installs window.__qfClock before any page renders.
      inject(ClockService);
      // Icons and theme are set up before the first page renders.
      inject(IconService).registerAll();
      inject(ThemeService);
      return inject(SettingsStore).initialize();
    }),
  ],
};
