package cz.komercpoj.tmpmgmt.common;

public class ConflictException extends DomainException {
  public ConflictException(String errorCode, String message) {
    super(errorCode, message);
  }
}
