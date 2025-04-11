package es.in2.wallet.infrastructure.appconfiguration.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
