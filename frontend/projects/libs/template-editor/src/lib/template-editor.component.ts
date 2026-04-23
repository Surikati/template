import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  output,
  viewChild,
  input,
} from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import { VariableNode } from './extensions/variable-node';
import { ConditionBlock } from './extensions/condition-block';
import { RepeatBlock } from './extensions/repeat-block';
import { ClauseRef } from './extensions/clause-ref';
import { TemplateDocument } from './model/template-ast';

@Component({
  selector: 'tme-template-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #host class="tme-host"></div>`,
  styles: [
    `.tme-host { min-height: 400px; border: 1px solid #e4e4e7; padding: 1rem; }
     .tm-variable { background: #eef2ff; padding: 0 0.25rem; border-radius: 3px; }
     .tm-condition, .tm-repeat { border-left: 3px solid #a78bfa; padding-left: 0.75rem; }
     .tm-clause-ref { font-style: italic; color: #6b7280; }`,
  ],
})
export class TemplateEditorComponent implements AfterViewInit, OnDestroy {
  readonly initialContent = input<TemplateDocument | null>(null);
  readonly contentChanged = output<TemplateDocument>();

  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  private editor?: Editor;

  ngAfterViewInit(): void {
    this.editor = new Editor({
      element: this.host().nativeElement,
      extensions: [StarterKit, VariableNode, ConditionBlock, RepeatBlock, ClauseRef],
      content: this.initialContent() ?? { type: 'doc', content: [{ type: 'paragraph' }] },
      onUpdate: ({ editor }) => {
        this.contentChanged.emit(editor.getJSON() as TemplateDocument);
      },
    });
  }

  ngOnDestroy(): void {
    this.editor?.destroy();
  }
}
