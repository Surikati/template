import { VariableNode } from './variable-node';

describe('VariableNode extension', () => {
  it('declares the canonical name and inline atomic node config', () => {
    expect(VariableNode.name).toBe('variable');
    expect(VariableNode.config.group).toBe('inline');
    expect(VariableNode.config.inline).toBeTrue();
    expect(VariableNode.config.atom).toBeTrue();
    expect(VariableNode.config.selectable).toBeTrue();
  });

  it('addAttributes() returns the documented defaults (path, dataType, format)', () => {
    const addAttributes = VariableNode.config.addAttributes as unknown as () => Record<
      string,
      { default: unknown }
    >;
    const attrs = addAttributes.call({});
    expect(attrs['path'].default).toBe('');
    expect(attrs['dataType'].default).toBe('STRING');
    expect(attrs['format'].default).toBeNull();
  });

  it('parseHTML() matches span[data-type="variable"]', () => {
    const parseHTML = VariableNode.config.parseHTML as unknown as () => Array<{ tag: string }>;
    const rules = parseHTML.call({});
    expect(rules[0].tag).toBe('span[data-type="variable"]');
  });

  it('renderHTML() emits a styled span with {{ path }} as text content', () => {
    const renderHTML = VariableNode.config.renderHTML as unknown as (args: {
      HTMLAttributes: Record<string, unknown>;
    }) => unknown[];
    const result = renderHTML.call({}, { HTMLAttributes: { path: 'customer.name' } });
    expect(result[0]).toBe('span');
    const attrs = result[1] as Record<string, string>;
    expect(attrs['data-type']).toBe('variable');
    expect(attrs['class']).toBe('tm-variable');
    expect(result[2]).toBe('{{ customer.name }}');
  });
});
