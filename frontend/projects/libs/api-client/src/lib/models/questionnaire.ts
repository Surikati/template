export type QuestionType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'BOOLEAN'
  | 'SELECT'
  | 'MULTISELECT'
  | 'GROUP';

export type SessionState = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

export interface QuestionOption {
  value: string;
  label: string;
}

export interface QuestionResponse {
  id: string;
  ordinal: number;
  variablePath: string;
  label: string;
  questionType: QuestionType;
  validation?: Record<string, unknown> | null;
  visibilityRule?: string | null;
  options?: QuestionOption[] | null;
}

export interface SectionResponse {
  id: string;
  ordinal: number;
  title: string;
  visibilityRule?: string | null;
  questions: QuestionResponse[];
}

export interface QuestionnaireResponse {
  id: string;
  templateId: string;
  templateVersionNumber: number;
  name: string;
  sections: SectionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface QuestionInputDto {
  ordinal: number;
  variablePath: string;
  label: string;
  questionType: QuestionType;
  validation?: Record<string, unknown> | null;
  visibilityRule?: string | null;
  options?: QuestionOption[] | null;
}

export interface SectionInputDto {
  ordinal: number;
  title: string;
  visibilityRule?: string | null;
  questions: QuestionInputDto[];
}

export interface CreateQuestionnaireRequest {
  templateId: string;
  templateVersionNumber: number;
  name: string;
  sections: SectionInputDto[];
}

export interface ReplaceStructureRequest {
  name: string;
  sections: SectionInputDto[];
}

export interface SessionResponse {
  id: string;
  questionnaireId: string;
  state: SessionState;
  startedBy: string;
  startedAt: string;
  completedAt?: string | null;
  answers: Record<string, unknown>;
  currentSectionId?: string | null;
}

export interface StartSessionRequest {
  questionnaireId: string;
}

export interface SubmitAnswersRequest {
  answers: Record<string, unknown>;
}

export interface RuleInput {
  key: string;
  expression: string;
}

export interface EvaluateRulesRequest {
  rules: RuleInput[];
  context: Record<string, unknown>;
}

export interface RuleResult {
  value: boolean;
  error?: string | null;
}

export interface EvaluateRulesResponse {
  results: Record<string, RuleResult>;
}
