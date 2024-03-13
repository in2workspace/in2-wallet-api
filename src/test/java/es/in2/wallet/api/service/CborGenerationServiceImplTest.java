package es.in2.wallet.api.service;

import es.in2.wallet.domain.service.impl.CborGenerationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.text.ParseException;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class CborGenerationServiceImplTest {

    @InjectMocks
    private CborGenerationServiceImpl cborGenerationService;

    @Test
    void generateCborTest() throws ParseException {
        String processId = "123";
        String content = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2cCI6eyJpZCI6InMiLCJ0eXBlIjpbIlZlcmlmaWFibGVQcmVzZW50YXRpb24iXSwiaG9sZGVyIjoicyIsIkBjb250ZXh0IjpbInMiXSwidmVyaWZpYWJsZUNyZWRlbnRpYWwiOlsiYSJdfSwiZXhwIjoxNzA4NzUwNjE2LCJpYXQiOjE3MDg2OTA2MTYsImlzcyI6InMiLCJqdGkiOiJzIiwibmJmIjoxNzA4NjkwNjE2LCJzdWIiOiJzIiwibm9uY2UiOiJzIn0.3vEfvTOP6Y38zHBuHypon2qcLshl1ZxcHHAjIY6z6JQ";
        StepVerifier.create(cborGenerationService.generateCbor(processId, content))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

}
