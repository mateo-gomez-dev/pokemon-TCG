import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Card } from '../../models/card.model';
import { DeckCardRequest } from '../../models/deck.model';
import { CardsService } from '../../services/cards.service';
import { DecksService } from '../../services/decks.service';

@Component({
  selector: 'app-deck-builder-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="page">
      <header class="page-title">
        <div>
          <h1>Deck Builder</h1>
          <p class="muted">Crea mazos con cartas cacheadas localmente.</p>
        </div>
      </header>

      <section class="panel form-panel">
        <label>
          Nombre del mazo
          <input type="text" [(ngModel)]="deckName" placeholder="Ej: Mazo fuego" />
        </label>
        <div>
          <strong>{{ selectedTotal }} cartas seleccionadas</strong>
          <p class="muted">El backend validara si el mazo cumple las reglas.</p>
        </div>
        <button type="button" (click)="createDeck()" [disabled]="creating || selectedTotal === 0 || !deckName.trim()">
          {{ creating ? 'Creando...' : 'Crear mazo' }}
        </button>
      </section>

      <p *ngIf="message" class="success">{{ message }}</p>
      <p *ngIf="error" class="error">{{ error }}</p>
      <p *ngIf="loading" class="muted">Cargando cartas...</p>

      <section class="panel cards-table" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Imagen</th>
              <th>Carta</th>
              <th>Tipo</th>
              <th>HP</th>
              <th>Cantidad</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let card of cards; trackBy: trackByCardId">
              <td>
                <img [src]="card.imageSmallUrl" [alt]="card.name" *ngIf="card.imageSmallUrl">
              </td>
              <td>
                <strong>{{ card.name }}</strong>
                <p class="muted">{{ card.subtypes?.join(', ') }}</p>
              </td>
              <td>{{ card.supertype }}</td>
              <td>{{ card.hp || '-' }}</td>
              <td>
                <input
                  type="number"
                  min="0"
                  max="60"
                  [ngModel]="quantities[card.id] || 0"
                  (ngModelChange)="setQuantity(card.id, $event)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  `,
  styles: [`
    .page-title {
      margin-bottom: 1rem;
    }

    h1,
    p {
      margin-top: 0;
    }

    .form-panel {
      align-items: end;
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(220px, 1fr) auto auto;
      margin-bottom: 1rem;
    }

    label {
      display: grid;
      font-weight: 800;
      gap: 0.35rem;
    }

    table {
      border-collapse: collapse;
      width: 100%;
    }

    th,
    td {
      border-bottom: 1px solid #e5e7eb;
      padding: 0.65rem;
      text-align: left;
      vertical-align: middle;
    }

    img {
      width: 56px;
    }

    td input {
      max-width: 90px;
    }

    @media (max-width: 820px) {
      .form-panel {
        align-items: stretch;
        grid-template-columns: 1fr;
      }

      .cards-table {
        overflow-x: auto;
      }
    }
  `]
})
export class DeckBuilderPageComponent implements OnInit {
  cards: Card[] = [];
  quantities: Record<string, number> = {};
  deckName = '';
  error = '';
  message = '';
  loading = false;
  creating = false;

  constructor(
    private readonly cardsService: CardsService,
    private readonly decksService: DecksService
  ) {
  }

  get selectedTotal(): number {
    return Object.values(this.quantities).reduce((total, quantity) => total + quantity, 0);
  }

  ngOnInit(): void {
    this.loadCards();
  }

  loadCards(): void {
    this.loading = true;
    this.error = '';
    this.cardsService.getCards().subscribe({
      next: (cards) => {
        this.cards = cards;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las cartas.';
        this.loading = false;
      }
    });
  }

  setQuantity(cardId: string, value: string | number): void {
    const parsed = Number(value);
    this.quantities[cardId] = Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : 0;
  }

  createDeck(): void {
    const cards = this.buildDeckCards();
    if (cards.length === 0) {
      this.error = 'Selecciona al menos una carta.';
      return;
    }

    this.creating = true;
    this.error = '';
    this.message = '';
    this.decksService.createDeck({ name: this.deckName.trim(), cards }).subscribe({
      next: (deck) => {
        this.message = `Mazo creado: ${deck.name} (ID ${deck.id}). Estado: ${deck.valid ? 'valido' : 'invalido'}.`;
        this.quantities = {};
        this.deckName = '';
        this.creating = false;
      },
      error: () => {
        this.error = 'No se pudo crear el mazo. Verifica que las cartas existan en el cache local.';
        this.creating = false;
      }
    });
  }

  private buildDeckCards(): DeckCardRequest[] {
    return Object.entries(this.quantities)
      .filter(([, quantity]) => quantity > 0)
      .map(([cardId, quantity]) => ({ cardId, quantity }));
  }

  trackByCardId(_index: number, card: Card): string {
    return card.id;
  }
}
