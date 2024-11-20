package es.in2.wallet.domain.exception;

public class AttestationServerErrorException extends RuntimeException {

    public AttestationServerErrorException(String message) {
        super(message);
    }
}