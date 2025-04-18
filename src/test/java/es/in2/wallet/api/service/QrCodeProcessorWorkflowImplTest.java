package es.in2.wallet.api.service;

import es.in2.wallet.application.workflows.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.application.workflows.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.domain.exceptions.NoSuchQrContentException;
import es.in2.wallet.application.dto.VcSelectorRequest;
import es.in2.wallet.application.workflows.processor.impl.QrCodeProcessorWorkflowImpl;
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
    private CredentialIssuanceCommonWorkflow credentialIssuanceCommonWorkflow;
    @Mock
    private CredentialIssuanceEbsiWorkflow credentialIssuanceEbsiWorkflow;
    @Mock
    private AttestationExchangeCommonWorkflow attestationExchangeCommonWorkflow;

    @InjectMocks
    private QrCodeProcessorWorkflowImpl qrCodeProcessorService;

    @Test
    void processQrContentCredentialOfferUriSuccess() {
        String qrContent = "https://credential-offer";
        String processId = "processId";
        String authorizationToken = "authToken";
        when(credentialIssuanceCommonWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenReturn(Mono.empty());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .verifyComplete();
    }
    @Test
    void processQrContentCredentialOfferUriFailure() {
        String qrContent = "https://credential-offer";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(credentialIssuanceCommonWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenThrow(new RuntimeException());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void processQrContentVcLoginRequestFailure() {
        String qrContent = "https://authentication-request?=response_type=vp_token";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(attestationExchangeCommonWorkflow.processAuthorizationRequest(processId, authorizationToken, qrContent)).thenThrow(new RuntimeException());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void processQrContentVcLoginRequestSuccess() {
        String qrContent = "https://authentication-request?response_type=vp_token";
        String processId = "processId";
        String authorizationToken = "authToken";
        VcSelectorRequest vcSelectorRequest = VcSelectorRequest.builder().build();

        when(attestationExchangeCommonWorkflow.processAuthorizationRequest(processId, authorizationToken, qrContent)).thenReturn(Mono.just(vcSelectorRequest));

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectNext(vcSelectorRequest)
                .verifyComplete();
    }

    @Test
    void processQrContentOpenIdCredentialOffer() {
        String qrContent = "openid-credential-offer://";
        String processId = "processId";
        String authorizationToken = "authToken";
        when(credentialIssuanceCommonWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenReturn(Mono.empty());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .verifyComplete();
    }
    @Test
    void processQrContentOpenIdCredentialOfferFailure() {
        String qrContent = "openid-credential-offer://";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(credentialIssuanceCommonWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenThrow(new RuntimeException());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
                .verify();
    }
    @Test
    void processQrContentCredentialOfferUriForEbsiTestSuccess() {
        String qrContent = "test-openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F9b1f0db1-45b6-4c93-a9be-79d3137f8fff";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(credentialIssuanceEbsiWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenReturn(Mono.empty());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .verifyComplete();
    }
    @Test
    void processQrContentCredentialOfferUriForEbsiTestFailure() {
        String qrContent = "test-openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fapi-conformance.ebsi.eu%2Fconformance%2Fv3%2Fissuer-mock%2Foffers%2F9b1f0db1-45b6-4c93-a9be-79d3137f8fff";
        String processId = "processId";
        String authorizationToken = "authToken";

        when(credentialIssuanceEbsiWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)).thenThrow(new RuntimeException());

        StepVerifier.create(qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent))
                .expectError(RuntimeException.class)
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
                .expectErrorMatches(throwable -> throwable instanceof NoSuchQrContentException &&
                        expectedErrorMessage.equals(throwable.getMessage()))
                .verify();
    }
}
