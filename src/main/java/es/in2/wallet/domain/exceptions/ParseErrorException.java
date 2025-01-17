package es.in2.wallet.domain.exceptions;

public class ParseErrorException extends RuntimeException {
    public ParseErrorException(String message) {
        super(message);
    }
}

