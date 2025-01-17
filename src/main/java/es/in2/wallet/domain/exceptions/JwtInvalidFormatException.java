package es.in2.wallet.domain.exceptions;

public class JwtInvalidFormatException extends  Exception{
    public JwtInvalidFormatException(String message) {
        super(message);
    }

}
