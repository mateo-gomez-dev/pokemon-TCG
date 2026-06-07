import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Card, CardAttack } from '../../models/card.model';
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
            <tr class="card-row" *ngFor="let card of cards; trackBy: trackByCardId" (click)="openCardDetail(card)">
              <td>
                <img class="table-card-image" [src]="cardThumbnailImage(card)" [alt]="card.name" *ngIf="cardThumbnailImage(card)">
              </td>
              <td>
                <strong class="card-name-link">{{ card.name }}</strong>
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
                  (click)="$event.stopPropagation()"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card-modal-backdrop" *ngIf="selectedCardDetail as card" (click)="closeCardDetail()">
        <article class="card-modal" (click)="$event.stopPropagation()">
          <button type="button" class="modal-close" (click)="closeCardDetail()">X</button>

          <div class="card-image-pane">
            <img *ngIf="cardDetailImage(card); else noDetailImage" [src]="cardDetailImage(card)" [alt]="card.name" />
            <ng-template #noDetailImage>
              <div class="no-detail-image">Sin imagen</div>
            </ng-template>
          </div>

          <div class="card-detail-body">
            <p class="eyebrow">Detalle de carta</p>
            <h2>{{ card.name }}</h2>
            <p class="muted">ID {{ card.id }}</p>

            <div class="detail-grid">
              <p *ngIf="card.supertype"><strong>Supertype</strong><span>{{ card.supertype }}</span></p>
              <p *ngIf="card.subtypes?.length"><strong>Subtypes</strong><span>{{ formatList(card.subtypes) }}</span></p>
              <p *ngIf="card.hp"><strong>HP</strong><span>{{ card.hp }}</span></p>
              <p *ngIf="card.types?.length"><strong>Types</strong><span>{{ formatList(card.types) }}</span></p>
              <p *ngIf="card.evolvesFrom"><strong>Evoluciona de</strong><span>{{ card.evolvesFrom }}</span></p>
              <p *ngIf="card.rarity"><strong>Rareza</strong><span>{{ card.rarity }}</span></p>
              <p *ngIf="card.number"><strong>Número</strong><span>{{ card.number }}</span></p>
              <p *ngIf="card.setName"><strong>Set</strong><span>{{ card.setName }}</span></p>
            </div>

            <section *ngIf="card.attacks?.length">
              <h3>Ataques</h3>
              <article class="detail-item" *ngFor="let attack of card.attacks; trackBy: trackByAttackName">
                <strong>{{ attack.name }}</strong>
                <p>Costo: {{ attackCost(attack) }}</p>
                <p>Daño: {{ attack.damage || '-' }}</p>
                <p *ngIf="attack.text">{{ attack.text }}</p>
              </article>
            </section>

            <section class="detail-section" *ngIf="hasDetail(card.weaknesses) || hasDetail(card.resistances) || card.retreatCost?.length || card.convertedRetreatCost !== undefined">
              <h3>Combate</h3>
              <p *ngIf="hasDetail(card.weaknesses)"><strong>Debilidades:</strong> {{ formatModifierList(card.weaknesses) }}</p>
              <p *ngIf="hasDetail(card.resistances)"><strong>Resistencias:</strong> {{ formatModifierList(card.resistances) }}</p>
              <p *ngIf="card.retreatCost?.length || card.convertedRetreatCost !== undefined">
                <strong>Retirada:</strong> {{ formatList(card.retreatCost) }}
                <span *ngIf="card.convertedRetreatCost !== undefined">({{ card.convertedRetreatCost }})</span>
              </p>
            </section>

            <section *ngIf="card.rules?.length">
              <h3>Reglas</h3>
              <p *ngFor="let rule of card.rules; trackBy: trackByIndex">{{ rule }}</p>
            </section>

            <section *ngIf="cardDescription(card) as description">
              <h3>Texto</h3>
              <p>{{ description }}</p>
            </section>
          </div>
        </article>
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

    .card-row {
      cursor: pointer;
      transition: background-color 140ms ease;
    }

    .card-row:hover {
      background: #f8fafc;
    }

    .card-name-link {
      color: #1d4ed8;
    }

    .table-card-image {
      width: 56px;
    }

    td input {
      cursor: default;
      max-width: 90px;
    }

    .card-modal-backdrop {
      align-items: center;
      background: rgba(15, 23, 42, 0.62);
      display: flex;
      inset: 0;
      justify-content: center;
      padding: 1rem;
      position: fixed;
      z-index: 100;
    }

    .card-modal {
      background: rgba(255, 255, 255, 0.97);
      border: 1px solid #dbeafe;
      border-radius: 26px;
      box-shadow: 0 28px 80px rgba(15, 23, 42, 0.35);
      display: grid;
      gap: 1.25rem;
      grid-template-columns: minmax(190px, 280px) minmax(0, 1fr);
      max-height: min(88vh, 780px);
      max-width: min(94vw, 920px);
      overflow: auto;
      padding: 1.25rem;
      position: relative;
    }

    .card-image-pane img,
    .no-detail-image {
      background: #f1f5f9;
      border-radius: 18px;
      box-shadow: 0 18px 36px rgba(15, 23, 42, 0.2);
      display: block;
      width: 100%;
    }

    .no-detail-image {
      align-items: center;
      color: #64748b;
      display: flex;
      min-height: 360px;
      justify-content: center;
    }

    .card-detail-body {
      color: #0f172a;
      display: grid;
      gap: 0.85rem;
    }

    .eyebrow {
      color: #2563eb;
      font-size: 0.78rem;
      font-weight: 900;
      letter-spacing: 0.08em;
      margin-bottom: 0.1rem;
      text-transform: uppercase;
    }

    .card-modal h2 {
      margin: 0;
    }

    .detail-grid {
      display: grid;
      gap: 0.55rem;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    }

    .detail-grid p,
    .detail-section p,
    .detail-item p,
    .card-detail-body section > p {
      margin-bottom: 0.35rem;
    }

    .detail-grid strong {
      color: #475569;
      display: block;
      font-size: 0.76rem;
      text-transform: uppercase;
    }

    .card-detail-body section {
      border-top: 1px solid #e0f2fe;
      padding-top: 0.8rem;
    }

    .detail-item {
      border-top: 1px solid #eef2ff;
      padding-top: 0.55rem;
    }

    .detail-item:first-of-type {
      border-top: 0;
      padding-top: 0;
    }

    .modal-close {
      align-items: center;
      background: #0f172a;
      border: 0;
      border-radius: 50%;
      color: white;
      display: flex;
      font-weight: 900;
      height: 34px;
      justify-content: center;
      position: absolute;
      right: 0.8rem;
      top: 0.8rem;
      width: 34px;
    }

    @media (max-width: 820px) {
      .form-panel {
        align-items: stretch;
        grid-template-columns: 1fr;
      }

      .cards-table {
        overflow-x: auto;
      }

      .card-modal {
        grid-template-columns: 1fr;
        max-width: 96vw;
      }

      .card-image-pane img {
        margin: 0 auto;
        max-width: 250px;
      }
    }
  `]
})
export class DeckBuilderPageComponent implements OnInit {
  cards: Card[] = [];
  quantities: Record<string, number> = {};
  selectedCardDetail: Card | null = null;
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

  @HostListener('document:keydown.escape')
  closeCardDetail(): void {
    this.selectedCardDetail = null;
  }

  openCardDetail(card: Card): void {
    this.selectedCardDetail = card;
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

  cardThumbnailImage(card: Card): string {
    return card.imageSmallUrl ?? card.imageSmall ?? card.images?.small ?? card.imageUrl ?? card.imageLargeUrl ?? card.imageLarge ?? card.images?.large ?? '';
  }

  cardDetailImage(card: Card): string {
    return card.images?.large ?? card.imageLarge ?? card.imageLargeUrl ?? card.images?.small ?? card.imageSmall ?? card.imageSmallUrl ?? card.imageUrl ?? '';
  }

  formatList(values?: string[] | null): string {
    return values?.length ? values.join(', ') : '-';
  }

  attackCost(attack: CardAttack): string {
    return this.formatList(attack.cost);
  }

  formatModifierList(value: unknown): string {
    if (Array.isArray(value)) {
      const entries = value.map((item) => {
        if (!this.isRecord(item)) {
          return String(item);
        }
        const type = typeof item['type'] === 'string' ? item['type'] : '';
        const modifier = typeof item['value'] === 'string' ? item['value'] : '';
        return [type, modifier].filter(Boolean).join(' ');
      }).filter(Boolean);
      return entries.join(', ') || '-';
    }
    return value ? JSON.stringify(value) : '-';
  }

  hasDetail(value: unknown): boolean {
    if (Array.isArray(value)) {
      return value.length > 0;
    }
    if (typeof value === 'string') {
      return value.trim().length > 0;
    }
    return this.isRecord(value) && Object.keys(value).length > 0;
  }

  cardDescription(card: Card): string {
    return card.flavorText
      ?? card.text
      ?? this.rawString(card.rawJson, 'flavorText')
      ?? this.rawString(card.rawJson, 'text')
      ?? '';
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

  trackByAttackName(index: number, attack: CardAttack): string {
    return attack.name || String(index);
  }

  trackByIndex(index: number): number {
    return index;
  }

  private rawString(rawJson: Record<string, unknown> | undefined, key: string): string | null {
    const value = rawJson?.[key];
    return typeof value === 'string' && value.trim() ? value : null;
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }
}
