package cz.komercpoj.tmpmgmt.common;

import java.util.List;

public class ValidationException extends DomainException {

    private final List<String> violations;

    public ValidationException(String errorCode, String message, List<String> violations) {
        super(errorCode, message);
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
