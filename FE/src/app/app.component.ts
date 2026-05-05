import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="app-header">
      <a class="brand" routerLink="/">Pokemon TCG</a>
      <nav>
        <a routerLink="/cards" routerLinkActive="active">Cartas</a>
        <a routerLink="/decks" routerLinkActive="active">Mazos</a>
        <a routerLink="/deck-builder" routerLinkActive="active">Deck Builder</a>
      </nav>
    </header>

    <router-outlet />
  `,
  styles: [`
    .app-header {
      align-items: center;
      background: #111827;
      color: white;
      display: flex;
      gap: 1rem;
      justify-content: space-between;
      padding: 1rem 1.5rem;
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .brand {
      font-size: 1.15rem;
      font-weight: 800;
      text-decoration: none;
    }

    nav {
      display: flex;
      flex-wrap: wrap;
      gap: 0.55rem;
    }

    nav a {
      border-radius: 999px;
      color: #d1d5db;
      padding: 0.5rem 0.8rem;
      text-decoration: none;
    }

    nav a.active,
    nav a:hover {
      background: #374151;
      color: white;
    }

    @media (max-width: 640px) {
      .app-header {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `]
})
export class AppComponent {
}
