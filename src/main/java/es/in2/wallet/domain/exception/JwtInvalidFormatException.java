package es.in2.wallet.domain.exception;

public class JwtInvalidFormatException extends  Exception{
    public JwtInvalidFormatException(String message) {
        super(message);
    }

}
