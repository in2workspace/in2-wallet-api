package es.in2.wallet.domain.exceptions;

public class AttestationServerErrorException extends RuntimeException {

    public AttestationServerErrorException(String message) {
        super(message);
    }
}