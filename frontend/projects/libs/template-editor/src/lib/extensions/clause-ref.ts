import { Node, mergeAttributes } from '@tiptap/core';

/** Reference to a versioned clause from the clause library. Resolved at render time. */
export const ClauseRef = Node.create({
  name: 'clauseRef',
  group: 'block',
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      clauseId: { default: '' },
      versionNumber: { default: 1 },
    };
  },

  parseHTML() {
    return [{ tag: 'div[data-type="clauseRef"]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, { 'data-type': 'clauseRef', class: 'tm-clause-ref' }),
      `⟦ clause: ${HTMLAttributes['clauseId']} v${HTMLAttributes['versionNumber']} ⟧`,
    ];
  },
});
