import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { Card } from '../../models/card.model';
import { CardsService } from '../../services/cards.service';

@Component({
  selector: 'app-cards-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main class="page">
      <header class="page-title">
        <div>
          <h1>Cartas importadas</h1>
          <p class="muted">Datos leidos desde <code>GET /api/cards</code>.</p>
        </div>
        <button type="button" (click)="loadCards()">Recargar</button>
      </header>

      <p *ngIf="error" class="error">{{ error }}</p>
      <p *ngIf="loading" class="muted">Cargando cartas...</p>

      <section class="cards-grid" *ngIf="!loading">
        <article class="card" *ngFor="let card of cards">
          <img [src]="card.imageSmallUrl" [alt]="card.name" *ngIf="card.imageSmallUrl; else noImage">
          <ng-template #noImage>
            <div class="no-image">Sin imagen</div>
          </ng-template>
          <div class="card-body">
            <h2>{{ card.name }}</h2>
            <p>{{ card.supertype || 'Sin tipo' }}</p>
            <p class="muted" *ngIf="card.hp">HP {{ card.hp }}</p>
            <div class="tags">
              <span *ngFor="let subtype of card.subtypes">{{ subtype }}</span>
            </div>
          </div>
        </article>
      </section>
    </main>
  `,
  styles: [`
    .page-title {
      align-items: center;
      display: flex;
      gap: 1rem;
      justify-content: space-between;
      margin-bottom: 1rem;
    }

    h1 {
      margin-bottom: 0.25rem;
    }

    .cards-grid {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    }

    .card {
      background: white;
      border: 1px solid #e5e7eb;
      border-radius: 18px;
      box-shadow: 0 12px 24px rgba(15, 23, 42, 0.07);
      overflow: hidden;
    }

    img,
    .no-image {
      background: #e5e7eb;
      display: block;
      min-height: 290px;
      object-fit: contain;
      padding: 0.75rem;
      width: 100%;
    }

    .no-image {
      align-items: center;
      color: #6b7280;
      display: flex;
      justify-content: center;
    }

    .card-body {
      padding: 1rem;
    }

    h2 {
      font-size: 1rem;
      margin: 0 0 0.35rem;
    }

    p {
      margin: 0 0 0.5rem;
    }

    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }

    .tags span {
      background: #eef2ff;
      border-radius: 999px;
      color: #3730a3;
      font-size: 0.8rem;
      font-weight: 700;
      padding: 0.25rem 0.5rem;
    }
  `]
})
export class CardsPageComponent implements OnInit {
  cards: Card[] = [];
  error = '';
  loading = false;

  constructor(private readonly cardsService: CardsService) {
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
        this.error = 'No se pudieron cargar las cartas. Verifica que el backend este en http://localhost:8080.';
        this.loading = false;
      }
    });
  }
}
