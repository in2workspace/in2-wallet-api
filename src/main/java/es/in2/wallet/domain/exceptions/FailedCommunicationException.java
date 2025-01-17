package es.in2.wallet.domain.exceptions;
public class FailedCommunicationException extends Exception {

    public FailedCommunicationException(String message) {
        super(message);
    }

    public FailedCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

}
