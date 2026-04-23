/**
 * Canonical shape of the template AST stored in the backend.
 * Mirrors the ProseMirror/TipTap JSON format so editor state is lossless.
 */
export interface TemplateNode {
  type: string;
  attrs?: Record<string, unknown>;
  content?: TemplateNode[];
  text?: string;
  marks?: Array<{ type: string; attrs?: Record<string, unknown> }>;
}

export interface TemplateDocument {
  type: 'doc';
  content: TemplateNode[];
}

export type VariableDataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'ENUM' | 'OBJECT' | 'LIST';

export interface VariableAttrs {
  path: string;
  dataType: VariableDataType;
  format?: string;
}

export interface ConditionBlockAttrs {
  when: string;
}

export interface RepeatBlockAttrs {
  each: string;
  in: string;
}

export interface ClauseRefAttrs {
  clauseId: string;
  versionNumber: number;
}
