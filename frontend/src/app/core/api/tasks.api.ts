import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Task, TaskCreate, TaskDueFilter, TaskPriority, TaskSort, TaskStatus, TaskUpdate } from './models';

// Query parameters of GET /api/tasks. Unset values are left out of the request.
export interface TaskListParams {
  q?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  due?: TaskDueFilter;
  dueFrom?: string;
  dueTo?: string;
  archived?: boolean;
  sort?: TaskSort;
}

// Relative URLs: the dev server proxies /api to the backend (frontend/proxy.conf.js).
@Injectable({ providedIn: 'root' })
export class TasksApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/tasks';

  listTasks(params: TaskListParams = {}): Observable<Task[]> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<Task[]>(this.url, { params: httpParams });
  }

  getTask(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.url}/${id}`);
  }

  createTask(body: TaskCreate): Observable<Task> {
    return this.http.post<Task>(this.url, body);
  }

  updateTask(id: number, body: TaskUpdate): Observable<Task> {
    return this.http.put<Task>(`${this.url}/${id}`, body);
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  completeTask(id: number): Observable<Task> {
    return this.http.post<Task>(`${this.url}/${id}/complete`, null);
  }

  archiveTask(id: number): Observable<Task> {
    return this.http.post<Task>(`${this.url}/${id}/archive`, null);
  }

  restoreTask(id: number): Observable<Task> {
    return this.http.post<Task>(`${this.url}/${id}/restore`, null);
  }
}
