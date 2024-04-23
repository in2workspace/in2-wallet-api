package es.in2.wallet.api.exception;

import es.in2.wallet.domain.exception.*;
import es.in2.wallet.domain.exception.handler.GlobalExceptionHandler;
import es.in2.wallet.domain.model.GlobalErrorMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    private static GlobalExceptionHandler globalExceptionHandler;
    private ServerHttpRequest request;
    private RequestPath requestPath;

    static Stream<Arguments> provideData() {
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(
                FailedCommunicationException.class,
                FailedDeserializingException.class,
                FailedSerializingException.class,
                JwtInvalidFormatException.class,
                NoSuchQrContentException.class,
                ParseErrorException.class,
                NoSuchVerifiableCredentialException.class,
                NoSuchTransactionException.class,
                InvalidPinException.class,
                CredentialNotAvailableException.class
        ));

        List<String> messages = new ArrayList<>(Arrays.asList(
                "FailedCommunication",
                "FailedDeserializing",
                "FailedSerializing",
                "JwtInvalidFormat",
                "NoSuchQrContent",
                "ParseError",
                "NoSuchVerifiableCredential",
                "NoSuchTransaction",
                "InvalidPin",
                "CredentialNotAvailable"
        ));

        List<BiFunction<Exception, ServerHttpRequest, Mono<GlobalErrorMessage>>> methods = new ArrayList<>(Arrays.asList(
                (ex, req) -> globalExceptionHandler.failedCommunicationException((FailedCommunicationException) ex, req),
                (ex, req) -> globalExceptionHandler.failedDeserializingException((FailedDeserializingException) ex, req),
                (ex, req) -> globalExceptionHandler.failedSerializingException((FailedSerializingException) ex, req),
                (ex, req) -> globalExceptionHandler.jwtInvalidFormatException((JwtInvalidFormatException) ex, req),
                (ex, req) -> globalExceptionHandler.noSuchQrContentException((NoSuchQrContentException) ex, req),
                (ex, req) -> globalExceptionHandler.parseErrorException((ParseErrorException) ex, req),
                (ex, req) -> globalExceptionHandler.noSuchVerifiableCredentialException((NoSuchVerifiableCredentialException) ex, req),
                (ex, req) -> globalExceptionHandler.noSuchTransactionException((NoSuchTransactionException) ex, req),
                (ex, req) -> globalExceptionHandler.invalidPinException((InvalidPinException) ex, req),
                (ex, req) -> globalExceptionHandler.credentialNotAvailableException((CredentialNotAvailableException) ex, req)

        ));

        Map<Class<? extends Exception>, String> exceptionMethodNames = new HashMap<>();
        exceptionMethodNames.put(FailedCommunicationException.class, "FailedCommunicationException");
        exceptionMethodNames.put(FailedDeserializingException.class, "FailedDeserializingException");
        exceptionMethodNames.put(FailedSerializingException.class, "FailedSerializingException");
        exceptionMethodNames.put(JwtInvalidFormatException.class, "JwtInvalidFormatException");
        exceptionMethodNames.put(NoSuchQrContentException.class, "NoSuchQrContentException");
        exceptionMethodNames.put(ParseErrorException.class, "ParseErrorException");
        exceptionMethodNames.put(NoSuchVerifiableCredentialException.class, "NoSuchVerifiableCredentialException");
        exceptionMethodNames.put(NoSuchTransactionException.class, "NoSuchTransactionException");
        exceptionMethodNames.put(InvalidPinException.class, "InvalidPinException");
        exceptionMethodNames.put(CredentialNotAvailableException.class, "CredentialNotAvailableException");

        return IntStream.range(0, classes.size())
                .mapToObj(i -> Arguments.of(classes.get(i), messages.get(i), methods.get(i % methods.size()), exceptionMethodNames));

    }

    @BeforeEach
    void setup() {
        request = mock(ServerHttpRequest.class);
        requestPath = mock(RequestPath.class);
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @ParameterizedTest
    @MethodSource("provideData")
    void testExceptions(Class<? extends Exception> exceptionClass, String message,
                        BiFunction<Exception, ServerHttpRequest, Mono<GlobalErrorMessage>> method,
                        Map<Class<? extends Exception>, String> exceptionMethodNames) throws NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        // Mock
        when(request.getPath()).thenReturn(requestPath);
        // Act
        Exception exception = exceptionClass.getConstructor(String.class)
                .newInstance(message);

        String title = exceptionMethodNames.get(exceptionClass);

        GlobalErrorMessage globalErrorMessage =
                GlobalErrorMessage.builder()
                        .title(title)
                        .message(message)
                        .path(String.valueOf(requestPath))
                        .build();
        //Assert
        StepVerifier.create(method.apply(exception, request))
                .expectNext(globalErrorMessage)
                .verifyComplete();
    }
}
