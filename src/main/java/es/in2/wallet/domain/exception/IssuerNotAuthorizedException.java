package es.in2.wallet.domain.exception;

public class IssuerNotAuthorizedException extends RuntimeException {
    public IssuerNotAuthorizedException(String message) {
        super(message);
    }
}
