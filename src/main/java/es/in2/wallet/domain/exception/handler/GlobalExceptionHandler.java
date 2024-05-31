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
    public Mono<GlobalErrorMessage> failedCommunicationException(FailedCommunicationException failedCommunicationException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        log.debug("failedCommunicationException", failedCommunicationException);
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedCommunicationException")
                .message(failedCommunicationException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(FailedDeserializingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<GlobalErrorMessage> failedDeserializingException(FailedDeserializingException failedDeserializingException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedDeserializingException")
                .message(failedDeserializingException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(FailedSerializingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<GlobalErrorMessage> failedSerializingException(FailedSerializingException failedSerializingException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("FailedSerializingException")
                .message(failedSerializingException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(JwtInvalidFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<GlobalErrorMessage> jwtInvalidFormatException(JwtInvalidFormatException jwtInvalidFormatException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("JwtInvalidFormatException")
                .message(jwtInvalidFormatException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(NoSuchQrContentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<GlobalErrorMessage> noSuchQrContentException(NoSuchQrContentException noSuchQrContentException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("NoSuchQrContentException")
                .message(noSuchQrContentException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(ParseErrorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<GlobalErrorMessage> parseErrorException(ParseErrorException parseErrorException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("ParseErrorException")
                .message(parseErrorException.getMessage())
                .path(path)
                .build());
    }
    @ExceptionHandler(NoSuchVerifiableCredentialException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<GlobalErrorMessage> noSuchVerifiableCredentialException(NoSuchVerifiableCredentialException noSuchVerifiableCredentialException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("NoSuchVerifiableCredentialException")
                .message(noSuchVerifiableCredentialException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(NoSuchTransactionException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Mono<GlobalErrorMessage> noSuchTransactionException(NoSuchTransactionException noSuchTransactionException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("NoSuchTransactionException")
                .message(noSuchTransactionException.getMessage())
                .path(path)
                .build());
    }
    @ExceptionHandler(InvalidPinException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Mono<GlobalErrorMessage> invalidPinException(InvalidPinException invalidPinException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("InvalidPinException")
                .message(invalidPinException.getMessage())
                .path(path)
                .build());
    }

    @ExceptionHandler(CredentialNotAvailableException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public Mono<GlobalErrorMessage> credentialNotAvailableException(CredentialNotAvailableException credentialNotAvailableException, ServerHttpRequest request) {
        String path = String.valueOf(request.getPath());
        return Mono.just(GlobalErrorMessage.builder()
                .title("CredentialNotAvailableException")
                .message(credentialNotAvailableException.getMessage())
                .path(path)
                .build());
    }
}
