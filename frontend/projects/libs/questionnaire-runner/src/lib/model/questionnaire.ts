export type QuestionType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'BOOLEAN'
  | 'SELECT'
  | 'MULTISELECT'
  | 'GROUP';

export interface Question {
  id: string;
  variablePath: string;
  label: string;
  questionType: QuestionType;
  validation?: Record<string, unknown>;
  visibilityRule?: string;
  options?: Array<{ value: string; label: string }>;
}

export interface Section {
  id: string;
  title: string;
  ordinal: number;
  visibilityRule?: string;
  questions: Question[];
}

export interface Questionnaire {
  id: string;
  templateId: string;
  templateVersionNumber: number;
  name: string;
  sections: Section[];
}
