package es.in2.wallet.domain.exception;

public class NoSuchTransactionException extends Exception {

    public NoSuchTransactionException(String message) {
        super(message);
    }
}
