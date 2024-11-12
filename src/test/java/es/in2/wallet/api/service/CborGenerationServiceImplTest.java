package es.in2.wallet.api.service;

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.OneKey;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.service.impl.CborGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class CborGenerationServiceImplTest {

    @InjectMocks
    private CborGenerationServiceImpl cborGenerationService;

    private String validContent;
    private String invalidContent;

    @BeforeEach
    void setUp() {
        validContent = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2cCI6eyJpZCI6InMiLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iXSwiaG9sZGVyIjoicyIsIkBjb250ZXh0IjpbInMiXSwidmVyaWZpYWJsZUNyZWRlbnRpYWwiOlsiYSJdfSwiZXhwIjoxNzA4NzUwNjE2LCJpYXQiOjE3MDg2OTA2MTYsImlzcyI6InMiLCJqdGkiOiJzIiwibmJmIjoxNzA4NjkwNjE2LCJzdWIiOiJzIiwibm9uY2UiOiJzIn0.3vEfvTOP6Y38zHBuHypon2qcLshl1ZxcHHAjIY6z6JQ";
        invalidContent = "invalid_token";
    }

    @Test
    void generateCborTest() {
        String processId = "123";
        String content = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2cCI6eyJpZCI6InMiLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iXSwiaG9sZGVyIjoicyIsIkBjb250ZXh0IjpbInMiXSwidmVyaWZpYWJsZUNyZWRlbnRpYWwiOlsiYSJdfSwiZXhwIjoxNzA4NzUwNjE2LCJpYXQiOjE3MDg2OTA2MTYsImlzcyI6InMiLCJqdGkiOiJzIiwibmJmIjoxNzA4NjkwNjE2LCJzdWIiOiJzIiwibm9uY2UiOiJzIn0.3vEfvTOP6Y38zHBuHypon2qcLshl1ZxcHHAjIY6z6JQ";
        StepVerifier.create(cborGenerationService.generateCbor(processId, content))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    void generateCborTest_withInvalidContent_shouldReturnParseErrorException() {
        String processId = "123";
        StepVerifier.create(cborGenerationService.generateCbor(processId, invalidContent))
                .expectErrorMatches(throwable ->
                        throwable instanceof ParseErrorException &&
                                throwable.getMessage().contains("Failed to parse token payload"))
                .verify();
    }

    @Test
    void generateCborTest_withValidContent() {
        String processId = "123";
        StepVerifier.create(cborGenerationService.generateCbor(processId, validContent))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    void generateCOSEBytesFromCBOR_withInvalidCbor_shouldThrowCoseException() {
        String processId = "123";

        try (MockedStatic<OneKey> mockedOneKey = Mockito.mockStatic(OneKey.class)) {
            mockedOneKey.when(() -> OneKey.generateKey(AlgorithmID.ECDSA_256))
                    .thenThrow(new CoseException("Forced COSE exception for testing"));

            StepVerifier.create(cborGenerationService.generateCbor(processId, validContent))
                    .expectErrorMatches(throwable ->
                            throwable instanceof CoseException &&
                                    throwable.getMessage().contains("Error generating COSE bytes from CBOR"))
                    .verify();
        }
    }

}
