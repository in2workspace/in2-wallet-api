package es.in2.wallet.domain.exception;

public class BindingMethodUnsupportedException extends RuntimeException {
    public BindingMethodUnsupportedException(String message) {
        super(message);
    }
}