package es.in2.wallet.infrastructure.appconfiguration.exception;

public class MissingAuthorizationRequestParameterException extends RuntimeException {
    public MissingAuthorizationRequestParameterException(String message) {
        super(message);
    }
}
