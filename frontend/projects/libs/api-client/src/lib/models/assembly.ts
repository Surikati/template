export type AssemblyState = 'PENDING' | 'RESOLVING_CLAUSES' | 'RENDERING' | 'COMPLETED' | 'FAILED';

export type OutputFormat = 'DOCX' | 'PDF';

export interface AssembleRequest {
  templateId: string;
  templateVersionNumber: number;
  data: Record<string, unknown>;
  /** Server defaults to [DOCX] when omitted. Pass multiple to render the same template into
   *  several formats in one job (clauses + AST resolution are shared across formats). */
  formats?: OutputFormat[];
}

export interface AssembledFile {
  format: OutputFormat;
  filename: string;
  /** Relative path exposed by document-service (e.g. `/api/v1/documents/<id>/files/DOCX`). */
  downloadUrl?: string;
}

export interface AssembleResponse {
  jobId: string;
  state: AssemblyState;
  documentId?: string;
  files: AssembledFile[];
  completedAt?: string;
}
