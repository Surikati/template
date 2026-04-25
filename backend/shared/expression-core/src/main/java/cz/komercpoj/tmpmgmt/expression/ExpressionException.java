package cz.komercpoj.tmpmgmt.expression;

import cz.komercpoj.tmpmgmt.common.DomainException;

public class ExpressionException extends DomainException {
  public ExpressionException(String errorCode, String message) {
    super(errorCode, message);
  }

  public ExpressionException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
