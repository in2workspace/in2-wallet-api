package es.in2.wallet.domain.exception;

public class InvalidPinException extends Exception {
    public InvalidPinException(String message) {
        super(message);
    }

}