export interface CardAttack {
  name: string;
  cost?: string[];
  convertedEnergyCost?: number;
  damage?: string;
  text?: string;
}

export interface Card {
  id: string;
  name: string;
  supertype?: string;
  subtypes?: string[];
  hp?: number;
  types?: string[];
  evolvesFrom?: string;
  rules?: string[];
  abilities?: unknown;
  attacks?: CardAttack[];
  weaknesses?: unknown;
  resistances?: unknown;
  retreatCost?: string[];
  convertedRetreatCost?: number;
  number?: string;
  rarity?: string;
  flavorText?: string;
  text?: string;
  imageUrl?: string;
  imageSmall?: string;
  imageLarge?: string;
  imageSmallUrl?: string;
  imageLargeUrl?: string;
  images?: {
    small?: string;
    large?: string;
  };
  rawJson?: Record<string, unknown>;
  setId?: string;
  setName?: string;
}
