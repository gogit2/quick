import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  LearningCard,
  LearningCardCreate,
  LearningCardUpdate,
  LearningMilestone,
  LearningMilestoneCreate,
  LearningMilestoneUpdate,
  LearningNote,
  LearningNoteCreate,
} from './models';

// Relative URLs: the dev server proxies /api to the backend (frontend/proxy.conf.js).
// One method per operation of the contract (tag Learning).
@Injectable({ providedIn: 'root' })
export class LearningApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/learning-cards';

  listCards(): Observable<LearningCard[]> {
    return this.http.get<LearningCard[]>(this.url);
  }

  createCard(body: LearningCardCreate): Observable<LearningCard> {
    return this.http.post<LearningCard>(this.url, body);
  }

  getCard(id: number): Observable<LearningCard> {
    return this.http.get<LearningCard>(`${this.url}/${id}`);
  }

  updateCard(id: number, body: LearningCardUpdate): Observable<LearningCard> {
    return this.http.put<LearningCard>(`${this.url}/${id}`, body);
  }

  deleteCard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  createMilestone(cardId: number, body: LearningMilestoneCreate): Observable<LearningMilestone> {
    return this.http.post<LearningMilestone>(`${this.url}/${cardId}/milestones`, body);
  }

  updateMilestone(cardId: number, milestoneId: number, body: LearningMilestoneUpdate): Observable<LearningMilestone> {
    return this.http.put<LearningMilestone>(`${this.url}/${cardId}/milestones/${milestoneId}`, body);
  }

  deleteMilestone(cardId: number, milestoneId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${cardId}/milestones/${milestoneId}`);
  }

  createNote(cardId: number, body: LearningNoteCreate): Observable<LearningNote> {
    return this.http.post<LearningNote>(`${this.url}/${cardId}/notes`, body);
  }

  deleteNote(cardId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${cardId}/notes/${noteId}`);
  }
}
