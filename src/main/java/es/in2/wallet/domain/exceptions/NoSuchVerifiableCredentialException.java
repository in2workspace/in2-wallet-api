package es.in2.wallet.domain.exceptions;

public class NoSuchVerifiableCredentialException extends Exception {

    public NoSuchVerifiableCredentialException(String message) {
        super(message);
    }
}
