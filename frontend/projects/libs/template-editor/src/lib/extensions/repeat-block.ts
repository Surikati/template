import { Node, mergeAttributes } from '@tiptap/core';

/** Block node that iterates over a list expression, binding each item to a loop variable. */
export const RepeatBlock = Node.create({
  name: 'repeatBlock',
  group: 'block',
  content: 'block+',
  defining: true,

  addAttributes() {
    return {
      each: { default: 'item' },
      in: { default: '' },
    };
  },

  parseHTML() {
    return [{ tag: 'div[data-type="repeatBlock"]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'repeatBlock', class: 'tm-repeat' }),
      0,
    ];
  },
});
