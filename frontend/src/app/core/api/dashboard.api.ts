import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Dashboard } from './models';

// Relative URLs: the dev server proxies /api to the backend (frontend/proxy.conf.js).
@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/dashboard');
  }
}
