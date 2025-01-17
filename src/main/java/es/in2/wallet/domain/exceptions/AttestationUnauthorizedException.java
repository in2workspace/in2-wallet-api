package es.in2.wallet.domain.exceptions;

public class AttestationUnauthorizedException extends RuntimeException {

    public AttestationUnauthorizedException(String message) {
        super(message);
    }
}
