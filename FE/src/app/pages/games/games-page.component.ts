import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { Card, CardAttack } from '../../models/card.model';
import { DeckResponse } from '../../models/deck.model';
import { GamePlayerResponse, GameResponse, PokemonInPlayResponse } from '../../models/game.model';
import { CardsService } from '../../services/cards.service';
import { DecksService } from '../../services/decks.service';
import { GamesService } from '../../services/games.service';

interface SelectedCardDetail {
  cardId: string;
  card?: Card;
  pokemon?: PokemonInPlayResponse;
}

interface AttackAnimationState {
  attackerInstanceId: string;
  defenderInstanceId: string;
}

interface DrawAnimationState {
  key: string;
}

@Component({
  selector: 'app-games-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="page game-page">
      <header class="game-header">
        <div>
          <p class="eyebrow">Pokemon TCG Pocket style board</p>
          <h1>Partidas</h1>
          <p class="muted">Mesa visual para probar el backend existente de partidas.</p>
        </div>
        <button type="button" class="ghost-button" (click)="reloadAll()">Recargar</button>
      </header>

      <p *ngIf="error" class="game-alert error">{{ error }}</p>
      <p *ngIf="message" class="game-alert success">{{ message }}</p>
      <p *ngIf="selectedGame?.status === 'FINISHED'" class="game-alert victory">{{ finishedGameText }}</p>
      <p *ngIf="validDecks.length === 0" class="game-alert warning">
        No hay mazos validos. Crea o valida un mazo desde Mazos antes de iniciar partidas.
      </p>

      <section class="game-shell">
        <aside class="control-rail">
          <section class="glass-card compact-form">
            <div class="rail-title">
              <h2>Nueva partida</h2>
              <span>{{ validDecks.length }} mazos validos</span>
            </div>
            <form (ngSubmit)="createGame()">
              <label>
                Jugador 1
                <input type="text" [(ngModel)]="createPlayerName" name="createPlayerName" />
              </label>
              <label>
                Mazo
                <select [(ngModel)]="createDeckId" name="createDeckId">
                  <option [ngValue]="undefined">Seleccionar</option>
                  <option *ngFor="let deck of validDecks; trackBy: trackByDeckId" [ngValue]="deck.id">{{ deck.name }} #{{ deck.id }}</option>
                </select>
              </label>
              <button type="submit" [disabled]="!createPlayerName.trim() || !createDeckId">Crear</button>
            </form>
          </section>

          <section class="glass-card compact-form">
            <div class="rail-title">
              <h2>Unirse</h2>
              <span *ngIf="selectedGame">Partida #{{ selectedGame.id }}</span>
            </div>
            <form (ngSubmit)="joinGame()">
              <label>
                Jugador 2
                <input type="text" [(ngModel)]="joinPlayerName" name="joinPlayerName" />
              </label>
              <label>
                Mazo
                <select [(ngModel)]="joinDeckId" name="joinDeckId">
                  <option [ngValue]="undefined">Seleccionar</option>
                  <option *ngFor="let deck of validDecks; trackBy: trackByDeckId" [ngValue]="deck.id">{{ deck.name }} #{{ deck.id }}</option>
                </select>
              </label>
              <button type="submit" [disabled]="!selectedGame || selectedGame.status !== 'WAITING' || !joinPlayerName.trim() || !joinDeckId">
                Unirse
              </button>
            </form>
          </section>

          <section class="glass-card games-list">
            <div class="rail-title">
              <h2>Guardadas</h2>
              <span>{{ games.length }}</span>
            </div>
            <p *ngIf="games.length === 0" class="muted">No hay partidas creadas.</p>
            <button
              type="button"
              class="saved-game"
              *ngFor="let game of games; trackBy: trackByGameId"
              [class.selected]="selectedGame?.id === game.id"
              (click)="selectGame(game.id)"
            >
              <strong>#{{ game.id }}</strong>
              <span>{{ game.status }}</span>
              <small>{{ game.players.length }}/2 jugadores</small>
            </button>
          </section>
        </aside>

        <section class="board-wrap" *ngIf="selectedGame; else emptyBoard">
          <section class="battle-table">
            <div class="table-aura"></div>
            <div class="draw-animation-card" *ngIf="drawAnimation">
              <div class="card-back flying-draw-card"></div>
            </div>

            <header class="match-strip">
              <div>
                <strong>Partida #{{ selectedGame.id }}</strong>
                <span>{{ turnText }}</span>
              </div>
              <div class="phase-pills">
                <span>{{ selectedGame.status }}</span>
                <span>Fase {{ selectedGame.turnPhase || '-' }}</span>
                <span>Turno {{ selectedGame.currentPlayerId || '-' }}</span>
              </div>
              <button type="button" (click)="startGame()" [disabled]="!canStartGame()">
                Iniciar
              </button>
            </header>

            <section class="player-band opponent" *ngIf="opponentPlayer as opponent">
              <div class="player-name" [class.active-turn]="opponent.id === selectedGame.currentPlayerId">
                <span>Rival</span>
                <strong>{{ opponent.playerName }}</strong>
              </div>
              <div class="hidden-hand" aria-label="Mano rival">
                <div class="card-back mini" *ngFor="let cardId of opponent.handCardIds.slice(0, 7); trackBy: trackByIndex"></div>
                <span *ngIf="opponent.handSize > 7" class="more-cards">+{{ opponent.handSize - 7 }}</span>
              </div>
              <div class="side-zones top-zones">
                <div class="zone-token prize">Premios {{ opponent.prizeCardsRemaining }}</div>
                <div class="zone-token deck">Deck {{ opponent.deckRemaining }}</div>
                <div class="zone-token discard">Descarte {{ opponent.discardSize }}</div>
              </div>
            </section>

            <section class="bench-row opponent-bench" *ngIf="opponentPlayer as opponent">
              <div class="slot bench-slot" *ngFor="let slot of benchSlots; trackBy: trackByIndex">
                <ng-container *ngIf="opponent.benchPokemon[slot] as pokemon; else emptyOpponentBench">
                  <article class="tcg-card bench-card" (click)="openCardDetail(pokemon.cardId, pokemon)">
                    <ng-container *ngTemplateOutlet="cardFace; context: { cardId: pokemon.cardId, player: opponent, compact: true, pokemon: pokemon }"></ng-container>
                  </article>
                </ng-container>
                <ng-template #emptyOpponentBench><span>Banca</span></ng-template>
              </div>
            </section>

            <section class="active-lane">
              <div class="side-stack left" *ngIf="opponentPlayer as opponent">
                <div class="card-back stack-card"></div>
                <span>Premios {{ opponent.prizeCardsRemaining }}</span>
              </div>

              <div class="active-column">
                <div class="active-slot opponent-active" *ngIf="opponentPlayer as opponent">
                  <ng-container *ngIf="opponent.activePokemon as pokemon; else noOpponentActive">
                    <article
                      class="tcg-card active-card"
                      [class.glow]="opponent.id === selectedGame.currentPlayerId"
                      [class.attack-hit]="isAttackDefender(pokemon)"
                      (click)="openCardDetail(pokemon.cardId, pokemon)"
                    >
                      <ng-container *ngTemplateOutlet="cardFace; context: { cardId: pokemon.cardId, player: opponent, compact: false, pokemon: pokemon }"></ng-container>
                    </article>
                  </ng-container>
                  <ng-template #noOpponentActive><div class="empty-active">Activo rival</div></ng-template>
                </div>

                <div class="center-field">
                  <div class="pokeball-mark"></div>
                  <strong>{{ selectedGame.turnPhase === 'DRAW' ? 'Robar carta' : 'Accion principal' }}</strong>
                  <span>{{ turnText }}</span>
                </div>

                <div
                  class="active-slot player-active"
                  *ngIf="boardPlayer as player"
                  [class.drop-target]="canDropToActive()"
                  (dragover)="onActiveDragOver($event)"
                  (drop)="onActiveDrop($event)"
                >
                  <ng-container *ngIf="player.activePokemon as pokemon; else noPlayerActive">
                    <article
                      class="tcg-card active-card attack-source"
                      [class.glow]="player.id === selectedGame.currentPlayerId"
                      [class.attack-lunge]="isAttackAttacker(pokemon)"
                      [class.energy-drop-target]="canAttachEnergyTo(pokemon)"
                      [class.evolution-drop-target]="canEvolvePokemonTo(pokemon)"
                      (dragover)="onPokemonDragOver($event, pokemon)"
                      (drop)="onPokemonDrop($event, pokemon)"
                      (click)="onPlayerActivePokemonClick($event, pokemon, player)"
                    >
                      <ng-container *ngTemplateOutlet="cardFace; context: { cardId: pokemon.cardId, player: player, compact: false, pokemon: pokemon }"></ng-container>
                    </article>
                  </ng-container>
                  <ng-template #noPlayerActive><div class="empty-active">Tu activo visual</div></ng-template>
                </div>
              </div>

              <div class="side-stack right" *ngIf="boardPlayer as player">
                <div class="card-back stack-card"></div>
                <span>Deck {{ player.deckRemaining }}</span>
                <small>Descarte {{ player.discardSize }}</small>
              </div>
            </section>

            <section class="bench-row player-bench" *ngIf="boardPlayer as player">
              <div
                class="slot bench-slot"
                *ngFor="let slot of benchSlots; trackBy: trackByIndex"
                [class.drop-target]="canDropToBench()"
                (dragover)="onBenchDragOver($event)"
                (drop)="onBenchDrop($event)"
              >
                <ng-container *ngIf="player.benchPokemon[slot] as pokemon; else emptyPlayerBench">
                  <article
                    class="tcg-card bench-card"
                    [class.energy-drop-target]="canAttachEnergyTo(pokemon)"
                    [class.evolution-drop-target]="canEvolvePokemonTo(pokemon)"
                    [draggable]="player.id === currentPlayer?.id"
                    (dragover)="onPokemonDragOver($event, pokemon)"
                    (drop)="onPokemonDrop($event, pokemon)"
                    (dragstart)="onBenchPokemonDragStart($event, pokemon)"
                    (dragend)="clearDragState()"
                    (click)="openCardDetail(pokemon.cardId, pokemon)"
                  >
                    <ng-container *ngTemplateOutlet="cardFace; context: { cardId: pokemon.cardId, player: player, compact: true, pokemon: pokemon }"></ng-container>
                  </article>
                </ng-container>
                <ng-template #emptyPlayerBench><span>Banca</span></ng-template>
              </div>
            </section>

            <section class="player-band local" *ngIf="boardPlayer as player">
              <div class="player-name" [class.active-turn]="player.id === selectedGame.currentPlayerId">
                <span>Jugador</span>
                <strong>{{ player.playerName }}</strong>
              </div>
              <div class="player-stats">
                <span>Mano {{ player.handSize }}</span>
                <span>Premios {{ player.prizeCardsRemaining }}</span>
                <span>Banca {{ player.benchSize }}/5</span>
                <span>Energia turno {{ player.energyAttachedThisTurn ? 'si' : 'no' }}</span>
              </div>
            </section>

            <section class="hand-fan" *ngIf="boardPlayer as player">
              <article
                class="tcg-card hand-card"
                *ngFor="let cardId of player.handCardIds; let i = index; trackBy: trackByIndexedCardId"
                [style.transform]="handTransform(i, player.handCardIds.length)"
                [class.selectable]="isBasicPokemon(cardId) || isAttachableEnergy(cardId) || isEvolutionPokemon(cardId)"
                [class.energy-card]="isAttachableEnergy(cardId)"
                [class.evolution-card]="isEvolutionPokemon(cardId)"
                [draggable]="isDraggableHandCard(cardId)"
                (dragstart)="onHandCardDragStart($event, cardId)"
                (dragend)="clearDragState()"
                (click)="openCardDetail(cardId)"
              >
                <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: player, compact: true }"></ng-container>
              </article>
            </section>
          </section>

          <aside class="floating-actions visual-actions" *ngIf="currentPlayer">
            <strong>{{ currentPlayer.playerName }}</strong>
            <span>Estado: {{ selectedGame.status }}</span>
            <span>Fase: {{ selectedGame.turnPhase || '-' }}</span>
            <span>{{ selectedGame.turnPhase === 'DRAW' ? 'Robando carta automaticamente...' : 'Arrastra cartas o toca tu activo.' }}</span>
            <div class="action-debug" *ngIf="showDebugInfo">
              <span>Estado: {{ selectedGame.status }}</span>
              <span>Fase: {{ selectedGame.turnPhase }}</span>
              <span>CurrentPlayerId: {{ selectedGame.currentPlayerId }}</span>
              <span>Jugador actual: {{ currentPlayer.playerName }}</span>
            </div>
            <div class="action-debug attack-debug" *ngIf="showDebugInfo">
              <span>Ataque seleccionado: {{ selectedAttackName || '-' }}</span>
              <span>Ataques disponibles: {{ activePokemonAttacks.length }}</span>
              <span>Activo actual: {{ activePokemonCardId || '-' }}</span>
              <span>Activo rival: {{ opponentActivePokemonCardId || '-' }}</span>
              <span>Puede atacar: {{ canAttack() ? 'si' : 'no' }}</span>
            </div>
            <button type="button" class="end-turn" (click)="endTurn()" [disabled]="!canEndTurn()">
              Acabar turno
            </button>
          </aside>

          <details class="debug-panel" *ngIf="showDebugInfo">
            <summary>Detalles / Log</summary>
            <div class="debug-grid">
              <section>
                <h3>Jugadores</h3>
                <article *ngFor="let player of selectedGame.players; trackBy: trackByPlayerId">
                  <strong>{{ player.playerName }} #{{ player.id }}</strong>
                  <p>Deck {{ player.deckRemaining }} - Mano {{ player.handSize }} - Premios {{ player.prizeCardsRemaining }} - Descarte {{ player.discardSize }}</p>
                  <p>Activo instancia: {{ player.activePokemonInstanceId || '-' }}</p>
                  <p>Activo carta: {{ player.activePokemon?.cardId || player.activePokemonCardId || '-' }}</p>
                  <p>Mano IDs: {{ player.handCardIds.join(', ') || '-' }}</p>
                  <p>Banca instancias: {{ pokemonInstanceIds(player.benchPokemon) }}</p>
                  <p>Banca cartas: {{ player.benchCardIds.join(', ') || '-' }}</p>
                </article>
              </section>
              <section>
                <h3>Log</h3>
                <p *ngIf="selectedGame.logs.length === 0" class="muted">Sin acciones registradas.</p>
                <article class="log-row" *ngFor="let log of selectedGame.logs.slice().reverse(); trackBy: trackByLogId">
                  <strong>{{ log.actionType }}</strong>
                  <span>{{ log.message }}</span>
                </article>
              </section>
            </div>
          </details>
        </section>

        <ng-template #emptyBoard>
          <section class="empty-state">
            <div class="pokeball-mark large"></div>
            <h2>Selecciona o crea una partida</h2>
            <p>Usa la columna izquierda para crear una partida, unirte con un segundo jugador o abrir una partida guardada.</p>
          </section>
        </ng-template>
      </section>

      <section class="attack-overlay-backdrop" *ngIf="selectedAttackPokemon as pokemon" (click)="closeAttackOverlay()">
        <article class="attack-overlay-panel" (click)="$event.stopPropagation()">
          <button type="button" class="modal-close" (click)="closeAttackOverlay()">X</button>
          <div class="attack-card-preview">
            <img *ngIf="cardLargeImageUrl(pokemon.cardId)" [src]="cardLargeImageUrl(pokemon.cardId)" [alt]="cardName(pokemon.cardId)" />
            <div class="attack-card-placeholder" *ngIf="!cardLargeImageUrl(pokemon.cardId)">
              <strong>{{ cardName(pokemon.cardId) }}</strong>
              <span>{{ cardType(pokemon.cardId) }}</span>
            </div>
            <button type="button" class="detail-link" (click)="openAttackPokemonDetail(pokemon)">Ver detalle</button>
          </div>
          <div class="attack-menu">
            <p class="eyebrow">Ataques disponibles</p>
            <h2>{{ cardName(pokemon.cardId) }}</h2>
            <p class="muted">PS {{ pokemon.remainingHp }}/{{ pokemon.hp || '-' }} · Energias {{ pokemon.attachedEnergyCount }}</p>

            <button
              type="button"
              class="attack-choice"
              *ngFor="let attack of selectedAttackPokemonAttacks; let i = index; trackBy: trackByAttackName"
              [disabled]="attackInFlight"
              (click)="useAttack(attack, i)"
            >
              <span class="attack-cost">{{ attackCostText(attack) }}</span>
              <strong>{{ attack.name || 'Ataque' }}</strong>
              <span class="attack-damage">{{ attack.damage || '-' }}</span>
              <small *ngIf="attack.text">{{ attack.text }}</small>
            </button>
          </div>
        </article>
      </section>

      <section class="card-modal-backdrop" *ngIf="selectedCardDetail as detail" (click)="closeCardDetail()">
        <article class="card-modal" (click)="$event.stopPropagation()">
          <button type="button" class="modal-close" (click)="closeCardDetail()">X</button>
          <img *ngIf="cardDetailImage(detail) as detailImage" [src]="detailImage" [alt]="cardDetailName(detail)" />
          <div>
            <p class="eyebrow">Detalle de carta</p>
            <h2>{{ cardDetailName(detail) }}</h2>
            <p class="muted">ID {{ detail.cardId }}</p>
            <p>{{ cardDetailType(detail) }}</p>
            <p *ngIf="detail.card?.subtypes?.length">Subtipos: {{ detail.card?.subtypes?.join(', ') }}</p>
            <p *ngIf="detail.card?.evolvesFrom">Evoluciona de: {{ detail.card?.evolvesFrom }}</p>
            <p *ngIf="detail.card?.hp">HP {{ detail.card?.hp }}</p>
            <p *ngIf="detail.card?.types?.length">Tipos: {{ detail.card?.types?.join(', ') }}</p>
            <p *ngIf="detail.pokemon">Daño actual: {{ detail.pokemon.damage }} - Energías: {{ detail.pokemon.attachedEnergyCount }}</p>
            <section *ngIf="detail.card?.attacks?.length">
              <h3>Ataques</h3>
              <article *ngFor="let attack of detail.card?.attacks; trackBy: trackByAttackName">
                <strong>{{ attackLabel(attack) }}</strong>
                <p *ngIf="attack.text">{{ attack.text }}</p>
              </article>
            </section>
            <p *ngIf="detail.card?.weaknesses">Debilidad: {{ formatModifierList(detail.card?.weaknesses) }}</p>
            <p *ngIf="detail.card?.resistances">Resistencia: {{ formatModifierList(detail.card?.resistances) }}</p>
          </div>
        </article>
      </section>

      <ng-template #cardFace let-cardId="cardId" let-player="player" let-compact="compact" let-pokemon="pokemon">
        <ng-container *ngIf="cardImageUrl(cardId) as imageUrl; else cardPlaceholder">
          <img [src]="imageUrl" [alt]="cardName(cardId)" />
        </ng-container>
        <ng-template #cardPlaceholder>
          <div class="card-placeholder">
            <strong>{{ cardName(cardId) }}</strong>
            <span>{{ cardId }}</span>
            <small>{{ cardType(cardId) }}</small>
          </div>
        </ng-template>
        <div class="card-caption" *ngIf="!compact">
          <strong>{{ cardName(cardId) }}</strong>
          <span>{{ cardType(cardId) }}</span>
          <span *ngIf="cardHp(cardId)">HP {{ remainingHp(player, cardId, pokemon) }}/{{ cardHp(cardId) }}</span>
          <small>Energia: {{ attachedEnergyCount(player, cardId, pokemon) }}</small>
        </div>
        <span class="energy-badge" *ngIf="attachedEnergyCount(player, cardId, pokemon) > 0">{{ attachedEnergyCount(player, cardId, pokemon) }}</span>
        <span class="damage-badge" *ngIf="damageFor(player, cardId, pokemon) > 0">{{ damageFor(player, cardId, pokemon) }} dmg</span>
      </ng-template>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      min-height: calc(100vh - 76px);
      background:
        radial-gradient(circle at 50% 12%, rgba(255, 255, 255, 0.95), rgba(219, 234, 254, 0.76) 28%, transparent 52%),
        linear-gradient(160deg, #dff5ff 0%, #f7fbff 42%, #d5ecff 100%);
    }

    .game-page {
      max-width: 1480px;
    }

    .game-header {
      align-items: center;
      display: flex;
      gap: 1rem;
      justify-content: space-between;
      margin-bottom: 1rem;
    }

    .eyebrow {
      color: #0284c7;
      font-size: 0.78rem;
      font-weight: 900;
      letter-spacing: 0.1em;
      margin: 0 0 0.35rem;
      text-transform: uppercase;
    }

    h1,
    h2,
    h3,
    p {
      margin-top: 0;
    }

    h1 {
      font-size: clamp(2rem, 5vw, 4rem);
      line-height: 1;
      margin-bottom: 0.4rem;
    }

    button,
    select,
    input {
      font: inherit;
    }

    select {
      border: 1px solid rgba(125, 211, 252, 0.7);
      border-radius: 14px;
      padding: 0.65rem 0.75rem;
      width: 100%;
    }

    .ghost-button,
    .compact-form button,
    .floating-actions button,
    .match-strip button {
      background: linear-gradient(135deg, #0ea5e9, #2563eb);
      border: 1px solid rgba(255, 255, 255, 0.7);
      border-radius: 999px;
      box-shadow: 0 12px 22px rgba(37, 99, 235, 0.22);
    }

    .game-alert {
      border-radius: 18px;
      box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
      margin-bottom: 0.9rem;
    }

    .warning {
      background: #fef3c7;
      border: 1px solid #fde68a;
      color: #92400e;
      padding: 0.8rem 1rem;
    }

    .victory {
      background: #dcfce7;
      border: 1px solid #86efac;
      color: #166534;
      font-weight: 900;
      padding: 0.8rem 1rem;
    }

    .game-shell {
      align-items: start;
      display: grid;
      gap: 1rem;
      grid-template-columns: 280px minmax(0, 1fr);
    }

    .control-rail {
      display: grid;
      gap: 0.85rem;
      position: sticky;
      top: 92px;
    }

    .glass-card,
    .debug-panel,
    .empty-state {
      background: rgba(255, 255, 255, 0.76);
      border: 1px solid rgba(186, 230, 253, 0.92);
      border-radius: 26px;
      box-shadow: 0 22px 45px rgba(15, 23, 42, 0.12);
      backdrop-filter: blur(16px);
    }

    .glass-card {
      padding: 1rem;
    }

    .rail-title {
      align-items: center;
      display: flex;
      gap: 0.8rem;
      justify-content: space-between;
      margin-bottom: 0.75rem;
    }

    .rail-title h2 {
      font-size: 1rem;
      margin: 0;
    }

    .rail-title span {
      background: #e0f2fe;
      border-radius: 999px;
      color: #0369a1;
      font-size: 0.76rem;
      font-weight: 900;
      padding: 0.25rem 0.55rem;
    }

    .compact-form form,
    .floating-actions {
      display: grid;
      gap: 0.7rem;
    }

    label {
      color: #334155;
      display: grid;
      font-size: 0.82rem;
      font-weight: 900;
      gap: 0.32rem;
    }

    .saved-game {
      background: rgba(255, 255, 255, 0.78);
      border: 1px solid #bae6fd;
      border-radius: 18px;
      box-shadow: none;
      color: #0f172a;
      display: grid;
      gap: 0.14rem;
      justify-items: start;
      margin-bottom: 0.55rem;
      padding: 0.75rem;
      text-align: left;
      width: 100%;
    }

    .saved-game.selected,
    .saved-game:hover {
      background: linear-gradient(135deg, #e0f2fe, #ffffff);
      border-color: #38bdf8;
      transform: translateY(-1px);
    }

    .saved-game small {
      color: #64748b;
    }

    .board-wrap {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(0, 1fr);
    }

    .battle-table {
      background:
        linear-gradient(90deg, rgba(14, 165, 233, 0.12), transparent 18%, transparent 82%, rgba(14, 165, 233, 0.14)),
        radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.95) 0 13%, rgba(219, 234, 254, 0.92) 14% 32%, rgba(125, 211, 252, 0.34) 33% 34%, transparent 35%),
        linear-gradient(180deg, #dbeafe 0%, #f8fbff 49%, #dff4ff 100%);
      border: 1px solid rgba(255, 255, 255, 0.9);
      border-radius: 42px;
      box-shadow: inset 0 0 70px rgba(14, 165, 233, 0.16), 0 28px 70px rgba(15, 23, 42, 0.18);
      min-height: 940px;
      overflow: hidden;
      padding: 1rem 1.1rem 1.4rem;
      position: relative;
    }

    .table-aura {
      border: 18px solid rgba(255, 255, 255, 0.36);
      border-left-color: rgba(56, 189, 248, 0.22);
      border-right-color: rgba(56, 189, 248, 0.22);
      border-radius: 50%;
      height: 720px;
      left: 50%;
      pointer-events: none;
      position: absolute;
      top: 50%;
      transform: translate(-50%, -50%);
      width: min(86%, 820px);
    }

    .draw-animation-card {
      animation: draw-card-flight 680ms cubic-bezier(0.2, 0.8, 0.25, 1) forwards;
      left: calc(100% - 170px);
      pointer-events: none;
      position: absolute;
      top: 52%;
      z-index: 30;
    }

    .flying-draw-card {
      height: 112px;
      width: 78px;
    }

    .match-strip {
      align-items: center;
      background: rgba(15, 23, 42, 0.72);
      border: 1px solid rgba(255, 255, 255, 0.45);
      border-radius: 26px;
      color: white;
      display: flex;
      gap: 1rem;
      justify-content: space-between;
      padding: 0.8rem 0.9rem;
      position: relative;
      z-index: 2;
    }

    .match-strip div:first-child {
      display: grid;
      gap: 0.1rem;
    }

    .phase-pills {
      display: flex;
      flex-wrap: wrap;
      gap: 0.45rem;
      justify-content: center;
    }

    .phase-pills span,
    .player-stats span,
    .zone-token {
      background: rgba(255, 255, 255, 0.82);
      border: 1px solid rgba(186, 230, 253, 0.85);
      border-radius: 999px;
      color: #075985;
      font-size: 0.8rem;
      font-weight: 900;
      padding: 0.34rem 0.62rem;
    }

    .player-band {
      align-items: center;
      display: grid;
      gap: 0.75rem;
      grid-template-columns: 180px 1fr auto;
      padding: 0.75rem 0.4rem;
      position: relative;
      z-index: 2;
    }

    .local {
      grid-template-columns: 180px 1fr;
      margin-top: 0.4rem;
    }

    .player-name {
      background: rgba(15, 23, 42, 0.68);
      border-radius: 0 999px 999px 0;
      color: white;
      display: grid;
      padding: 0.65rem 1rem;
    }

    .player-name span {
      color: #bae6fd;
      font-size: 0.72rem;
      font-weight: 900;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .player-name.active-turn {
      box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.34), 0 0 26px rgba(34, 211, 238, 0.65);
    }

    .hidden-hand,
    .player-stats,
    .side-zones {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .card-back {
      background:
        radial-gradient(circle at 50% 47%, #f8fafc 0 13%, #ef4444 14% 17%, #e2e8f0 18% 23%, transparent 24%),
        radial-gradient(circle at 50% 47%, transparent 0 28%, rgba(255, 255, 255, 0.2) 29% 31%, transparent 32%),
        linear-gradient(145deg, #1d4ed8, #0284c7 48%, #1e3a8a);
      border: 4px solid #dbeafe;
      border-radius: 10px;
      box-shadow: 0 8px 18px rgba(15, 23, 42, 0.22);
    }

    .card-back.mini {
      height: 74px;
      margin-left: -10px;
      transform: rotate(-4deg);
      width: 52px;
    }

    .card-back.mini:nth-child(even) {
      transform: rotate(5deg);
    }

    .more-cards {
      color: #075985;
      font-weight: 900;
    }

    .bench-row {
      display: grid;
      gap: 0.65rem;
      grid-template-columns: repeat(5, minmax(74px, 1fr));
      margin: 0 auto;
      max-width: 760px;
      position: relative;
      z-index: 2;
    }

    .slot {
      align-items: center;
      background: rgba(255, 255, 255, 0.32);
      border: 2px solid rgba(255, 255, 255, 0.64);
      border-radius: 24px;
      display: grid;
      justify-items: center;
      min-height: 132px;
      padding: 0.4rem;
    }

    .drop-target {
      background: rgba(220, 252, 231, 0.66);
      border-color: #22c55e;
      box-shadow: inset 0 0 0 3px rgba(34, 197, 94, 0.24), 0 12px 28px rgba(34, 197, 94, 0.22);
    }

    .slot > span,
    .empty-active {
      color: rgba(51, 65, 85, 0.55);
      font-size: 0.82rem;
      font-weight: 900;
      text-transform: uppercase;
    }

    .active-lane {
      align-items: center;
      display: grid;
      gap: 1rem;
      grid-template-columns: 120px minmax(0, 1fr) 120px;
      margin: 0.75rem 0;
      min-height: 410px;
      position: relative;
      z-index: 2;
    }

    .active-column {
      align-items: center;
      display: grid;
      gap: 0.75rem;
      justify-items: center;
    }

    .active-slot {
      align-items: center;
      display: grid;
      min-height: 168px;
      place-items: center;
      width: 100%;
    }

    .center-field {
      align-items: center;
      background: rgba(255, 255, 255, 0.78);
      border: 1px solid rgba(125, 211, 252, 0.85);
      border-radius: 999px;
      box-shadow: 0 18px 35px rgba(14, 165, 233, 0.16);
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      min-width: min(92%, 520px);
      padding: 0.7rem 1rem;
      text-align: center;
    }

    .center-field span {
      color: #64748b;
      font-size: 0.86rem;
      font-weight: 800;
    }

    .pokeball-mark {
      background: linear-gradient(#ef4444 0 48%, #0f172a 49% 52%, #ffffff 53%);
      border: 3px solid #0f172a;
      border-radius: 50%;
      box-shadow: inset 0 0 0 7px rgba(255, 255, 255, 0.65);
      height: 42px;
      width: 42px;
    }

    .pokeball-mark.large {
      height: 82px;
      margin: 0 auto 1rem;
      width: 82px;
    }

    .side-stack {
      align-items: center;
      color: #075985;
      display: grid;
      font-weight: 900;
      gap: 0.45rem;
      justify-items: center;
      text-align: center;
    }

    .stack-card {
      height: 112px;
      width: 78px;
    }

    .tcg-card {
      background: #ffffff;
      border: 4px solid rgba(255, 255, 255, 0.92);
      border-radius: 14px;
      box-shadow: 0 15px 30px rgba(15, 23, 42, 0.22);
      display: grid;
      overflow: hidden;
      position: relative;
    }

    .tcg-card img {
      display: block;
      height: 100%;
      object-fit: cover;
      width: 100%;
    }

    .active-card {
      height: 194px;
      width: 138px;
    }

    .bench-card {
      height: 118px;
      width: 84px;
    }

    .hand-card {
      flex: 0 0 auto;
      height: 156px;
      margin-left: -26px;
      transform-origin: bottom center;
      transition: transform 160ms ease, z-index 160ms ease;
      width: 110px;
    }

    .hand-card:first-child {
      margin-left: 0;
    }

    .hand-card:hover {
      transform: translateY(-36px) scale(1.12) !important;
      z-index: 20;
    }

    .hand-card.selectable {
      box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.42), 0 18px 32px rgba(14, 165, 233, 0.28);
    }

    .hand-card.energy-card {
      box-shadow: 0 0 0 3px rgba(250, 204, 21, 0.46), 0 18px 32px rgba(234, 179, 8, 0.28);
    }

    .hand-card.evolution-card {
      box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.42), 0 18px 32px rgba(124, 58, 237, 0.26);
    }

    .attack-source {
      cursor: pointer;
    }

    .attack-source:hover {
      transform: translateY(-4px) scale(1.03);
      transition: transform 160ms ease;
    }

    .attack-lunge {
      animation: pokemon-attack-lunge 560ms cubic-bezier(0.2, 0.88, 0.24, 1.12);
      z-index: 15;
    }

    .attack-hit {
      animation: pokemon-hit-shake 560ms ease-in-out;
      z-index: 14;
    }

    .energy-drop-target {
      border-color: #facc15;
      box-shadow: 0 0 0 5px rgba(250, 204, 21, 0.34), 0 0 34px rgba(250, 204, 21, 0.58), 0 18px 35px rgba(15, 23, 42, 0.2);
    }

    .evolution-drop-target {
      border-color: #a855f7;
      box-shadow: 0 0 0 5px rgba(168, 85, 247, 0.3), 0 0 34px rgba(124, 58, 237, 0.5), 0 18px 35px rgba(15, 23, 42, 0.2);
    }

    .glow {
      box-shadow: 0 0 0 5px rgba(34, 211, 238, 0.38), 0 0 34px rgba(34, 211, 238, 0.72), 0 18px 35px rgba(15, 23, 42, 0.2);
    }

    .card-placeholder {
      align-content: center;
      background: linear-gradient(150deg, #fefce8, #e0f2fe);
      color: #0f172a;
      display: grid;
      gap: 0.2rem;
      height: 100%;
      padding: 0.45rem;
      text-align: center;
      width: 100%;
    }

    .card-placeholder strong {
      font-size: 0.8rem;
      line-height: 1.1;
    }

    .card-placeholder span,
    .card-placeholder small {
      color: #64748b;
      font-size: 0.68rem;
    }

    .card-caption {
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.98));
      bottom: 0;
      color: #0f172a;
      display: grid;
      gap: 0.05rem;
      left: 0;
      padding: 0.35rem 0.45rem;
      position: absolute;
      right: 0;
    }

    .card-caption strong {
      font-size: 0.75rem;
      line-height: 1;
    }

    .card-caption span,
    .card-caption small {
      color: #475569;
      font-size: 0.66rem;
    }

    .energy-badge {
      align-items: center;
      background: #facc15;
      border: 2px solid white;
      border-radius: 50%;
      bottom: 0.25rem;
      box-shadow: 0 4px 10px rgba(15, 23, 42, 0.22);
      color: #713f12;
      display: flex;
      font-size: 0.75rem;
      font-weight: 900;
      height: 24px;
      justify-content: center;
      position: absolute;
      right: 0.25rem;
      width: 24px;
    }

    .hand-fan {
      align-items: flex-end;
      display: flex;
      justify-content: center;
      min-height: 182px;
      overflow: visible;
      padding: 1rem 1rem 0;
      position: relative;
      z-index: 5;
    }

    .floating-actions {
      background: rgba(255, 255, 255, 0.86);
      border: 1px solid rgba(186, 230, 253, 0.95);
      border-radius: 28px;
      box-shadow: 0 24px 55px rgba(15, 23, 42, 0.16);
      bottom: 1.25rem;
      display: grid;
      gap: 0.45rem;
      min-width: 210px;
      padding: 1rem;
      position: fixed;
      right: 1.25rem;
      z-index: 60;
    }

    .floating-actions strong {
      color: #075985;
      font-size: 1.1rem;
    }

    .visual-actions > span {
      color: #475569;
      font-size: 0.82rem;
      font-weight: 800;
    }

    .action-debug {
      background: rgba(224, 242, 254, 0.8);
      border: 1px solid rgba(125, 211, 252, 0.8);
      border-radius: 16px;
      color: #075985;
      display: grid;
      font-size: 0.78rem;
      font-weight: 800;
      gap: 0.2rem;
      padding: 0.65rem;
    }

    .floating-actions .end-turn {
      background: linear-gradient(135deg, #22c55e, #0ea5e9);
      font-size: 1rem;
      margin-top: 0.25rem;
      padding: 0.95rem 1rem;
    }

    .damage-badge {
      background: #ef4444;
      border: 2px solid white;
      border-radius: 999px;
      box-shadow: 0 4px 10px rgba(15, 23, 42, 0.22);
      color: white;
      font-size: 0.7rem;
      font-weight: 900;
      left: 0.25rem;
      padding: 0.16rem 0.4rem;
      position: absolute;
      top: 0.25rem;
    }

    .debug-panel {
      grid-column: 1 / -1;
      padding: 1rem;
    }

    .debug-panel summary {
      color: #075985;
      cursor: pointer;
      font-weight: 900;
    }

    .debug-grid {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      margin-top: 1rem;
    }

    .debug-grid article,
    .log-row {
      border-bottom: 1px solid #e0f2fe;
      padding: 0.65rem 0;
    }

    .debug-grid p,
    .log-row span {
      color: #475569;
      font-size: 0.88rem;
      margin-bottom: 0.25rem;
    }

    .empty-state {
      align-self: stretch;
      display: grid;
      min-height: 620px;
      padding: 2rem;
      place-content: center;
      text-align: center;
    }

    .empty-state h2 {
      font-size: clamp(1.8rem, 4vw, 3rem);
      margin-bottom: 0.6rem;
    }

    .card-modal-backdrop {
      align-items: center;
      background: rgba(15, 23, 42, 0.58);
      display: flex;
      inset: 0;
      justify-content: center;
      padding: 1rem;
      position: fixed;
      z-index: 100;
    }

    .attack-overlay-backdrop {
      align-items: center;
      background: rgba(15, 23, 42, 0.66);
      display: flex;
      inset: 0;
      justify-content: center;
      padding: 1rem;
      position: fixed;
      z-index: 110;
    }

    .attack-overlay-panel {
      background:
        radial-gradient(circle at 22% 12%, rgba(250, 204, 21, 0.22), transparent 34%),
        linear-gradient(145deg, rgba(248, 250, 252, 0.98), rgba(219, 234, 254, 0.96));
      border: 1px solid rgba(255, 255, 255, 0.88);
      border-radius: 34px;
      box-shadow: 0 32px 90px rgba(15, 23, 42, 0.44);
      display: grid;
      gap: 1.25rem;
      grid-template-columns: minmax(190px, 300px) minmax(280px, 1fr);
      max-height: min(90vh, 820px);
      max-width: min(94vw, 900px);
      overflow: auto;
      padding: 1.25rem;
      position: relative;
    }

    .attack-card-preview {
      align-content: start;
      display: grid;
      gap: 0.75rem;
      justify-items: center;
    }

    .attack-card-preview img,
    .attack-card-placeholder {
      background: white;
      border: 6px solid rgba(255, 255, 255, 0.94);
      border-radius: 22px;
      box-shadow: 0 20px 48px rgba(15, 23, 42, 0.28);
      width: min(100%, 270px);
    }

    .attack-card-placeholder {
      align-content: center;
      display: grid;
      min-height: 360px;
      padding: 1rem;
      text-align: center;
    }

    .detail-link {
      background: rgba(15, 23, 42, 0.86);
      border: 1px solid rgba(255, 255, 255, 0.7);
      border-radius: 999px;
      color: white;
      font-weight: 900;
      padding: 0.7rem 1rem;
    }

    .attack-menu {
      display: grid;
      gap: 0.8rem;
    }

    .attack-menu h2 {
      font-size: clamp(1.6rem, 3vw, 2.6rem);
      margin: 0;
    }

    .attack-choice {
      align-items: center;
      background: linear-gradient(135deg, #ffffff, #e0f2fe);
      border: 1px solid rgba(56, 189, 248, 0.7);
      border-radius: 22px;
      box-shadow: 0 12px 28px rgba(14, 165, 233, 0.16);
      color: #0f172a;
      display: grid;
      gap: 0.35rem;
      grid-template-columns: minmax(90px, auto) 1fr auto;
      padding: 0.9rem 1rem;
      text-align: left;
    }

    .attack-choice:hover {
      border-color: #facc15;
      box-shadow: 0 0 0 4px rgba(250, 204, 21, 0.22), 0 14px 30px rgba(14, 165, 233, 0.2);
      transform: translateY(-1px);
    }

    .attack-choice:disabled {
      cursor: wait;
      opacity: 0.72;
      transform: none;
    }

    .attack-choice small {
      color: #475569;
      grid-column: 2 / -1;
      line-height: 1.35;
    }

    .attack-cost {
      background: #fef3c7;
      border: 1px solid #facc15;
      border-radius: 999px;
      color: #713f12;
      font-size: 0.78rem;
      font-weight: 900;
      padding: 0.32rem 0.62rem;
      text-align: center;
    }

    .attack-damage {
      color: #dc2626;
      font-size: 1.25rem;
      font-weight: 1000;
    }

    .card-modal {
      background: rgba(255, 255, 255, 0.96);
      border: 1px solid rgba(186, 230, 253, 0.9);
      border-radius: 28px;
      box-shadow: 0 28px 80px rgba(15, 23, 42, 0.35);
      display: grid;
      gap: 1.25rem;
      grid-template-columns: minmax(180px, 260px) minmax(0, 1fr);
      max-height: min(88vh, 760px);
      max-width: min(92vw, 820px);
      overflow: auto;
      padding: 1.25rem;
      position: relative;
    }

    .card-modal img {
      border-radius: 18px;
      box-shadow: 0 18px 36px rgba(15, 23, 42, 0.24);
      width: 100%;
    }

    .card-modal h2 {
      margin-bottom: 0.3rem;
    }

    .card-modal section {
      display: grid;
      gap: 0.55rem;
      margin-top: 0.8rem;
    }

    .card-modal article {
      border-top: 1px solid #e0f2fe;
      padding-top: 0.55rem;
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

    @media (max-width: 1180px) {
      .game-shell,
      .board-wrap {
        grid-template-columns: 1fr;
      }

      .control-rail {
        position: static;
      }

      .control-rail {
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      }
    }

    @media (max-width: 760px) {
      .game-page {
        padding: 1rem 0.6rem;
      }

      .game-header,
      .match-strip,
      .player-band {
        align-items: stretch;
        flex-direction: column;
        grid-template-columns: 1fr;
      }

      .battle-table {
        border-radius: 28px;
        min-height: 880px;
        padding: 0.7rem;
      }

      .bench-row {
        gap: 0.35rem;
        grid-template-columns: repeat(5, minmax(52px, 1fr));
      }

      .slot {
        border-radius: 16px;
        min-height: 102px;
        padding: 0.25rem;
      }

      .active-lane {
        grid-template-columns: 74px minmax(0, 1fr) 74px;
        min-height: 360px;
      }

      .stack-card {
        height: 86px;
        width: 60px;
      }

      .active-card {
        height: 168px;
        width: 120px;
      }

      .bench-card {
        height: 88px;
        width: 62px;
      }

      .hand-card {
        height: 124px;
        margin-left: -42px;
        width: 88px;
      }

      .center-field {
        border-radius: 22px;
        display: grid;
        min-width: 0;
      }

      .card-modal {
        grid-template-columns: 1fr;
        max-width: 96vw;
      }

      .attack-overlay-panel {
        grid-template-columns: 1fr;
        max-width: 96vw;
      }

      .card-modal img {
        margin: 0 auto;
        max-width: 240px;
      }

      .attack-card-preview img,
      .attack-card-placeholder {
        max-width: 240px;
      }

      .attack-choice {
        grid-template-columns: 1fr auto;
      }

      .attack-cost,
      .attack-choice small {
        grid-column: 1 / -1;
      }

      .floating-actions {
        bottom: 0.7rem;
        left: 0.7rem;
        right: 0.7rem;
      }
    }

    @keyframes pokemon-attack-lunge {
      0% {
        transform: translateY(0) scale(1);
      }
      38% {
        filter: brightness(1.12);
        transform: translateY(-78px) scale(1.08) rotate(-2deg);
      }
      62% {
        transform: translateY(-54px) scale(1.04) rotate(1deg);
      }
      100% {
        filter: brightness(1);
        transform: translateY(0) scale(1);
      }
    }

    @keyframes pokemon-hit-shake {
      0% {
        filter: brightness(1);
        transform: translateX(0);
      }
      18% {
        filter: brightness(1.75) saturate(1.35);
        transform: translateX(-8px) rotate(-2deg);
      }
      34% {
        transform: translateX(10px) rotate(2deg);
      }
      50% {
        box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.32), 0 0 32px rgba(239, 68, 68, 0.55), 0 18px 35px rgba(15, 23, 42, 0.2);
        transform: translateX(-6px) rotate(-1deg);
      }
      70% {
        transform: translateX(4px) rotate(1deg);
      }
      100% {
        filter: brightness(1);
        transform: translateX(0);
      }
    }

    @keyframes draw-card-flight {
      0% {
        opacity: 0;
        transform: translate(0, -28px) scale(0.82) rotate(7deg);
      }
      14% {
        opacity: 1;
      }
      72% {
        opacity: 1;
        transform: translate(-42vw, 210px) scale(1.05) rotate(-10deg);
      }
      100% {
        opacity: 0;
        transform: translate(-46vw, 240px) scale(0.92) rotate(-14deg);
      }
    }
  `]
})
export class GamesPageComponent implements OnInit {
  private readonly attackAnimationMs = 560;
  private readonly drawAnimationMs = 680;
  private readonly supportedEnergyTypes = ['Grass', 'Fire', 'Water', 'Lightning', 'Psychic', 'Fighting', 'Darkness', 'Metal', 'Fairy', 'Dragon', 'Colorless'];
  readonly benchSlots = [0, 1, 2, 3, 4];
  readonly showDebugInfo = false;
  decks: DeckResponse[] = [];
  games: GameResponse[] = [];
  selectedGame: GameResponse | null = null;
  cardsById: Record<string, Card> = {};
  createPlayerName = 'Ash';
  joinPlayerName = 'Misty';
  createDeckId?: number;
  joinDeckId?: number;
  selectedBasicCardId = '';
  selectedEnergyCardId = '';
  selectedTargetPokemonInstanceId = '';
  selectedPromotionPokemonInstanceId = '';
  selectedAttackName = '';
  draggedHandCardId = '';
  draggedEnergyCardId = '';
  draggedEvolutionCardId = '';
  draggedBenchPokemonInstanceId = '';
  selectedAttackPokemon: PokemonInPlayResponse | null = null;
  attackAnimation: AttackAnimationState | null = null;
  drawAnimation: DrawAnimationState | null = null;
  attackInFlight = false;
  selectedCardDetail: SelectedCardDetail | null = null;
  private lastAutoDrawKey = '';
  private autoDrawInFlightKey = '';
  error = '';
  message = '';

  constructor(
    private readonly cardsService: CardsService,
    private readonly decksService: DecksService,
    private readonly gamesService: GamesService
  ) {
  }

  get validDecks(): DeckResponse[] {
    return this.decks.filter((deck) => deck.valid);
  }

  get currentPlayer(): GamePlayerResponse | null {
    if (!this.selectedGame?.currentPlayerId) {
      return null;
    }
    return this.selectedGame.players.find((player) => player.id === this.selectedGame?.currentPlayerId) ?? null;
  }

  get boardPlayer(): GamePlayerResponse | null {
    return this.currentPlayer ?? this.selectedGame?.players[0] ?? null;
  }

  get opponentPlayer(): GamePlayerResponse | null {
    if (!this.selectedGame) {
      return null;
    }
    const boardPlayerId = this.boardPlayer?.id;
    return this.selectedGame.players.find((player) => player.id !== boardPlayerId) ?? this.selectedGame.players[1] ?? null;
  }

  get turnText(): string {
    if (!this.selectedGame) {
      return 'Sin partida seleccionada';
    }
    if (this.selectedGame.status === 'FINISHED') {
      return this.finishedGameText;
    }
    if (this.selectedGame.status === 'WAITING') {
      return 'Esperando jugadores';
    }
    const playerName = this.currentPlayer?.playerName ?? `Jugador ${this.selectedGame.currentPlayerId ?? '-'}`;
    return `Turno de ${playerName}`;
  }

  get finishedGameText(): string {
    if (!this.selectedGame || this.selectedGame.status !== 'FINISHED') {
      return '';
    }
    return `Partida finalizada. Ganador: ${this.winnerName(this.selectedGame)}`;
  }

  get basicCardsInHand(): string[] {
    return this.currentPlayer?.handCardIds.filter((cardId) => this.isBasicPokemon(cardId)) ?? [];
  }

  get energyCardsInHand(): string[] {
    return this.currentPlayer?.handCardIds.filter((cardId) => this.isAttachableEnergy(cardId)) ?? [];
  }

  get energyTargetPokemon(): PokemonInPlayResponse[] {
    if (!this.currentPlayer) {
      return [];
    }
    return [this.currentPlayer.activePokemon, ...this.currentPlayer.benchPokemon]
      .filter((pokemon): pokemon is PokemonInPlayResponse => !!pokemon);
  }

  get activePokemonAttacks(): CardAttack[] {
    const activeCardId = this.activePokemonCardId;
    if (!activeCardId) {
      return [];
    }
    return this.cardsById[activeCardId]?.attacks ?? [];
  }

  get selectedAttackPokemonAttacks(): CardAttack[] {
    if (!this.selectedAttackPokemon) {
      return [];
    }
    return this.attacksForPokemon(this.selectedAttackPokemon);
  }

  get activePokemonCardId(): string {
    return this.currentPlayer?.activePokemon?.cardId ?? '';
  }

  get opponentActivePokemonCardId(): string {
    return this.opponentPlayer?.activePokemon?.cardId ?? '';
  }

  isActiveGame(): boolean {
    return this.selectedGame?.status === 'ACTIVE';
  }

  isDrawPhase(): boolean {
    return this.selectedGame?.turnPhase === 'DRAW';
  }

  isMainPhase(): boolean {
    return this.selectedGame?.turnPhase === 'MAIN';
  }

  canStartGame(): boolean {
    const selectedGame = this.selectedGame;
    return !!selectedGame && selectedGame.status === 'WAITING' && selectedGame.players.length === 2;
  }

  canDrawCard(): boolean {
    return this.isActiveGame()
      && this.isDrawPhase()
      && !!this.selectedGame?.currentPlayerId;
  }

  canPlayBasicPokemon(): boolean {
    return this.isActiveGame()
      && this.isMainPhase()
      && !!this.selectedBasicCardId;
  }

  canAttachEnergy(): boolean {
    return this.isActiveGame()
      && this.isMainPhase()
      && !!this.selectedEnergyCardId
      && !!this.selectedTargetPokemonInstanceId;
  }

  canEndTurn(): boolean {
    return this.isActiveGame() && this.isMainPhase();
  }

  canAttack(): boolean {
    const selectedGame = this.selectedGame;
    const currentPlayer = this.currentPlayer;
    const opponentPlayer = this.opponentPlayer;

    return !!selectedGame
      && this.isActiveGame()
      && this.isMainPhase()
      && !!selectedGame.currentPlayerId
      && !!currentPlayer
      && !!opponentPlayer
      && !!currentPlayer.activePokemon
      && !!opponentPlayer.activePokemon
      && this.activePokemonAttacks.length > 0;
  }

  canPromoteActive(): boolean {
    return this.isActiveGame()
      && !!this.currentPlayer
      && !this.currentPlayer.activePokemon
      && this.currentPlayer.benchPokemon.length > 0;
  }

  canDropToActive(): boolean {
    return this.isActiveGame()
      && this.isMainPhase()
      && !!this.currentPlayer
      && !this.currentPlayer.activePokemon
      && (!!this.draggedBenchPokemonInstanceId || (!!this.draggedHandCardId && this.isBasicPokemon(this.draggedHandCardId)));
  }

  canDropToBench(): boolean {
    return this.isActiveGame()
      && this.isMainPhase()
      && !!this.currentPlayer
      && !!this.draggedHandCardId
      && this.isBasicPokemon(this.draggedHandCardId)
      && this.currentPlayer.benchPokemon.length < this.benchSlots.length;
  }

  canAttachEnergyTo(pokemon: PokemonInPlayResponse): boolean {
    return this.isActiveGame()
      && this.isMainPhase()
      && !!this.currentPlayer
      && !!this.draggedEnergyCardId
      && this.energyTargetPokemon.some((target) => target.instanceId === pokemon.instanceId)
      && this.isEnergyCompatibleWithPokemon(this.draggedEnergyCardId, pokemon.cardId);
  }

  canEvolvePokemonTo(pokemon: PokemonInPlayResponse): boolean {
    if (!this.canDropEvolutionOn(pokemon)) {
      return false;
    }

    const evolutionCard = this.cardsById[this.draggedEvolutionCardId];
    const targetCard = this.cardsById[pokemon.cardId];
    if (!evolutionCard || !targetCard || !evolutionCard.evolvesFrom) {
      return false;
    }
    if (!this.cardNamesMatch(evolutionCard.evolvesFrom, targetCard.name)) {
      return false;
    }
    if (this.hasCardSubtype(evolutionCard, 'Stage 1')) {
      return this.hasCardSubtype(targetCard, 'Basic');
    }
    if (this.hasCardSubtype(evolutionCard, 'Stage 2')) {
      return this.hasCardSubtype(targetCard, 'Stage 1');
    }
    return this.hasCardSubtype(evolutionCard, 'MEGA');
  }

  canDropEvolutionOn(pokemon: PokemonInPlayResponse): boolean {
    if (!this.isActiveGame() || !this.isMainPhase() || !this.currentPlayer || !this.draggedEvolutionCardId) {
      return false;
    }
    return this.energyTargetPokemon.some((target) => target.instanceId === pokemon.instanceId);
  }

  @HostListener('document:keydown.escape')
  closeOverlays(): void {
    this.closeCardDetail();
    this.closeAttackOverlay();
  }

  closeCardDetail(): void {
    this.selectedCardDetail = null;
  }

  ngOnInit(): void {
    this.reloadAll();
  }

  reloadAll(): void {
    this.error = '';
    this.message = '';
    this.loadCards();
    this.loadDecks();
    this.loadGames();
  }

  loadCards(): void {
    this.cardsService.getCards().subscribe({
      next: (cards) => {
        this.cardsById = Object.fromEntries(cards.map((card) => [card.id, card]));
      },
      error: (error) => {
        this.error = this.backendError(error, 'No se pudieron cargar las cartas.');
      }
    });
  }

  loadDecks(): void {
    this.decksService.getDecks().subscribe({
      next: (decks) => {
        this.decks = decks;
        this.createDeckId ??= this.validDecks[0]?.id;
        this.joinDeckId ??= this.validDecks[0]?.id;
      },
      error: (error) => {
        this.error = this.backendError(error, 'No se pudieron cargar los mazos.');
      }
    });
  }

  loadGames(): void {
    this.gamesService.getGames().subscribe({
      next: (games) => {
        this.games = games;
        if (this.selectedGame && !this.hasPendingVisualAnimation()) {
          const selectedGame = games.find((game) => game.id === this.selectedGame?.id);
          if (selectedGame) {
            this.setSelectedGame(selectedGame);
          }
        }
      },
      error: (error) => {
        this.error = this.backendError(error, 'No se pudieron cargar las partidas.');
      }
    });
  }

  selectGame(gameId: number): void {
    this.gamesService.getGame(gameId).subscribe({
      next: (game) => this.setSelectedGame(game),
      error: (error) => {
        this.error = this.backendError(error, 'No se pudo cargar la partida.');
      }
    });
  }

  createGame(): void {
    if (!this.createDeckId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.createGame({ playerName: this.createPlayerName.trim(), deckId: this.createDeckId }),
      'Partida creada.'
    );
  }

  joinGame(): void {
    if (!this.selectedGame || !this.joinDeckId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.joinGame(this.selectedGame.id, { playerName: this.joinPlayerName.trim(), deckId: this.joinDeckId }),
      'Jugador unido.'
    );
  }

  startGame(): void {
    if (!this.selectedGame) {
      return;
    }
    this.runGameMutation(this.gamesService.startGame(this.selectedGame.id), 'Partida iniciada.');
  }

  drawCard(): void {
    if (!this.selectedGame?.currentPlayerId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.drawCard(this.selectedGame.id, { playerId: this.selectedGame.currentPlayerId }),
      'Carta robada.'
    );
  }

  playBasicPokemon(): void {
    if (!this.selectedGame || !this.currentPlayer || !this.selectedBasicCardId) {
      return;
    }
    const targetZone = this.currentPlayer.activePokemon ? 'BENCH' : 'ACTIVE';
    this.playBasicPokemonToZone(this.selectedBasicCardId, targetZone, 'Pokemon jugado.');
  }

  attachEnergy(): void {
    if (!this.selectedGame || !this.currentPlayer || !this.selectedEnergyCardId || !this.selectedTargetPokemonInstanceId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.attachEnergy(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        energyCardId: this.selectedEnergyCardId,
        pokemonInstanceId: this.selectedTargetPokemonInstanceId
      }),
      'Energia unida.'
    );
  }

  promoteActive(): void {
    if (!this.selectedGame || !this.currentPlayer || !this.selectedPromotionPokemonInstanceId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.promoteActive(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        pokemonInstanceId: this.selectedPromotionPokemonInstanceId
      }),
      'Pokemon promovido a activo.'
    );
  }

  endTurn(): void {
    if (!this.selectedGame || !this.currentPlayer) {
      return;
    }
    this.runGameMutation(
      this.gamesService.endTurn(this.selectedGame.id, { playerId: this.currentPlayer.id }),
      'Turno finalizado.'
    );
  }

  attack(): void {
    if (!this.selectedGame?.currentPlayerId || this.activePokemonAttacks.length === 0 || this.attackInFlight) {
      return;
    }
    const request = this.selectedAttackName
      ? { playerId: this.selectedGame.currentPlayerId, attackName: this.selectedAttackName }
      : { playerId: this.selectedGame.currentPlayerId };
    this.runGameMutation(
      this.gamesService.attack(this.selectedGame.id, request),
      'Ataque realizado.',
      'No se pudo atacar.'
    );
  }

  onPlayerActivePokemonClick(event: MouseEvent, pokemon: PokemonInPlayResponse, player: GamePlayerResponse): void {
    event.stopPropagation();
    const blockedReason = this.attackBlockedReason(pokemon, player);
    if (blockedReason) {
      this.error = blockedReason;
      return;
    }
    this.error = '';
    this.selectedAttackPokemon = pokemon;
  }

  closeAttackOverlay(): void {
    this.selectedAttackPokemon = null;
  }

  openAttackPokemonDetail(pokemon: PokemonInPlayResponse): void {
    this.closeAttackOverlay();
    this.openCardDetail(pokemon.cardId, pokemon);
  }

  useAttack(attack: CardAttack, attackIndex: number): void {
    if (!this.selectedGame?.currentPlayerId || this.attackInFlight) {
      return;
    }
    const animation = this.currentAttackAnimationState();
    if (!animation) {
      return;
    }
    const request = attack.name
      ? { playerId: this.selectedGame.currentPlayerId, attackName: attack.name }
      : { playerId: this.selectedGame.currentPlayerId, attackIndex };

    this.attackInFlight = true;
    this.error = '';
    this.message = '';
    this.gamesService.attack(this.selectedGame.id, request).subscribe({
      next: (game) => {
        this.selectedAttackPokemon = null;
        this.playAttackAnimation(animation, game);
      },
      error: (error) => {
        this.attackInFlight = false;
        this.error = this.backendError(error, 'No se pudo atacar.');
      }
    });
  }

  attackCostText(attack: CardAttack): string {
    return attack.cost?.length ? attack.cost.map((cost) => `[${cost}]`).join(' ') : 'Sin costo';
  }

  private attackBlockedReason(pokemon: PokemonInPlayResponse, player: GamePlayerResponse): string {
    if (this.attackInFlight || this.attackAnimation) {
      return 'El ataque está en curso.';
    }
    if (!this.selectedGame || this.selectedGame.status !== 'ACTIVE') {
      return 'La partida no está activa.';
    }
    if (!this.currentPlayer || player.id !== this.currentPlayer.id || this.selectedGame.currentPlayerId !== player.id) {
      return 'No es tu turno.';
    }
    if (this.selectedGame.turnPhase === 'DRAW') {
      return 'Primero debes robar carta.';
    }
    if (this.selectedGame.turnPhase !== 'MAIN') {
      return 'La accion solo se puede realizar en fase MAIN.';
    }
    if (!this.opponentPlayer?.activePokemon) {
      return 'El rival no tiene Pokemon activo.';
    }
    if (this.attacksForPokemon(pokemon).length === 0) {
      return 'Este Pokémon no tiene ataques.';
    }
    return '';
  }

  private attacksForPokemon(pokemon: PokemonInPlayResponse): CardAttack[] {
    return this.cardsById[pokemon.cardId]?.attacks ?? [];
  }

  isAttackAttacker(pokemon: PokemonInPlayResponse): boolean {
    return this.attackAnimation?.attackerInstanceId === pokemon.instanceId;
  }

  isAttackDefender(pokemon: PokemonInPlayResponse): boolean {
    return this.attackAnimation?.defenderInstanceId === pokemon.instanceId;
  }

  private currentAttackAnimationState(): AttackAnimationState | null {
    const attacker = this.currentPlayer?.activePokemon;
    const defender = this.opponentPlayer?.activePokemon;
    if (!attacker || !defender) {
      return null;
    }
    return {
      attackerInstanceId: attacker.instanceId,
      defenderInstanceId: defender.instanceId
    };
  }

  private playAttackAnimation(animation: AttackAnimationState, game: GameResponse): void {
    this.attackAnimation = animation;
    window.setTimeout(() => {
      this.attackAnimation = null;
      this.attackInFlight = false;
      this.message = this.mutationMessage(game, 'Ataque realizado.');
      this.setSelectedGame(game);
      this.refreshAfterGameUpdate();
    }, this.attackAnimationMs);
  }

  trackByDeckId(_index: number, deck: DeckResponse): number {
    return deck.id;
  }

  trackByGameId(_index: number, game: GameResponse): number {
    return game.id;
  }

  trackByPlayerId(_index: number, player: GamePlayerResponse): number {
    return player.id;
  }

  trackByLogId(index: number, log: { id?: number }): number {
    return log.id ?? index;
  }

  trackByAttackName(index: number, attack: CardAttack): string {
    return attack.name || String(index);
  }

  trackByPokemonInstanceId(_index: number, pokemon: PokemonInPlayResponse): string {
    return pokemon.instanceId;
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByIndexedCardId(index: number, cardId: string): string {
    return `${cardId}-${index}`;
  }

  cardLabel(cardId: string): string {
    return this.cardsById[cardId]?.name ? `${this.cardsById[cardId].name} (${cardId})` : cardId;
  }

  cardName(cardId: string): string {
    return this.cardsById[cardId]?.name ?? cardId;
  }

  cardType(cardId: string): string {
    return this.formatCardType(this.cardsById[cardId]);
  }

  cardHp(cardId: string): number {
    return this.cardsById[cardId]?.hp ?? 0;
  }

  damageFor(player: GamePlayerResponse, pokemonCardId: string, pokemon?: PokemonInPlayResponse): number {
    if (pokemon) {
      return pokemon.damage;
    }
    return 0;
  }

  remainingHp(player: GamePlayerResponse, pokemonCardId: string, pokemon?: PokemonInPlayResponse): number {
    if (pokemon) {
      return pokemon.remainingHp;
    }
    const hp = this.cardHp(pokemonCardId);
    return Math.max(0, hp - this.damageFor(player, pokemonCardId, pokemon));
  }

  attackLabel(attack: CardAttack): string {
    const cost = attack.cost?.length ? attack.cost.join(', ') : 'Sin costo';
    const damage = attack.damage || '-';
    return `${attack.name} - Costo: ${cost} - Dano: ${damage}`;
  }

  cardImageUrl(cardId: string): string {
    return this.preferredCardImage(this.cardsById[cardId]);
  }

  cardLargeImageUrl(cardId: string): string {
    return this.preferredCardImage(this.cardsById[cardId], true);
  }

  attachedEnergyCount(player: GamePlayerResponse, pokemonCardId: string, pokemon?: PokemonInPlayResponse): number {
    if (pokemon) {
      return pokemon.attachedEnergyCount;
    }
    return 0;
  }

  pokemonOptionLabel(pokemon: PokemonInPlayResponse): string {
    const zone = pokemon.instanceId === this.currentPlayer?.activePokemonInstanceId ? 'Activo' : 'Banca';
    return `${this.cardName(pokemon.cardId)} (${zone})`;
  }

  pokemonInstanceIds(pokemon: PokemonInPlayResponse[]): string {
    return pokemon.length ? pokemon.map((item) => item.instanceId).join(', ') : '-';
  }

  openCardDetail(cardId: string, pokemon?: PokemonInPlayResponse): void {
    this.selectedCardDetail = {
      cardId,
      card: this.cardsById[cardId],
      pokemon
    };
  }

  cardDetailImage(detail: SelectedCardDetail): string {
    return this.preferredCardImage(detail.card, true);
  }

  cardDetailName(detail: SelectedCardDetail): string {
    return detail.card?.name ?? detail.cardId;
  }

  cardDetailType(detail: SelectedCardDetail): string {
    return this.formatCardType(detail.card);
  }

  private formatCardType(card?: Card): string {
    return [card?.supertype, ...(card?.subtypes ?? [])].filter(Boolean).join(' / ') || 'Carta';
  }

  private preferredCardImage(card?: Card, preferLarge = false): string {
    if (preferLarge) {
      return card?.images?.large ?? card?.imageLarge ?? card?.imageLargeUrl ?? card?.images?.small ?? card?.imageSmall ?? card?.imageSmallUrl ?? card?.imageUrl ?? '';
    }
    return card?.images?.small ?? card?.imageSmall ?? card?.imageSmallUrl ?? card?.imageUrl ?? card?.images?.large ?? card?.imageLarge ?? card?.imageLargeUrl ?? '';
  }

  private hasCardSubtype(card: Card, subtype: string): boolean {
    const normalizedSubtype = this.normalizeCardText(subtype);
    return (card.subtypes ?? []).some((value) => this.normalizeCardText(value) === normalizedSubtype);
  }

  private cardNamesMatch(firstName?: string, secondName?: string): boolean {
    return this.normalizeCardText(firstName) === this.normalizeCardText(secondName);
  }

  private normalizeCardText(value?: string): string {
    return value?.trim().toUpperCase() ?? '';
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

  onHandCardDragStart(event: DragEvent, cardId: string): void {
    if (!this.isDraggableHandCard(cardId)) {
      event.preventDefault();
      return;
    }
    this.draggedHandCardId = cardId;
    this.draggedEnergyCardId = this.isAttachableEnergy(cardId) ? cardId : '';
    this.draggedEvolutionCardId = this.isEvolutionPokemon(cardId) ? cardId : '';
    this.draggedBenchPokemonInstanceId = '';
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
    event.dataTransfer?.setData('text/plain', cardId);
  }

  onBenchPokemonDragStart(event: DragEvent, pokemon: PokemonInPlayResponse): void {
    this.draggedBenchPokemonInstanceId = pokemon.instanceId;
    this.draggedHandCardId = '';
    this.draggedEnergyCardId = '';
    this.draggedEvolutionCardId = '';
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
    event.dataTransfer?.setData('text/plain', pokemon.instanceId);
  }

  clearDragState(): void {
    this.draggedHandCardId = '';
    this.draggedEnergyCardId = '';
    this.draggedEvolutionCardId = '';
    this.draggedBenchPokemonInstanceId = '';
  }

  onActiveDragOver(event: DragEvent): void {
    if (this.canDropToActive()) {
      event.preventDefault();
    }
  }

  onBenchDragOver(event: DragEvent): void {
    if (this.canDropToBench()) {
      event.preventDefault();
    }
  }

  onPokemonDragOver(event: DragEvent, pokemon: PokemonInPlayResponse): void {
    if (this.canAttachEnergyTo(pokemon) || this.canDropEvolutionOn(pokemon)) {
      event.preventDefault();
    }
  }

  onPokemonDrop(event: DragEvent, pokemon: PokemonInPlayResponse): void {
    if (!this.canAttachEnergyTo(pokemon) && !this.canDropEvolutionOn(pokemon)) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    if (this.canAttachEnergyTo(pokemon)) {
      this.attachEnergyToPokemon(this.draggedEnergyCardId, pokemon.instanceId);
    } else if (this.canDropEvolutionOn(pokemon)) {
      this.evolvePokemonTo(this.draggedEvolutionCardId, pokemon);
    }
    this.clearDragState();
  }

  onActiveDrop(event: DragEvent): void {
    event.preventDefault();
    if (!this.selectedGame || !this.currentPlayer || !this.canDropToActive()) {
      this.clearDragState();
      return;
    }
    if (this.draggedBenchPokemonInstanceId) {
      this.selectedPromotionPokemonInstanceId = this.draggedBenchPokemonInstanceId;
      this.promoteActive();
    } else if (this.draggedHandCardId) {
      this.playBasicPokemonToZone(this.draggedHandCardId, 'ACTIVE');
    }
    this.clearDragState();
  }

  onBenchDrop(event: DragEvent): void {
    event.preventDefault();
    if (!this.canDropToBench() || !this.draggedHandCardId) {
      this.clearDragState();
      return;
    }
    this.playBasicPokemonToZone(this.draggedHandCardId, 'BENCH');
    this.clearDragState();
  }

  private playBasicPokemonToZone(cardId: string, targetZone: 'ACTIVE' | 'BENCH', successMessage?: string): void {
    if (!this.selectedGame || !this.currentPlayer) {
      return;
    }
    this.runGameMutation(
      this.gamesService.playBasicPokemon(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        cardId,
        targetZone
      }),
      successMessage ?? (targetZone === 'ACTIVE' ? 'Pokemon jugado como activo.' : 'Pokemon jugado a banca.')
    );
  }

  private attachEnergyToPokemon(energyCardId: string, pokemonInstanceId: string): void {
    if (!this.selectedGame || !this.currentPlayer || !energyCardId || !pokemonInstanceId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.attachEnergy(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        energyCardId,
        pokemonInstanceId
      }),
      'Energia unida.'
    );
  }

  private evolvePokemonTo(evolutionCardId: string, targetPokemon: PokemonInPlayResponse): void {
    if (!this.selectedGame || !this.currentPlayer || !evolutionCardId || !targetPokemon.instanceId) {
      return;
    }
    const fromName = this.cardName(targetPokemon.cardId);
    const toName = this.cardName(evolutionCardId);
    this.runGameMutation(
      this.gamesService.evolvePokemon(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        evolutionCardId,
        targetPokemonInstanceId: targetPokemon.instanceId
      }),
      `${fromName} evolucionó a ${toName}.`
    );
  }

  handTransform(index: number, total: number): string {
    const center = (total - 1) / 2;
    const offset = index - center;
    const rotation = Math.max(-22, Math.min(22, offset * 7));
    const lift = Math.abs(offset) * 4;
    return `translateY(${lift}px) rotate(${rotation}deg)`;
  }

  isBasicPokemon(cardId: string): boolean {
    const card = this.cardsById[cardId];
    return card?.supertype === 'Pokémon' && (card.subtypes ?? []).includes('Basic');
  }

  isAttachableEnergy(cardId: string): boolean {
    const card = this.cardsById[cardId];
    return card?.supertype === 'Energy' && (this.hasCardSubtype(card, 'Basic') || this.isSpecialAnyEnergy(card));
  }

  isEvolutionPokemon(cardId: string): boolean {
    const card = this.cardsById[cardId];
    return card?.supertype === 'Pokémon'
      && !!card.evolvesFrom
      && (this.hasCardSubtype(card, 'Stage 1') || this.hasCardSubtype(card, 'Stage 2') || this.hasCardSubtype(card, 'MEGA'));
  }

  isDraggableHandCard(cardId: string): boolean {
    return this.isBasicPokemon(cardId) || this.isAttachableEnergy(cardId) || this.isEvolutionPokemon(cardId);
  }

  private isEnergyCompatibleWithPokemon(energyCardId: string, pokemonCardId: string): boolean {
    const energyCard = this.cardsById[energyCardId];
    const pokemonCard = this.cardsById[pokemonCardId];
    if (!energyCard || !pokemonCard || this.isSpecialAnyEnergy(energyCard)) {
      return true;
    }

    const energyType = this.cardEnergyType(energyCard);
    const pokemonTypes = (pokemonCard.types ?? [])
      .map((type) => this.canonicalEnergyType(type))
      .filter(Boolean);
    if (!energyType || pokemonTypes.length === 0) {
      return true;
    }
    return pokemonTypes.includes(energyType);
  }

  private cardEnergyType(card: Card): string {
    for (const type of card.types ?? []) {
      const canonicalType = this.canonicalEnergyType(type);
      if (canonicalType) {
        return canonicalType;
      }
    }
    for (const subtype of card.subtypes ?? []) {
      const canonicalSubtype = this.canonicalEnergyType(subtype);
      if (canonicalSubtype) {
        return canonicalSubtype;
      }
    }

    const normalizedName = this.normalizeEnergyType(card.name);
    return this.supportedEnergyTypes.find((type) => {
      const normalizedType = this.normalizeEnergyType(type);
      return normalizedName.includes(`${normalizedType} ENERGY`) || normalizedName.includes(`${normalizedType} ENERGIA`);
    }) ?? '';
  }

  private canonicalEnergyType(value?: string): string {
    const normalizedValue = this.normalizeEnergyType(value);
    return this.supportedEnergyTypes.find((type) => this.normalizeEnergyType(type) === normalizedValue) ?? '';
  }

  private isSpecialAnyEnergy(card: Card): boolean {
    const normalizedName = this.normalizeEnergyType(card.name);
    return normalizedName === 'DOUBLE COLORLESS ENERGY' || normalizedName === 'RAINBOW ENERGY';
  }

  private normalizeEnergyType(value?: string): string {
    return value?.trim().toUpperCase() ?? '';
  }

  private maybeAutoDraw(game: GameResponse): void {
    if (game.status !== 'ACTIVE' || game.turnPhase !== 'DRAW' || !game.currentPlayerId) {
      return;
    }
    if (this.drawAnimation) {
      return;
    }

    const autoDrawKey = this.autoDrawKey(game);
    if (this.lastAutoDrawKey === autoDrawKey || this.autoDrawInFlightKey === autoDrawKey) {
      return;
    }

    this.lastAutoDrawKey = autoDrawKey;
    this.autoDrawInFlightKey = autoDrawKey;
    this.gamesService.drawCard(game.id, { playerId: game.currentPlayerId }).subscribe({
      next: (drawnGame) => {
        if (drawnGame.status === 'FINISHED') {
          if (this.autoDrawInFlightKey === autoDrawKey) {
            this.autoDrawInFlightKey = '';
          }
          this.message = this.mutationMessage(drawnGame, 'Carta robada automáticamente.');
          this.setSelectedGame(drawnGame);
          this.loadGames();
          return;
        }
        this.playDrawAnimation(autoDrawKey, drawnGame);
      },
      error: (error) => {
        if (this.autoDrawInFlightKey === autoDrawKey) {
          this.autoDrawInFlightKey = '';
        }
        this.error = this.backendError(error, 'No se pudo robar carta automáticamente.');
      }
    });
  }

  private autoDrawKey(game: GameResponse): string {
    const latestLog = game.logs[game.logs.length - 1];
    return [
      game.id,
      game.currentPlayerId ?? '-',
      game.turnPhase ?? '-',
      game.updatedAt ?? '-',
      latestLog?.id ?? game.logs.length
    ].join('-');
  }

  private playDrawAnimation(autoDrawKey: string, game: GameResponse): void {
    this.drawAnimation = { key: autoDrawKey };
    window.setTimeout(() => {
      this.drawAnimation = null;
      if (this.autoDrawInFlightKey === autoDrawKey) {
        this.autoDrawInFlightKey = '';
      }
      this.message = this.mutationMessage(game, 'Carta robada automáticamente.');
      this.setSelectedGame(game);
      this.loadGames();
    }, this.drawAnimationMs);
  }

  private syncAttackOverlay(game: GameResponse): void {
    if (!this.selectedAttackPokemon) {
      return;
    }
    const currentPlayer = game.players.find((player) => player.id === game.currentPlayerId);
    const activePokemon = currentPlayer?.activePokemon;
    if (game.status !== 'ACTIVE' || game.turnPhase !== 'MAIN' || activePokemon?.instanceId !== this.selectedAttackPokemon.instanceId) {
      this.closeAttackOverlay();
      return;
    }
    this.selectedAttackPokemon = activePokemon;
  }

  private runGameMutation(
    request: Observable<GameResponse>,
    successMessage: string,
    errorMessage = 'La accion no se pudo completar. Revisa fase, turno, mazos validos y cartas disponibles.'
  ): void {
    this.error = '';
    this.message = '';
    request.subscribe({
      next: (game) => {
        this.message = this.mutationMessage(game, successMessage);
        this.setSelectedGame(game);
        this.refreshAfterGameUpdate();
      },
      error: (error) => {
        this.error = this.backendError(error, errorMessage);
      }
    });
  }

  private refreshAfterGameUpdate(): void {
    if (!this.hasPendingVisualAnimation()) {
      this.refreshSelectedGame();
    }
    this.loadGames();
  }

  private hasPendingVisualAnimation(): boolean {
    return !!this.attackAnimation || !!this.drawAnimation || !!this.autoDrawInFlightKey;
  }

  private refreshSelectedGame(): void {
    if (!this.selectedGame) {
      return;
    }
    this.gamesService.getGame(this.selectedGame.id).subscribe({
      next: (game) => this.setSelectedGame(game),
      error: () => undefined
    });
  }

  private setSelectedGame(game: GameResponse): void {
    this.selectedGame = game;
    this.selectedBasicCardId = '';
    this.selectedEnergyCardId = '';
    this.selectedTargetPokemonInstanceId = '';
    this.selectedPromotionPokemonInstanceId = '';
    this.selectedAttackName = '';
    this.syncAttackOverlay(game);
    this.maybeAutoDraw(game);
  }

  private mutationMessage(game: GameResponse, successMessage: string): string {
    if (game.status === 'FINISHED') {
      return `Partida finalizada. Ganador: ${this.winnerName(game)}`;
    }
    const latestLog = game.logs[game.logs.length - 1];
    const latestLogShowsCombat = latestLog?.actionType === 'KNOCK_OUT' || latestLog?.actionType === 'TAKE_PRIZE';
    return latestLogShowsCombat ? `${successMessage} ${latestLog.message}` : successMessage;
  }

  private winnerName(game: GameResponse): string {
    if (!game.winnerPlayerId) {
      return '-';
    }
    return game.players.find((player) => player.id === game.winnerPlayerId)?.playerName ?? `Jugador ${game.winnerPlayerId}`;
  }

  private backendError(error: unknown, fallback: string): string {
    if (this.isRecord(error)) {
      const responseError = error['error'];
      if (typeof responseError === 'string' && responseError.trim()) {
        return responseError;
      }
      if (this.isRecord(responseError)) {
        const message = responseError['message'] ?? responseError['error'] ?? responseError['detail'];
        if (typeof message === 'string' && message.trim()) {
          return message;
        }
      }
      const message = error['message'];
      if (typeof message === 'string' && message.trim()) {
        return message;
      }
    }
    return fallback;
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }
}
