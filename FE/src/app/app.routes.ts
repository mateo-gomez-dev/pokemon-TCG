import { Routes } from '@angular/router';

import { CardsPageComponent } from './pages/cards/cards-page.component';
import { DeckBuilderPageComponent } from './pages/deck-builder/deck-builder-page.component';
import { DecksPageComponent } from './pages/decks/decks-page.component';
import { GamesPageComponent } from './pages/games/games-page.component';
import { HomeComponent } from './pages/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'cards', component: CardsPageComponent },
  { path: 'decks', component: DecksPageComponent },
  { path: 'deck-builder', component: DeckBuilderPageComponent },
  { path: 'game', component: GamesPageComponent },
  { path: '**', redirectTo: '' }
];
