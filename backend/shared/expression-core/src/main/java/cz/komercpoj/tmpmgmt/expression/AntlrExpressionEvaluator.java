package cz.komercpoj.tmpmgmt.expression;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Production implementation backed by the ANTLR-generated {@code TmpExprParser}.
 *
 * <p>Supports: literals, path access with dot notation, comparisons, boolean operators, arithmetic,
 * and a small whitelist of pure functions ({@code len}, {@code empty}, {@code today},
 * {@code formatDate}). No reflection, no method calls on user data — this is a safe evaluator for
 * untrusted template authors.
 */
public class AntlrExpressionEvaluator implements ExpressionEvaluator {

    @Override
    public Object evaluate(String expression, Map<String, Object> data) {
        TmpExprParser parser = parse(expression);
        return new EvaluatingVisitor(data).visit(parser.expression());
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> data) {
        Object v = evaluate(expression, data);
        return Truthy.asBoolean(v);
    }

    @Override
    public void validate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ExpressionException("expression.empty", "Expression must not be empty.");
        }
        // Force a full parse so syntax errors surface at validation time.
        parse(expression).expression();
    }

    private TmpExprParser parse(String expression) {
        ThrowingErrorListener listener = new ThrowingErrorListener();

        TmpExprLexer lexer = new TmpExprLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        TmpExprParser parser = new TmpExprParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        return parser;
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e) {
            throw new ExpressionException(
                    "expression.syntax_error",
                    "Syntax error at " + line + ":" + charPositionInLine + " — " + msg);
        }
    }

    private static final class EvaluatingVisitor extends TmpExprBaseVisitor<Object> {

        private final Map<String, Object> data;

        EvaluatingVisitor(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Object visitExpression(TmpExprParser.ExpressionContext ctx) {
            return visit(ctx.orExpr());
        }

        @Override
        public Object visitProgram(TmpExprParser.ProgramContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Object visitOrExpr(TmpExprParser.OrExprContext ctx) {
            // Pass through single-operand unchanged to preserve non-boolean types.
            if (ctx.andExpr().size() == 1) return visit(ctx.andExpr(0));
            for (var operand : ctx.andExpr()) {
                if (Truthy.asBoolean(visit(operand))) return true;
            }
            return false;
        }

        @Override
        public Object visitAndExpr(TmpExprParser.AndExprContext ctx) {
            if (ctx.notExpr().size() == 1) return visit(ctx.notExpr(0));
            for (var operand : ctx.notExpr()) {
                if (!Truthy.asBoolean(visit(operand))) return false;
            }
            return true;
        }

        @Override
        public Object visitNotExpr(TmpExprParser.NotExprContext ctx) {
            if (ctx.notExpr() != null) {
                return !Truthy.asBoolean(visit(ctx.notExpr()));
            }
            return visit(ctx.comparison());
        }

        @Override
        public Object visitComparison(TmpExprParser.ComparisonContext ctx) {
            Object left = visit(ctx.additive(0));
            if (ctx.op == null) return left;
            Object right = visit(ctx.additive(1));
            return switch (ctx.op.getText()) {
                case "==" -> eq(left, right);
                case "!=" -> !eq(left, right);
                case "<"  -> cmp(left, right) < 0;
                case ">"  -> cmp(left, right) > 0;
                case "<=" -> cmp(left, right) <= 0;
                case ">=" -> cmp(left, right) >= 0;
                default -> throw new ExpressionException(
                        "expression.unknown_op", "Unknown operator: " + ctx.op.getText());
            };
        }

        @Override
        public Object visitAdditive(TmpExprParser.AdditiveContext ctx) {
            Object acc = visit(ctx.multiplicative(0));
            for (int i = 1; i < ctx.multiplicative().size(); i++) {
                String op = ctx.ops.get(i - 1).getText();
                Object right = visit(ctx.multiplicative(i));
                acc = op.equals("+") ? add(acc, right) : arith(acc, right, "-");
            }
            return acc;
        }

        @Override
        public Object visitMultiplicative(TmpExprParser.MultiplicativeContext ctx) {
            Object acc = visit(ctx.unary(0));
            for (int i = 1; i < ctx.unary().size(); i++) {
                String op = ctx.ops.get(i - 1).getText();
                Object right = visit(ctx.unary(i));
                acc = arith(acc, right, op);
            }
            return acc;
        }

        @Override
        public Object visitUnary(TmpExprParser.UnaryContext ctx) {
            if (ctx.unary() != null) {
                Object v = visit(ctx.unary());
                if (v instanceof Number n) return -n.doubleValue();
                throw new ExpressionException(
                        "expression.type_error", "Unary minus on non-number: " + v);
            }
            return visit(ctx.primary());
        }

        @Override
        public Object visitPrimary(TmpExprParser.PrimaryContext ctx) {
            if (ctx.expression() != null) return visit(ctx.expression());
            if (ctx.literal() != null) return visit(ctx.literal());
            if (ctx.functionCall() != null) return visit(ctx.functionCall());
            return visit(ctx.path());
        }

        @Override
        public Object visitLiteral(TmpExprParser.LiteralContext ctx) {
            String text = ctx.getText();
            if (ctx.NUMBER() != null) {
                // Don't use ternary — it auto-promotes Long to Double via type unification.
                if (text.contains(".")) return Double.parseDouble(text);
                return Long.parseLong(text);
            }
            if (ctx.STRING() != null) {
                // Strip surrounding quotes and unescape \\ and \"/\'
                String body = text.substring(1, text.length() - 1);
                return body.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\'", "'");
            }
            return switch (text) {
                case "true" -> Boolean.TRUE;
                case "false" -> Boolean.FALSE;
                case "null" -> null;
                default -> throw new ExpressionException(
                        "expression.unknown_literal", "Unknown literal: " + text);
            };
        }

        @Override
        public Object visitPath(TmpExprParser.PathContext ctx) {
            Object cursor = data;
            for (var ident : ctx.IDENT()) {
                cursor = getField(cursor, ident.getText());
                if (cursor == null) return null;
            }
            return cursor;
        }

        @Override
        public Object visitFunctionCall(TmpExprParser.FunctionCallContext ctx) {
            String name = ctx.IDENT().getText();
            List<Object> args = ctx.expression().stream().map(this::visit).toList();
            return Functions.call(name, args);
        }

        @SuppressWarnings("unchecked")
        private static Object getField(Object container, String key) {
            if (container instanceof Map<?, ?> m) return ((Map<String, Object>) m).get(key);
            return null;
        }

        private static boolean eq(Object a, Object b) {
            if (a == null || b == null) return a == b;
            if (a instanceof Number na && b instanceof Number nb) {
                return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
            }
            return a.equals(b);
        }

        private static int cmp(Object a, Object b) {
            if (a instanceof Number na && b instanceof Number nb) {
                return Double.compare(na.doubleValue(), nb.doubleValue());
            }
            if (a instanceof String sa && b instanceof String sb) {
                return sa.compareTo(sb);
            }
            throw new ExpressionException(
                    "expression.type_error",
                    "Cannot compare " + typeOf(a) + " and " + typeOf(b));
        }

        private static Object add(Object a, Object b) {
            if (a instanceof String || b instanceof String) {
                return String.valueOf(a) + String.valueOf(b);
            }
            return arith(a, b, "+");
        }

        private static Object arith(Object a, Object b, String op) {
            if (!(a instanceof Number na) || !(b instanceof Number nb)) {
                throw new ExpressionException(
                        "expression.type_error",
                        "Arithmetic requires numbers: " + typeOf(a) + " " + op + " " + typeOf(b));
            }
            double x = na.doubleValue();
            double y = nb.doubleValue();
            double r = switch (op) {
                case "+" -> x + y;
                case "-" -> x - y;
                case "*" -> x * y;
                case "/" -> x / y;
                default -> throw new ExpressionException(
                        "expression.unknown_op", "Unknown operator: " + op);
            };
            // Preserve integer shape when inputs are integer.
            if (na instanceof Long && nb instanceof Long && r == Math.floor(r)) return (long) r;
            return r;
        }

        private static String typeOf(Object o) {
            if (o == null) return "null";
            return o.getClass().getSimpleName();
        }
    }

    /** Truthy coercion — booleans as-is, null is false, non-empty strings/collections are true. */
    private static final class Truthy {
        static boolean asBoolean(Object v) {
            if (v == null) return false;
            if (v instanceof Boolean b) return b;
            if (v instanceof Number n) return n.doubleValue() != 0.0;
            if (v instanceof String s) return !s.isEmpty();
            if (v instanceof Collection<?> c) return !c.isEmpty();
            if (v instanceof Map<?, ?> m) return !m.isEmpty();
            return true;
        }
    }

    /** Whitelisted pure functions. Adding a new function = one new case here. */
    private static final class Functions {
        static Object call(String name, List<Object> args) {
            return switch (name) {
                case "len" -> len(arg(args, 0, name));
                case "empty" -> !Truthy.asBoolean(arg(args, 0, name));
                case "today" -> LocalDate.now().toString();
                case "formatDate" -> formatDate(arg(args, 0, name), str(args, 1, name));
                default -> throw new ExpressionException(
                        "expression.unknown_function", "Unknown function: " + name);
            };
        }

        private static Object arg(List<Object> args, int i, String name) {
            if (i >= args.size()) {
                throw new ExpressionException(
                        "expression.arity", "Function " + name + " missing argument " + i);
            }
            return args.get(i);
        }

        private static String str(List<Object> args, int i, String name) {
            Object v = arg(args, i, name);
            if (v instanceof String s) return s;
            throw new ExpressionException(
                    "expression.type_error",
                    "Function " + name + " argument " + i + " must be a string, got " + v);
        }

        private static long len(Object v) {
            if (v == null) return 0;
            if (v instanceof String s) return s.length();
            if (v instanceof Collection<?> c) return c.size();
            if (v instanceof Map<?, ?> m) return m.size();
            throw new ExpressionException(
                    "expression.type_error", "len() on unsupported type: " + v);
        }

        private static String formatDate(Object dateValue, String pattern) {
            if (dateValue == null) return "";
            LocalDate d;
            if (dateValue instanceof String s) {
                d = LocalDate.parse(s);
            } else if (dateValue instanceof LocalDate ld) {
                d = ld;
            } else {
                throw new ExpressionException(
                        "expression.type_error",
                        "formatDate expects ISO string or LocalDate, got " + dateValue);
            }
            return d.format(DateTimeFormatter.ofPattern(pattern));
        }
    }
}
