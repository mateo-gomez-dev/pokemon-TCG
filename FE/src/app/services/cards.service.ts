import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Card } from '../models/card.model';

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly apiUrl = '/api/cards';

  constructor(private readonly http: HttpClient) {
  }

  getCards(): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl);
  }

  getCard(id: string): Observable<Card> {
    return this.http.get<Card>(`${this.apiUrl}/${id}`);
  }
}
