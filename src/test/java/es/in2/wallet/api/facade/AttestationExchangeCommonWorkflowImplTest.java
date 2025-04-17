package es.in2.wallet.api.facade;

import es.in2.wallet.application.dto.AuthorizationRequestOIDC4VP;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.application.dto.VcSelectorRequest;
import es.in2.wallet.application.dto.VcSelectorResponse;
import es.in2.wallet.application.workflows.presentation.impl.AttestationExchangeCommonWorkflowImpl;
import es.in2.wallet.domain.services.*;
import es.in2.wallet.domain.utils.ApplicationUtils;
import es.in2.wallet.infrastructure.appconfiguration.exception.VpFormatsNotSupportedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static es.in2.wallet.domain.utils.ApplicationUtils.getUserIdFromToken;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttestationExchangeCommonWorkflowImplTest {
    @Mock
    private AuthorizationRequestService authorizationRequestService;
    @Mock
    private AuthorizationResponseService authorizationResponseService;
    @Mock
    private VerifierValidationService verifierValidationService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private PresentationService presentationService;
    @InjectMocks
    private AttestationExchangeCommonWorkflowImpl attestationExchangeServiceFacade;
    @Test
    void getSelectableCredentialsRequiredToBuildThePresentationTest() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "123";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            String jwtAuthorizationRequest = "authRequest";

            AuthorizationRequestOIDC4VP.DcqlCredential credential = AuthorizationRequestOIDC4VP.DcqlCredential.builder().id("scope1").format("jwt_vc_json").build();
            AuthorizationRequestOIDC4VP.DcqlQuery dcqlQuery = AuthorizationRequestOIDC4VP.DcqlQuery.builder().credentials(List.of(credential)).build();
            AuthorizationRequestOIDC4VP authorizationRequestOIDC4VP = AuthorizationRequestOIDC4VP.builder().scope(List.of("scope1")).responseUri("responseUri").state("state").dcqlQuery(dcqlQuery).build();
            CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().build();
            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder().redirectUri("responseUri").state("state").nonce(null).selectableVcList(List.of(credentialsBasicInfo)).build();

            when(authorizationRequestService.getJwtRequestObjectFromUri(processId, qrContent)).thenReturn(Mono.just(jwtAuthorizationRequest));
            when(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(jwtAuthorizationRequest));
            when(authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(authorizationRequestOIDC4VP));
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialService.getCredentialsByUserIdTypeAndFormat(processId, "userId", "scope1", "jwt_vc_json")).thenReturn(Mono.just(List.of(credentialsBasicInfo)));
            StepVerifier.create(attestationExchangeServiceFacade.processAuthorizationRequest(processId, authorizationToken, qrContent))
                    .expectNext(expectedVcSelectorRequest)
                    .verifyComplete();
        }
    }

    @Test
    void buildVerifiablePresentationWithSelectedVCsTest() {
        String processId = "123";
        String authorizationToken = "authToken";
        String nonce = "321";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().nonce(nonce).build();
        String verifiablePresentation = "vp";

        when(presentationService.createSignedVerifiablePresentation(
                processId,
                authorizationToken,
                vcSelectorResponse,
                nonce,
                "https://self-issued.me/v2"
        )).thenReturn(Mono.just(verifiablePresentation));
        when(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken)).thenReturn(Mono.empty());

        StepVerifier.create(attestationExchangeServiceFacade.buildVerifiablePresentationWithSelectedVCs(processId, authorizationToken, vcSelectorResponse))
                .verifyComplete();
    }

    @Test
    void processAuthorizationRequest_shouldThrowVpFormatsNotSupportedException_whenUnsupportedFormatIsPresent() {
        String processId = "123";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiJ1c2VySWQxMjMifQ."
                + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"; // sub = "userId123"
        String qrContent = "qrContent";
        String jwtAuthorizationRequest = "jwtRequest";
        AuthorizationRequestOIDC4VP.DcqlCredential unsupportedCredential =
                AuthorizationRequestOIDC4VP.DcqlCredential.builder()
                        .id("unsupported-scope")
                        .format("ldp_vc")
                        .build();

        AuthorizationRequestOIDC4VP.DcqlQuery dcqlQuery =
                AuthorizationRequestOIDC4VP.DcqlQuery.builder()
                        .credentials(List.of(unsupportedCredential))
                        .build();

        AuthorizationRequestOIDC4VP authorizationRequest =
                AuthorizationRequestOIDC4VP.builder()
                        .dcqlQuery(dcqlQuery)
                        .scope(List.of("unsupported-scope"))
                        .build();

        when(authorizationRequestService.getJwtRequestObjectFromUri(processId, qrContent))
                .thenReturn(Mono.just(jwtAuthorizationRequest));

        when(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest))
                .thenReturn(Mono.just(jwtAuthorizationRequest));

        when(authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequest))
                .thenReturn(Mono.just(authorizationRequest));

        when(credentialService.getCredentialsByUserIdTypeAndFormat(
                processId, "userId123", "unsupported-scope", "jwt_vc_json"))
                .thenReturn(Mono.error(new VpFormatsNotSupportedException("At least one credential format is not supported by the wallet")));

        // Act & Assert
        StepVerifier.create(attestationExchangeServiceFacade.processAuthorizationRequest(processId, authorizationToken, qrContent))
                .expectErrorMatches(throwable ->
                        throwable instanceof VpFormatsNotSupportedException &&
                                throwable.getMessage().equals("At least one credential format is not supported by the wallet")
                )
                .verify();
    }
}
