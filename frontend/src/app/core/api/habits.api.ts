import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Habit, HabitCompletion, HabitCompletionCreate, HabitCreate, HabitUpdate } from './models';

// Query parameters of GET /api/habits/{id}/completions. Unset values are left out of the request.
export interface HabitCompletionParams {
  from?: string;
  to?: string;
}

// Relative URLs: the dev server proxies /api to the backend (frontend/proxy.conf.js).
@Injectable({ providedIn: 'root' })
export class HabitsApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/habits';

  listHabits(active?: boolean): Observable<Habit[]> {
    let params = new HttpParams();
    if (active !== undefined) {
      params = params.set('active', String(active));
    }
    return this.http.get<Habit[]>(this.url, { params });
  }

  getHabit(id: number): Observable<Habit> {
    return this.http.get<Habit>(`${this.url}/${id}`);
  }

  createHabit(body: HabitCreate): Observable<Habit> {
    return this.http.post<Habit>(this.url, body);
  }

  updateHabit(id: number, body: HabitUpdate): Observable<Habit> {
    return this.http.put<Habit>(`${this.url}/${id}`, body);
  }

  deleteHabit(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  listCompletions(id: number, params: HabitCompletionParams = {}): Observable<HabitCompletion[]> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<HabitCompletion[]>(`${this.url}/${id}/completions`, { params: httpParams });
  }

  // Without a date the server completes the habit for today (user time zone).
  completeHabit(id: number, date?: string): Observable<HabitCompletion> {
    const body: HabitCompletionCreate = date ? { completionDate: date } : {};
    return this.http.post<HabitCompletion>(`${this.url}/${id}/completions`, body);
  }

  deleteCompletion(id: number, date: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/completions/${date}`);
  }
}
