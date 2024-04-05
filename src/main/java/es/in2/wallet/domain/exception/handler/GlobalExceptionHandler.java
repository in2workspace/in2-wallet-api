package es.in2.wallet.domain.exception.handler;

import es.in2.wallet.domain.exception.*;
import es.in2.wallet.domain.model.GlobalErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(FailedCommunicationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public Mono<GlobalErrorMessage> failedCommunicationException(FailedCommunicationException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        log.debug("failedCommunicationException", ex);
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedCommunicationException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(FailedDeserializingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<GlobalErrorMessage> failedDeserializingException(FailedDeserializingException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedDeserializingException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(FailedSerializingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<GlobalErrorMessage> failedSerializingException(FailedSerializingException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedSerializingException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(JwtInvalidFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<GlobalErrorMessage> jwtInvalidFormatException(JwtInvalidFormatException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("JwtInvalidFormatException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(NoSuchQrContentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<GlobalErrorMessage> noSuchQrContentException(NoSuchQrContentException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("NoSuchQrContentException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(ParseErrorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<GlobalErrorMessage> parseErrorException(ParseErrorException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("ParseErrorException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }
    @ExceptionHandler(NoSuchVerifiableCredentialException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<GlobalErrorMessage> noSuchVerifiableCredentialException(NoSuchVerifiableCredentialException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("NoSuchVerifiableCredentialException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(InvalidPinException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Mono<GlobalErrorMessage> invalidCredentialsException(InvalidPinException ex, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("InvalidCredentialsException")
                .message(ex.getMessage())
                .path(path)
                .build());
    }
}
