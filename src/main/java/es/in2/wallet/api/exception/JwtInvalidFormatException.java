package es.in2.wallet.api.exception;

public class JwtInvalidFormatException extends  Exception{
    public JwtInvalidFormatException(String message) {
        super(message);
    }

}
