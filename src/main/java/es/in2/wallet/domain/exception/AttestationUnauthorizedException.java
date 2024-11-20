package es.in2.wallet.domain.exception;

public class AttestationUnauthorizedException extends RuntimeException {

    public AttestationUnauthorizedException(String message) {
        super(message);
    }
}
