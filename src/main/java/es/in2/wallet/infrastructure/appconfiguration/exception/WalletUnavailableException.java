package es.in2.wallet.infrastructure.appconfiguration.exception;

public class WalletUnavailableException extends RuntimeException {
    public WalletUnavailableException(String message) {
        super(message);
    }
}
