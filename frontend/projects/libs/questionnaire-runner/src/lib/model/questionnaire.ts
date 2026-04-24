export type QuestionType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'BOOLEAN'
  | 'SELECT'
  | 'MULTISELECT'
  | 'GROUP';

export interface QuestionOption {
  value: string;
  label: string;
}

export interface Question {
  id: string;
  ordinal: number;
  variablePath: string;
  label: string;
  questionType: QuestionType;
  validation?: Record<string, unknown> | null;
  visibilityRule?: string | null;
  options?: QuestionOption[] | null;
}

export interface Section {
  id: string;
  ordinal: number;
  title: string;
  visibilityRule?: string | null;
  questions: Question[];
}

export interface Questionnaire {
  id: string;
  templateId: string;
  templateVersionNumber: number;
  name: string;
  sections: Section[];
}
