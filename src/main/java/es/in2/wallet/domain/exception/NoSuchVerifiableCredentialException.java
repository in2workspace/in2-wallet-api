package es.in2.wallet.domain.exception;

public class NoSuchVerifiableCredentialException extends Exception {

    public NoSuchVerifiableCredentialException(String message) {
        super(message);
    }
}
