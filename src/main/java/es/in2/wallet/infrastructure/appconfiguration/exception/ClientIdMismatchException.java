package es.in2.wallet.infrastructure.appconfiguration.exception;

public class ClientIdMismatchException extends RuntimeException {
    public ClientIdMismatchException(String message) {
        super(message);
    }
}
