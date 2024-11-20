package es.in2.wallet.domain.exception;

public class AttestationClientErrorException extends RuntimeException {

    public AttestationClientErrorException(String message) {
        super(message);
    }
}