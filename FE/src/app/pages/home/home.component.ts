import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="page hero">
      <section class="panel intro">
        <p class="eyebrow">TPI Pokemon TCG</p>
        <h1>Frontend inicial para probar cartas y mazos</h1>
        <p class="muted">
          Esta interfaz consume el backend Spring Boot: cartas importadas desde el cache local y Deck Builder backend.
        </p>
        <div class="actions">
          <a routerLink="/cards">Ver cartas</a>
          <a routerLink="/deck-builder">Crear mazo</a>
          <a routerLink="/decks">Ver mazos</a>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .hero {
      display: grid;
      min-height: calc(100vh - 76px);
      place-items: center;
    }

    .intro {
      background: linear-gradient(135deg, #ffffff 0%, #eef2ff 100%);
      max-width: 760px;
      padding: 2rem;
    }

    .eyebrow {
      color: #2563eb;
      font-weight: 800;
      letter-spacing: 0.08em;
      margin: 0 0 0.75rem;
      text-transform: uppercase;
    }

    h1 {
      font-size: clamp(2rem, 5vw, 4rem);
      line-height: 1;
      margin: 0 0 1rem;
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }

    .actions a {
      background: #111827;
      border-radius: 999px;
      color: white;
      font-weight: 800;
      padding: 0.75rem 1rem;
      text-decoration: none;
    }
  `]
})
export class HomeComponent {
}
