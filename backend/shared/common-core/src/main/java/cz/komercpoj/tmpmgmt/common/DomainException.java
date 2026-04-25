package cz.komercpoj.tmpmgmt.common;

/**
 * Base type for all business/domain-level exceptions. Carries a stable error code so callers
 * (including the frontend) can react programmatically instead of matching on messages.
 */
public class DomainException extends RuntimeException {

  private final String errorCode;

  public DomainException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public DomainException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
