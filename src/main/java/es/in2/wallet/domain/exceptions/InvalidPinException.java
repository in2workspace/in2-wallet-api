package es.in2.wallet.domain.exceptions;

public class InvalidPinException extends Exception {
    public InvalidPinException(String message) {
        super(message);
    }

}