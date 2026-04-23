import { Node, mergeAttributes } from '@tiptap/core';

/** Block node wrapping content that renders only when {@code when} expression evaluates to true. */
export const ConditionBlock = Node.create({
  name: 'conditionBlock',
  group: 'block',
  content: 'block+',
  defining: true,

  addAttributes() {
    return { when: { default: '' } };
  },

  parseHTML() {
    return [{ tag: 'div[data-type="conditionBlock"]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'conditionBlock', class: 'tm-condition' }),
      0,
    ];
  },
});
