package es.in2.wallet.api.service;

import es.in2.wallet.application.workflows.issuance.Oid4vciWorkflow;
import es.in2.wallet.application.workflows.presentation.Oid4vpWorkflow;
import es.in2.wallet.application.dto.VcSelectorRequest;
import es.in2.wallet.application.workflows.processor.impl.QrCodeProcessorWorkflowImpl;
import es.in2.wallet.domain.exceptions.NoSuchQrContentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeProcessorWorkflowImplTest {
    @Mock
    private Oid4vciWorkflow oid4vciWorkflow;
    @Mock
    private Oid4vpWorkflow oid4vpWorkflow;

    @InjectMocks
    private QrCodeProcessorWorkflowImpl qrCodeProcessorService;

    @Test
    void processQrContentOid4vciCredentialOfferSuccess() {
        String qrContent = "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fexample.com%2Foffer";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(oid4vciWorkflow.execute(processId, authorizationToken, qrContent)).thenReturn(Mono.empty());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .verifyComplete();
    }

    @Test
    void processQrContentCredentialOfferUriFailure() {
        String qrContent = "openid-credential-offer://";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(oid4vciWorkflow.execute(processId, authorizationToken, qrContent)).thenReturn(Mono.error(new RuntimeException()));

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void processQrContentVcLoginRequestFailure() {
        String qrContent = "https://authentication-request?=response_type=vp_token";
        String processId = "processId";
        String authorizationToken = "authToken";

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(NoSuchQrContentException.class)
                .verify();
    }

    @Test
    void processQrContentVcLoginRequestSuccess() {
        String qrContent = "openid4vp://mock.request.jwt";
        String processId = "processId";
        String authorizationToken = "authToken";
        VcSelectorRequest vcSelectorRequest = VcSelectorRequest.builder().build();

        when(oid4vpWorkflow.processAuthorizationRequest(processId, authorizationToken, qrContent)).thenReturn(Mono.just(vcSelectorRequest));

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectNext(vcSelectorRequest)
                .verifyComplete();
    }

    @Test
    void processQrContentOpenIdCredentialOffer() {
        String qrContent = "openid-credential-offer://";
        String processId = "processId";
        String authorizationToken = "authToken";
        when(oid4vciWorkflow.execute(processId, authorizationToken, qrContent)).thenReturn(Mono.empty());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .verifyComplete();
    }
    @Test
    void processQrContentOpenIdCredentialOfferFailure() {
        String qrContent = "openid-credential-offer://";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(oid4vciWorkflow.execute(processId, authorizationToken, qrContent)).thenThrow(new RuntimeException());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void processQrContentCredentialOfferUriForEbsiTestFailure() {
        String qrContent = "test-openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2F...";
        String processId = "processId";
        String authorizationToken = "authToken";

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(NoSuchQrContentException.class)
                .verify();
    }

    @Test
    void processQrContentOpenIdAuthenticationRequestFailure() {
        String qrContent = "openid://";
        String processId = "processId";
        String authorizationToken = "authToken";

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(NoSuchQrContentException.class)
                .verify();
    }

    @Test
    void processQrContentUnknown() {
        String qrContent = "unknownContent";
        String processId = "processId";
        String authorizationToken = "authToken";
        String expectedErrorMessage = "The received QR content cannot be processed";

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectErrorMatches(throwable ->
                        throwable instanceof NoSuchQrContentException &&
                                expectedErrorMessage.equals(throwable.getMessage()))
                .verify();
    }
}
