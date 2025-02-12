package es.in2.wallet.domain.exceptions;

public class IssuerNotAuthorizedException extends RuntimeException {
    public IssuerNotAuthorizedException(String message) {
        super(message);
    }
}
