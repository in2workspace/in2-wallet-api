package es.in2.wallet.domain.exceptions;

public class AttestationClientErrorException extends RuntimeException {

    public AttestationClientErrorException(String message) {
        super(message);
    }
}