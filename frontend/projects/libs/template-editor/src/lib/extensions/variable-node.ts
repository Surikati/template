import { Node, mergeAttributes } from '@tiptap/core';

/**
 * Inline node representing a template variable placeholder.
 * Renders like a chip in the editor; serializes to { type: 'variable', attrs: { path, dataType, format } }.
 */
export const VariableNode = Node.create({
  name: 'variable',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      path: { default: '' },
      dataType: { default: 'STRING' },
      format: { default: null },
    };
  },

  parseHTML() {
    return [{ tag: 'span[data-type="variable"]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'span',
      mergeAttributes(HTMLAttributes, { 'data-type': 'variable', class: 'tm-variable' }),
      `{{ ${HTMLAttributes['path']} }}`,
    ];
  },
});
