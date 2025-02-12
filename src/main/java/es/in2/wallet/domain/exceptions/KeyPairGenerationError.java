package es.in2.wallet.domain.exceptions;

public class KeyPairGenerationError extends RuntimeException {
    public KeyPairGenerationError(String message) {
        super(message);
    }
}
