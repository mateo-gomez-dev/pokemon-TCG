import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Card } from '../models/card.model';

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly apiUrl = '/api/cards';

  constructor(private readonly http: HttpClient) {
  }

  getCards(): Observable<Card[]> {
    return this.http.get<Card[]>(this.apiUrl).pipe(
      map((cards) => [...cards].sort((first, second) => this.compareCards(first, second)))
    );
  }

  getCard(id: string): Observable<Card> {
    return this.http.get<Card>(`${this.apiUrl}/${id}`);
  }

  private compareCards(first: Card, second: Card): number {
    const setComparison = this.cardSetKey(first).localeCompare(this.cardSetKey(second), undefined, { numeric: true, sensitivity: 'base' });
    if (setComparison !== 0) {
      return setComparison;
    }

    const numberComparison = this.cardNumber(first) - this.cardNumber(second);
    if (numberComparison !== 0) {
      return numberComparison;
    }

    return first.id.localeCompare(second.id, undefined, { numeric: true, sensitivity: 'base' });
  }

  private cardSetKey(card: Card): string {
    return card.setId?.trim() || card.id.match(/^(.+)-\d+$/)?.[1] || '';
  }

  private cardNumber(card: Card): number {
    const idMatch = card.id.match(/-(\d+)$/);
    if (idMatch) {
      return Number(idMatch[1]);
    }

    const numberMatch = card.number?.match(/\d+/);
    return numberMatch ? Number(numberMatch[0]) : Number.MAX_SAFE_INTEGER;
  }
}
