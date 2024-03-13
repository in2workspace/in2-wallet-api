package es.in2.wallet.domain.exception;

public class KeyPairGenerationError extends RuntimeException {
    public KeyPairGenerationError(String message) {
        super(message);
    }
}
