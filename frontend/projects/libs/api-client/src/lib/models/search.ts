export type SearchHitType = 'template' | 'clause';

export interface SearchHit {
  id: string;
  type: SearchHitType;
  slug?: string;
  name?: string;
  description?: string;
  category?: string;
  status?: string;
  updatedAt?: string;
  score: number;
}
