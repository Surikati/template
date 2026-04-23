export type ClauseStatus = 'ACTIVE' | 'ARCHIVED';

export interface ClauseResponse {
  id: string;
  slug: string;
  name: string;
  description?: string;
  category?: string;
  tags: string[];
  status: ClauseStatus;
  ownerUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClauseVersionResponse {
  id: string;
  clauseId: string;
  versionNumber: number;
  /** Always rooted as `{ type: 'fragment', content: [...] }`. */
  content: unknown;
  changeNote?: string;
  publishedAt: string;
  publishedBy: string;
}

export interface CreateClauseRequest {
  slug: string;
  name: string;
  description?: string;
  category?: string;
}

export interface PublishClauseVersionRequest {
  content: unknown;
  changeNote?: string;
}
