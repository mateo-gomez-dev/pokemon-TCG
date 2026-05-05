export interface Card {
  id: string;
  name: string;
  supertype?: string;
  subtypes?: string[];
  hp?: number;
  imageSmallUrl?: string;
  imageLargeUrl?: string;
  setId?: string;
  setName?: string;
}
