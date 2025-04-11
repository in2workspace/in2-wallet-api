package es.in2.wallet.infrastructure.appconfiguration.exception;

public class InvalidClientException extends RuntimeException {
    public InvalidClientException(String message) {
        super(message);
    }
}
