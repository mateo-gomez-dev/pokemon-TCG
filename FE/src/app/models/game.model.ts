export interface CreateGameRequest {
  playerName: string;
  deckId: number;
}

export type JoinGameRequest = CreateGameRequest;

export interface GameActionRequest {
  playerId: number;
}

export interface PlayBasicPokemonRequest extends GameActionRequest {
  cardId: string;
}

export interface AttachEnergyRequest extends GameActionRequest {
  energyCardId: string;
  targetPokemonCardId: string;
}

export interface AttackRequest extends GameActionRequest {
  attackName: string;
}

export interface GameResponse {
  id: number;
  status: 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED' | string;
  turnPhase: 'DRAW' | 'MAIN' | string;
  currentPlayerId?: number;
  createdAt: string;
  updatedAt: string;
  startedAt?: string;
  finishedAt?: string;
  players: GamePlayerResponse[];
  logs: GameLogResponse[];
}

export interface GamePlayerResponse {
  id: number;
  playerName: string;
  playerOrder: number;
  deckId: number;
  deckName: string;
  deckRemaining: number;
  handSize: number;
  prizeCardsRemaining: number;
  benchSize: number;
  discardSize: number;
  energyAttachedThisTurn: boolean;
  deckCardIds: string[];
  handCardIds: string[];
  prizeCardIds: string[];
  benchCardIds: string[];
  attachedEnergyCardIdsByPokemonCardId: Record<string, string[]>;
  damageByPokemonCardId: Record<string, number>;
  discardCardIds: string[];
}

export interface GameLogResponse {
  id: number;
  playerId?: number;
  actionType: string;
  message: string;
  createdAt: string;
}
