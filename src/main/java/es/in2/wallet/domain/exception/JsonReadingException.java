package es.in2.wallet.domain.exception;

public class JsonReadingException extends RuntimeException {

    public JsonReadingException(String message) {
        super(message);
    }

    public JsonReadingException(String message, Throwable cause) {
        super(message, cause);
    }

}
