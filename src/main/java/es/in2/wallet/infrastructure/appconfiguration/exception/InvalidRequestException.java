package es.in2.wallet.infrastructure.appconfiguration.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
