package es.in2.wallet.domain.exception;

public class CredentialConfigurationIdNotCompatible extends RuntimeException {
    public CredentialConfigurationIdNotCompatible(String message) {
        super(message);
    }
}