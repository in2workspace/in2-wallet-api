package es.in2.wallet.domain.exception;

public class AttestationServerErrorException extends Exception {
    public AttestationServerErrorException(String message) {
        super(message);
    }
}