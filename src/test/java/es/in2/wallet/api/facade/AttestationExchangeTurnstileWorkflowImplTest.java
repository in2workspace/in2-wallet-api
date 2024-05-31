package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonParseException;
import es.in2.wallet.application.workflow.presentation.impl.AttestationExchangeTurnstileWorkflowImpl;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.service.CborGenerationService;
import es.in2.wallet.domain.service.PresentationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.text.ParseException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttestationExchangeTurnstileWorkflowImplTest {
    @Mock
    private PresentationService presentationService;
    @Mock
    private CborGenerationService cborGenerationService;
    @InjectMocks
    private AttestationExchangeTurnstileWorkflowImpl credentialPresentationForTurnstileServiceFacade;
    @Test
    void createVerifiablePresentationForTurnstileTestSuccess() throws ParseException {
        String processId = "123";
        String authorizationToken = "authToken";
        String audience = "vpTurnstile";
        String expectedVp = "vp";
        String expectedCBOR = "vp_cbor";
        CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().id("id").build();
        when(presentationService.createSignedVerifiablePresentation(processId, authorizationToken, credentialsBasicInfo, credentialsBasicInfo.id(), audience)).thenReturn(Mono.just("vp"));
        when(cborGenerationService.generateCbor(processId, expectedVp)).thenReturn(Mono.just("vp_cbor"));

        StepVerifier.create(credentialPresentationForTurnstileServiceFacade.createVerifiablePresentationForTurnstile(processId, authorizationToken, credentialsBasicInfo))
                .expectNext(expectedCBOR)
                .verifyComplete();

    }
    @Test
    void createVerifiablePresentationForTurnstileTestFailure() throws ParseException {
        String processId = "123";
        String authorizationToken = "authToken";
        String audience = "vpTurnstile";
        String expectedVp = "vp";
        CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().id("id").build();
        when(presentationService.createSignedVerifiablePresentation(processId, authorizationToken, credentialsBasicInfo, credentialsBasicInfo.id(), audience)).thenReturn(Mono.just("vp"));
        when(cborGenerationService.generateCbor(processId, expectedVp)).thenThrow(new ParseException("Simulated CBOR generation error", 0));

        StepVerifier.create(credentialPresentationForTurnstileServiceFacade.createVerifiablePresentationForTurnstile(processId, authorizationToken, credentialsBasicInfo))
                .expectErrorMatches(error -> error instanceof JsonParseException && error.getMessage().contains("Error parsing the Verifiable Presentation"))
                .verify();

    }
}
