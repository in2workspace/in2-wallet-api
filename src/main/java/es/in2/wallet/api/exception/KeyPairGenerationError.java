package es.in2.wallet.api.exception;

public class KeyPairGenerationError extends RuntimeException {
    public KeyPairGenerationError(String message) {
        super(message);
    }
}
