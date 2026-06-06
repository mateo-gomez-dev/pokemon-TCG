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
  attacks?: CardAttack[];
  weaknesses?: unknown;
  resistances?: unknown;
  imageUrl?: string;
  imageSmall?: string;
  imageSmallUrl?: string;
  imageLargeUrl?: string;
  images?: {
    small?: string;
    large?: string;
  };
  setId?: string;
  setName?: string;
}
