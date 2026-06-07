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
  targetZone?: 'ACTIVE' | 'BENCH' | string;
}

export interface AttachEnergyRequest extends GameActionRequest {
  energyCardId: string;
  targetPokemonCardId?: string;
  pokemonInstanceId?: string;
}

export interface PromoteActiveRequest extends GameActionRequest {
  pokemonInstanceId: string;
}

export interface AttackRequest extends GameActionRequest {
  attackName?: string;
  attackIndex?: number;
}

export interface GameResponse {
  id: number;
  status: 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED' | string;
  turnPhase: 'DRAW' | 'MAIN' | string;
  currentPlayerId?: number;
  winnerPlayerId?: number;
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
  activePokemonInstanceId?: string;
  activePokemonCardId?: string;
  activePokemon?: PokemonInPlayResponse;
  benchPokemon: PokemonInPlayResponse[];
  deckCardIds: string[];
  handCardIds: string[];
  prizeCardIds: string[];
  benchCardIds: string[];
  attachedEnergyCardIdsByPokemonInstanceId: Record<string, string[]>;
  attachedEnergyCardIdsByPokemonCardId: Record<string, string[]>;
  damageByPokemonInstanceId: Record<string, number>;
  damageByPokemonCardId: Record<string, number>;
  discardCardIds: string[];
}

export interface PokemonInPlayResponse {
  instanceId: string;
  cardId: string;
  name: string;
  hp?: number;
  damage: number;
  remainingHp: number;
  attachedEnergyCardIds: string[];
  attachedEnergyCount: number;
}

export interface GameLogResponse {
  id: number;
  playerId?: number;
  actionType: string;
  message: string;
  createdAt: string;
}
