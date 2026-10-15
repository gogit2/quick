import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Settings, SettingsUpdate } from './models';

// Relative URL: the dev server proxies /api to the backend (frontend/proxy.conf.js).
@Injectable({ providedIn: 'root' })
export class SettingsApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/settings';

  getSettings(): Observable<Settings> {
    return this.http.get<Settings>(this.url);
  }

  updateSettings(body: SettingsUpdate): Observable<Settings> {
    return this.http.put<Settings>(this.url, body);
  }
}
