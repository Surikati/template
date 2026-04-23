export type AssemblyState = 'PENDING' | 'RESOLVING_CLAUSES' | 'RENDERING' | 'COMPLETED' | 'FAILED';

export interface AssembleRequest {
  templateId: string;
  templateVersionNumber: number;
  data: Record<string, unknown>;
}

export interface AssembleResponse {
  jobId: string;
  state: AssemblyState;
  documentId?: string;
  filename?: string;
  /** Relative path exposed by document-service (e.g. `/api/v1/documents/<id>/files/DOCX`). */
  downloadUrl?: string;
  completedAt?: string;
}
