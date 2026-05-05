export interface DeckCardRequest {
  cardId: string;
  quantity: number;
}

export interface DeckRequest {
  name: string;
  cards: DeckCardRequest[];
}

export interface DeckCardResponse {
  cardId: string;
  name: string;
  supertype?: string;
  subtypes?: string[];
  quantity: number;
}

export interface DeckResponse {
  id: number;
  name: string;
  valid: boolean;
  totalCards: number;
  createdAt: string;
  updatedAt: string;
  cards: DeckCardResponse[];
}

export interface DeckValidationResponse {
  valid: boolean;
  totalCards: number;
  errors: string[];
}
