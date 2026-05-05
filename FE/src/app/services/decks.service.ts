import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DeckRequest, DeckResponse, DeckValidationResponse } from '../models/deck.model';

@Injectable({ providedIn: 'root' })
export class DecksService {
  private readonly apiUrl = '/api/decks';

  constructor(private readonly http: HttpClient) {
  }

  createDeck(request: DeckRequest): Observable<DeckResponse> {
    return this.http.post<DeckResponse>(this.apiUrl, request);
  }

  getDecks(): Observable<DeckResponse[]> {
    return this.http.get<DeckResponse[]>(this.apiUrl);
  }

  getDeck(id: number): Observable<DeckResponse> {
    return this.http.get<DeckResponse>(`${this.apiUrl}/${id}`);
  }

  validateDeck(id: number): Observable<DeckValidationResponse> {
    return this.http.post<DeckValidationResponse>(`${this.apiUrl}/${id}/validate`, {});
  }
}
