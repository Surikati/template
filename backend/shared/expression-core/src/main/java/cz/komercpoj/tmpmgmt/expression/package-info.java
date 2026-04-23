/**
 * Restricted expression language used by template conditions and visibility rules.
 *
 * <p>Grammar: {@code src/main/antlr4/cz/komercpoj/tmpmgmt/expression/TmpExpr.g4}. The ANTLR Maven
 * plugin generates parser/lexer into {@code target/generated-sources/antlr4}. Implementations of
 * {@link cz.komercpoj.tmpmgmt.expression.ExpressionEvaluator} should extend {@code
 * TmpExprBaseVisitor} and build a small tree-walking interpreter.
 */
package cz.komercpoj.tmpmgmt.expression;
