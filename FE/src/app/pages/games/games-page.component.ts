import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { Card, CardAttack } from '../../models/card.model';
import { DeckResponse } from '../../models/deck.model';
import { GamePlayerResponse, GameResponse } from '../../models/game.model';
import { CardsService } from '../../services/cards.service';
import { DecksService } from '../../services/decks.service';
import { GamesService } from '../../services/games.service';

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
              <button type="button" (click)="startGame()" [disabled]="selectedGame.status !== 'WAITING' || selectedGame.players.length !== 2">
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
                <ng-container *ngIf="opponent.benchCardIds[slot] as cardId; else emptyOpponentBench">
                  <article class="tcg-card bench-card" [class.glow]="slot === 0">
                    <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: opponent, compact: true }"></ng-container>
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
                  <ng-container *ngIf="visualActiveCardId(opponent) as cardId; else noOpponentActive">
                    <article class="tcg-card active-card" [class.glow]="opponent.id === selectedGame.currentPlayerId">
                      <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: opponent, compact: false }"></ng-container>
                    </article>
                  </ng-container>
                  <ng-template #noOpponentActive><div class="empty-active">Activo rival</div></ng-template>
                </div>

                <div class="center-field">
                  <div class="pokeball-mark"></div>
                  <strong>{{ selectedGame.turnPhase === 'DRAW' ? 'Robar carta' : 'Accion principal' }}</strong>
                  <span>{{ turnText }}</span>
                </div>

                <div class="active-slot player-active" *ngIf="boardPlayer as player">
                  <ng-container *ngIf="visualActiveCardId(player) as cardId; else noPlayerActive">
                    <article class="tcg-card active-card" [class.glow]="player.id === selectedGame.currentPlayerId">
                      <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: player, compact: false }"></ng-container>
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
              <div class="slot bench-slot" *ngFor="let slot of benchSlots; trackBy: trackByIndex">
                <ng-container *ngIf="player.benchCardIds[slot] as cardId; else emptyPlayerBench">
                  <article class="tcg-card bench-card" [class.glow]="slot === 0">
                    <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: player, compact: true }"></ng-container>
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
                [class.selectable]="isBasicPokemon(cardId) || isBasicEnergy(cardId)"
              >
                <ng-container *ngTemplateOutlet="cardFace; context: { cardId: cardId, player: player, compact: true }"></ng-container>
              </article>
            </section>
          </section>

          <aside class="floating-actions" *ngIf="currentPlayer">
            <strong>Acciones</strong>
            <div class="action-debug" *ngIf="showDebugInfo">
              <span>Estado: {{ selectedGame.status }}</span>
              <span>Fase: {{ selectedGame.turnPhase }}</span>
              <span>CurrentPlayerId: {{ selectedGame.currentPlayerId }}</span>
              <span>Jugador actual: {{ currentPlayer.playerName }}</span>
            </div>
            <button type="button" (click)="drawCard()" [disabled]="!canDrawCard()">
              Robar carta
            </button>

            <label>
              Pokemon Basico
              <select [(ngModel)]="selectedBasicCardId">
                <option value="">Seleccionar</option>
                <option *ngFor="let cardId of basicCardsInHand; trackBy: trackByIndexedCardId" [value]="cardId">{{ cardLabel(cardId) }}</option>
              </select>
            </label>
            <button type="button" (click)="playBasicPokemon()" [disabled]="selectedGame.status !== 'ACTIVE' || selectedGame.turnPhase !== 'MAIN' || !selectedBasicCardId">
              Jugar basico
            </button>

            <label>
              Energia
              <select [(ngModel)]="selectedEnergyCardId">
                <option value="">Seleccionar</option>
                <option *ngFor="let cardId of energyCardsInHand; trackBy: trackByIndexedCardId" [value]="cardId">{{ cardLabel(cardId) }}</option>
              </select>
            </label>
            <label>
              Objetivo
              <select [(ngModel)]="selectedTargetPokemonCardId">
                <option value="">Seleccionar</option>
                <option *ngFor="let cardId of currentPlayer.benchCardIds; trackBy: trackByIndexedCardId" [value]="cardId">{{ cardLabel(cardId) }}</option>
              </select>
            </label>
            <button type="button" (click)="attachEnergy()" [disabled]="selectedGame.status !== 'ACTIVE' || selectedGame.turnPhase !== 'MAIN' || !selectedEnergyCardId || !selectedTargetPokemonCardId">
              Unir energia
            </button>
            <label>
              Ataque
              <select [(ngModel)]="selectedAttackName">
                <option value="">Seleccionar</option>
                <option *ngFor="let attack of activePokemonAttacks; trackBy: trackByAttackName" [value]="attack.name">
                  {{ attackLabel(attack) }}
                </option>
              </select>
            </label>
            <div class="action-debug attack-debug" *ngIf="showDebugInfo">
              <span>Ataque seleccionado: {{ selectedAttackName || '-' }}</span>
              <span>Ataques disponibles: {{ activePokemonAttacks.length }}</span>
              <span>Activo actual: {{ activePokemonCardId || '-' }}</span>
              <span>Activo rival: {{ opponentActivePokemonCardId || '-' }}</span>
              <span>Puede atacar: {{ canAttack() ? 'si' : 'no' }}</span>
            </div>
            <button type="button" class="attack-button" (click)="attack()" [disabled]="!canAttack()">
              Atacar
            </button>
            <button type="button" class="end-turn" (click)="endTurn()" [disabled]="selectedGame.status !== 'ACTIVE'">
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
                  <p>Mano IDs: {{ player.handCardIds.join(', ') || '-' }}</p>
                  <p>Banca IDs: {{ player.benchCardIds.join(', ') || '-' }}</p>
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

      <ng-template #cardFace let-cardId="cardId" let-player="player" let-compact="compact">
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
          <span *ngIf="cardHp(cardId)">HP {{ remainingHp(player, cardId) }}/{{ cardHp(cardId) }}</span>
          <small>Energia: {{ attachedEnergyCount(player, cardId) }}</small>
        </div>
        <span class="energy-badge" *ngIf="attachedEnergyCount(player, cardId) > 0">{{ attachedEnergyCount(player, cardId) }}</span>
        <span class="damage-badge" *ngIf="damageFor(player, cardId) > 0">{{ damageFor(player, cardId) }} dmg</span>
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
      grid-template-columns: minmax(0, 1fr) 230px;
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
      align-self: start;
      background: rgba(255, 255, 255, 0.86);
      border: 1px solid rgba(186, 230, 253, 0.95);
      border-radius: 28px;
      box-shadow: 0 24px 55px rgba(15, 23, 42, 0.16);
      padding: 1rem;
      position: sticky;
      top: 92px;
    }

    .floating-actions strong {
      color: #075985;
      font-size: 1.1rem;
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

    .floating-actions .attack-button {
      background: linear-gradient(135deg, #f97316, #ef4444);
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

    @media (max-width: 1180px) {
      .game-shell,
      .board-wrap {
        grid-template-columns: 1fr;
      }

      .control-rail,
      .floating-actions {
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
    }
  `]
})
export class GamesPageComponent implements OnInit {
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
  selectedTargetPokemonCardId = '';
  selectedAttackName = '';
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
    if (this.selectedGame.status === 'WAITING') {
      return 'Esperando jugadores';
    }
    const playerName = this.currentPlayer?.playerName ?? `Jugador ${this.selectedGame.currentPlayerId ?? '-'}`;
    return `Turno de ${playerName}`;
  }

  get basicCardsInHand(): string[] {
    return this.currentPlayer?.handCardIds.filter((cardId) => this.isBasicPokemon(cardId)) ?? [];
  }

  get energyCardsInHand(): string[] {
    return this.currentPlayer?.handCardIds.filter((cardId) => this.isBasicEnergy(cardId)) ?? [];
  }

  get activePokemonAttacks(): CardAttack[] {
    const activeCardId = this.activePokemonCardId;
    if (!activeCardId) {
      return [];
    }
    return this.cardsById[activeCardId]?.attacks ?? [];
  }

  get activePokemonCardId(): string {
    return this.currentPlayer ? this.visualActiveCardId(this.currentPlayer) : '';
  }

  get opponentActivePokemonCardId(): string {
    return this.opponentPlayer ? this.visualActiveCardId(this.opponentPlayer) : '';
  }

  canDrawCard(): boolean {
    return !!this.selectedGame
      && this.selectedGame.status === 'ACTIVE'
      && this.selectedGame.turnPhase === 'DRAW'
      && !!this.selectedGame.currentPlayerId;
  }

  canAttack(): boolean {
    const selectedGame = this.selectedGame;
    const currentPlayer = this.currentPlayer;
    const opponentPlayer = this.opponentPlayer;

    return !!selectedGame
      && selectedGame.status === 'ACTIVE'
      && selectedGame.turnPhase === 'MAIN'
      && !!selectedGame.currentPlayerId
      && !!currentPlayer
      && !!opponentPlayer
      && !!this.visualActiveCardId(currentPlayer)
      && !!this.visualActiveCardId(opponentPlayer)
      && !!this.selectedAttackName;
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
        if (this.selectedGame) {
          this.selectedGame = games.find((game) => game.id === this.selectedGame?.id) ?? this.selectedGame;
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
    this.runGameMutation(
      this.gamesService.playBasicPokemon(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        cardId: this.selectedBasicCardId
      }),
      'Pokemon jugado a banca.'
    );
  }

  attachEnergy(): void {
    if (!this.selectedGame || !this.currentPlayer || !this.selectedEnergyCardId || !this.selectedTargetPokemonCardId) {
      return;
    }
    this.runGameMutation(
      this.gamesService.attachEnergy(this.selectedGame.id, {
        playerId: this.currentPlayer.id,
        energyCardId: this.selectedEnergyCardId,
        targetPokemonCardId: this.selectedTargetPokemonCardId
      }),
      'Energia unida.'
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
    if (!this.selectedGame?.currentPlayerId || !this.selectedAttackName) {
      return;
    }
    this.runGameMutation(
      this.gamesService.attack(this.selectedGame.id, {
        playerId: this.selectedGame.currentPlayerId,
        attackName: this.selectedAttackName
      }),
      'Ataque realizado.',
      'No se pudo atacar.'
    );
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
    const card = this.cardsById[cardId];
    return [card?.supertype, ...(card?.subtypes ?? [])].filter(Boolean).join(' / ') || 'Carta';
  }

  cardHp(cardId: string): number {
    return this.cardsById[cardId]?.hp ?? 0;
  }

  damageFor(player: GamePlayerResponse, pokemonCardId: string): number {
    return player.damageByPokemonCardId?.[pokemonCardId] ?? 0;
  }

  remainingHp(player: GamePlayerResponse, pokemonCardId: string): number {
    const hp = this.cardHp(pokemonCardId);
    return Math.max(0, hp - this.damageFor(player, pokemonCardId));
  }

  attackLabel(attack: CardAttack): string {
    const energyCost = attack.convertedEnergyCost ?? attack.cost?.length ?? 0;
    const damage = attack.damage ? ` - ${attack.damage} dano` : '';
    return `${attack.name} (${energyCost} energia)${damage}`;
  }

  cardImageUrl(cardId: string): string {
    const card = this.cardsById[cardId];
    return card?.imageSmallUrl ?? card?.imageSmall ?? card?.imageUrl ?? card?.images?.small ?? card?.imageLargeUrl ?? card?.images?.large ?? '';
  }

  attachedEnergyCount(player: GamePlayerResponse, pokemonCardId: string): number {
    return player.attachedEnergyCardIdsByPokemonCardId[pokemonCardId]?.length ?? 0;
  }

  visualActiveCardId(player: GamePlayerResponse): string {
    return player.benchCardIds[0] ?? '';
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

  isBasicEnergy(cardId: string): boolean {
    const card = this.cardsById[cardId];
    return card?.supertype === 'Energy' && (card.subtypes ?? []).includes('Basic');
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
        this.message = successMessage;
        this.setSelectedGame(game);
        this.refreshSelectedGame();
        this.loadGames();
      },
      error: (error) => {
        this.error = this.backendError(error, errorMessage);
      }
    });
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
    this.selectedTargetPokemonCardId = '';
    this.selectedAttackName = '';
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
