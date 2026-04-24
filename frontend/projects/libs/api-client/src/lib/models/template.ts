export type TemplateStatus = 'ACTIVE' | 'ARCHIVED';

export interface TemplateResponse {
  id: string;
  slug: string;
  name: string;
  description?: string;
  category?: string;
  tags: string[];
  status: TemplateStatus;
  ownerUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateDraftResponse {
  templateId: string;
  content: unknown;
  variablesSchema: unknown;
  lastEditedBy: string;
  lastEditedAt: string;
}

export interface TemplateVersionResponse {
  id: string;
  templateId: string;
  versionNumber: number;
  content: unknown;
  variablesSchema: unknown;
  changeNote?: string;
  publishedAt: string;
  publishedBy: string;
}

export interface CreateTemplateRequest {
  slug: string;
  name: string;
  description?: string;
  category?: string;
}

export interface UpdateMetadataRequest {
  name: string;
  description?: string;
  category?: string;
  tags?: string[];
}

export interface UpdateDraftRequest {
  content: unknown;
  variablesSchema: unknown;
}

export interface PublishVersionRequest {
  changeNote?: string;
}
