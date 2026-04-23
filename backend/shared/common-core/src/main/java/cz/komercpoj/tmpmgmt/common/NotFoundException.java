package cz.komercpoj.tmpmgmt.common;

public class NotFoundException extends DomainException {
    public NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
