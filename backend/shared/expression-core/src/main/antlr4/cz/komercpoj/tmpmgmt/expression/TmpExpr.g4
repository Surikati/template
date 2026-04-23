grammar TmpExpr;

// Restricted expression language for template conditions (when, visibilityRule).
// Supports: comparisons, boolean ops, arithmetic, dot-path data access, literal values,
// and a whitelist of pure functions (len, empty, today, formatDate).
// NO property assignment, NO method calls on user data, NO reflection hooks.

// Top-level rule anchors EOF so the whole input must be consumed. Nested contexts use
// `expression` (no EOF) so parenthesised sub-expressions and function arguments parse cleanly.
program    : expression EOF ;

expression : orExpr ;
orExpr     : andExpr ('||' andExpr)* ;
andExpr    : notExpr ('&&' notExpr)* ;
notExpr    : '!' notExpr
           | comparison ;
comparison : additive (op=('==' | '!=' | '<' | '>' | '<=' | '>=') additive)? ;
additive   : multiplicative (ops+=('+' | '-') multiplicative)* ;
multiplicative : unary (ops+=('*' | '/') unary)* ;
unary      : '-' unary
           | primary ;
primary    : '(' expression ')'
           | literal
           | functionCall
           | path ;

path       : IDENT ('.' IDENT)* ;
functionCall : IDENT '(' (expression (',' expression)*)? ')' ;

literal    : NUMBER | STRING | 'true' | 'false' | 'null' ;

NUMBER     : [0-9]+ ('.' [0-9]+)? ;
STRING     : '\'' (~['\\] | '\\' .)* '\''
           | '"'  (~["\\] | '\\' .)* '"' ;
IDENT      : [a-zA-Z_][a-zA-Z_0-9]* ;

WS         : [ \t\r\n]+ -> skip ;
