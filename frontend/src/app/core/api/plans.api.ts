import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Plan, PlanCreate, PlanGroup, PlanItemUpdate } from './models';

// Relative URLs: the dev server proxies /api to the backend (frontend/proxy.conf.js).
// One method per operation of the contract (tag Plans).
@Injectable({ providedIn: 'root' })
export class PlansApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/plans';

  listPlans(group?: PlanGroup): Observable<Plan[]> {
    let params = new HttpParams();
    if (group !== undefined) {
      params = params.set('group', group);
    }
    return this.http.get<Plan[]>(this.url, { params });
  }

  getPlan(id: number): Observable<Plan> {
    return this.http.get<Plan>(`${this.url}/${id}`);
  }

  createPlan(body: PlanCreate): Observable<Plan> {
    return this.http.post<Plan>(this.url, body);
  }

  deletePlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  // Returns the whole plan: progress and status are recomputed by the server.
  setItemDone(planId: number, itemId: number, done: boolean): Observable<Plan> {
    const body: PlanItemUpdate = { done };
    return this.http.put<Plan>(`${this.url}/${planId}/items/${itemId}`, body);
  }

  // Idempotent. Returns the plan with startNotified=true.
  acknowledgeStart(planId: number): Observable<Plan> {
    return this.http.post<Plan>(`${this.url}/${planId}/acknowledge-start`, {});
  }
}
