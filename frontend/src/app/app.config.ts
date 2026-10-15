import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { apiErrorInterceptor } from './core/api/error.interceptor';
import { ClockService } from './core/clock.service';
import { IconService } from './core/icon.service';
import { SettingsStore } from './core/settings.store';
import { ThemeService, reduceMotionRequested } from './core/theme.service';

// Material animations are switched off when the OS or the ?reduceMotion=1 test flag asks for it.
const reducedMotion =
  reduceMotionRequested() || window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiErrorInterceptor])),
    provideAnimationsAsync(reducedMotion ? 'noop' : undefined),
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
