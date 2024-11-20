package es.in2.wallet.domain.exception;

public class AttestationUnauthorizedException extends Exception {
    public AttestationUnauthorizedException(String message) {
        super(message);
    }
}
