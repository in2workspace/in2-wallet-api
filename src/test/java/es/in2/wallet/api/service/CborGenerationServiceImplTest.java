package es.in2.wallet.api.service;

import es.in2.wallet.api.service.impl.CborGenerationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.text.ParseException;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
public class CborGenerationServiceImplTest {

    @InjectMocks
    private CborGenerationServiceImpl cborGenerationService;

    @Test
    void generateCborTest() throws ParseException {
        String processId = "123";
        String content = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        StepVerifier.create(cborGenerationService.generateCbor(processId, content))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

}
