import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { DeckResponse, DeckValidationResponse } from '../../models/deck.model';
import { DecksService } from '../../services/decks.service';

@Component({
  selector: 'app-decks-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="page">
      <header class="page-title">
        <div>
          <h1>Mazos</h1>
          <p class="muted">Listado desde <code>GET /api/decks</code>.</p>
        </div>
        <div class="actions">
          <button type="button" class="secondary" (click)="createTestDeck()" [disabled]="creatingTestDeck">
            {{ creatingTestDeck ? 'Creando...' : 'Mazo prueba' }}
          </button>
          <button type="button" (click)="loadDecks()">Recargar</button>
        </div>
      </header>

      <p *ngIf="error" class="error">{{ error }}</p>
      <p *ngIf="loading" class="muted">Cargando mazos...</p>

      <section class="layout" *ngIf="!loading">
        <div class="panel list">
          <p *ngIf="decks.length === 0" class="muted">Todavia no hay mazos creados.</p>
          <article class="deck-row" *ngFor="let deck of decks; trackBy: trackByDeckId" (click)="selectDeck(deck)">
            <div>
              <h2>{{ deck.name }}</h2>
              <p class="muted">{{ deck.totalCards }} cartas</p>
            </div>
            <div class="row-actions">
              <strong [class.valid]="deck.valid" [class.invalid]="!deck.valid">
                {{ deck.valid ? 'Valido' : 'Invalido' }}
              </strong>
              <button type="button" class="danger" (click)="deleteDeck(deck.id, $event)">Eliminar</button>
            </div>
          </article>
        </div>

        <aside class="panel detail" *ngIf="selectedDeck; else noDeck">
          <div class="detail-header">
            <div>
              <h2>{{ selectedDeck.name }}</h2>
              <p class="muted">ID {{ selectedDeck.id }} - {{ selectedDeck.totalCards }} cartas</p>
            </div>
            <button type="button" (click)="validateDeck(selectedDeck.id)">Validar</button>
          </div>

          <div *ngIf="validation" [class.success]="validation.valid" [class.error]="!validation.valid">
            <strong>{{ validation.valid ? 'Mazo valido' : 'Mazo invalido' }}</strong>
            <p>Total: {{ validation.totalCards }} cartas</p>
            <ul *ngIf="validation.errors.length > 0">
              <li *ngFor="let validationError of validation.errors; trackBy: trackByIndex">{{ validationError }}</li>
            </ul>
          </div>

          <h3>Cartas</h3>
          <table>
            <thead>
              <tr>
                <th>Cantidad</th>
                <th>Carta</th>
                <th>Tipo</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let deckCard of selectedDeck.cards; trackBy: trackByDeckCardId">
                <td>{{ deckCard.quantity }}</td>
                <td>{{ deckCard.name }}</td>
                <td>{{ deckCard.supertype }}</td>
              </tr>
            </tbody>
          </table>
        </aside>

        <ng-template #noDeck>
          <aside class="panel detail muted">Selecciona un mazo para ver el detalle.</aside>
        </ng-template>
      </section>
    </main>
  `,
  styles: [`
    .page-title,
    .detail-header {
      align-items: center;
      display: flex;
      gap: 1rem;
      justify-content: space-between;
      margin-bottom: 1rem;
    }

    h1,
    h2,
    h3,
    p {
      margin-top: 0;
    }

    .layout {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1.4fr);
    }

    .deck-row {
      align-items: center;
      border: 1px solid #e5e7eb;
      border-radius: 14px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.75rem;
      padding: 0.85rem;
    }

    .deck-row:hover {
      background: #f9fafb;
    }

    .actions,
    .row-actions {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
      justify-content: flex-end;
    }

    .danger {
      background: #dc2626;
      padding: 0.45rem 0.7rem;
    }

    .valid {
      color: #15803d;
    }

    .invalid {
      color: #b91c1c;
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
    }

    @media (max-width: 820px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DecksPageComponent implements OnInit {
  decks: DeckResponse[] = [];
  selectedDeck: DeckResponse | null = null;
  validation: DeckValidationResponse | null = null;
  error = '';
  loading = false;
  creatingTestDeck = false;

  constructor(private readonly decksService: DecksService) {
  }

  ngOnInit(): void {
    this.loadDecks();
  }

  loadDecks(): void {
    this.loading = true;
    this.error = '';
    this.decksService.getDecks().subscribe({
      next: (decks) => {
        this.decks = decks;
        if (this.selectedDeck) {
          this.selectedDeck = decks.find((deck) => deck.id === this.selectedDeck?.id) ?? this.selectedDeck;
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los mazos.';
        this.loading = false;
      }
    });
  }

  selectDeck(deck: DeckResponse): void {
    this.selectedDeck = deck;
    this.validation = null;
  }

  validateDeck(deckId: number): void {
    this.decksService.validateDeck(deckId).subscribe({
      next: (validation) => {
        this.validation = validation;
        this.loadDecks();
      },
      error: () => {
        this.error = 'No se pudo validar el mazo.';
      }
    });
  }

  createTestDeck(): void {
    this.creatingTestDeck = true;
    this.error = '';
    this.decksService.createDeck({
      name: `Mazo prueba ${new Date().toLocaleTimeString()}`,
      cards: [
        { cardId: 'xy1-1', quantity: 4 },
        { cardId: 'xy1-132', quantity: 56 }
      ]
    }).subscribe({
      next: (deck) => {
        this.creatingTestDeck = false;
        this.selectedDeck = deck;
        this.loadDecks();
      },
      error: () => {
        this.error = 'No se pudo crear el mazo prueba. Verifica que las cartas XY1 esten importadas.';
        this.creatingTestDeck = false;
      }
    });
  }

  deleteDeck(deckId: number, event: MouseEvent): void {
    event.stopPropagation();
    this.error = '';
    this.decksService.deleteDeck(deckId).subscribe({
      next: () => {
        if (this.selectedDeck?.id === deckId) {
          this.selectedDeck = null;
          this.validation = null;
        }
        this.loadDecks();
      },
      error: () => {
        this.error = 'No se pudo eliminar el mazo. Puede estar usado por una partida.';
      }
    });
  }

  trackByDeckId(_index: number, deck: DeckResponse): number {
    return deck.id;
  }

  trackByDeckCardId(index: number, deckCard: { cardId?: string }): string {
    return deckCard.cardId ?? String(index);
  }

  trackByIndex(index: number): number {
    return index;
  }
}
