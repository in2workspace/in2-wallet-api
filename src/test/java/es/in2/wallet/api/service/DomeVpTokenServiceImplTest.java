package es.in2.wallet.api.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import es.in2.wallet.application.workflow.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.model.CredentialStatus;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.service.impl.DomeVpTokenServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.DEFAULT_VC_TYPES_FOR_DOME_VERIFIER;
import static es.in2.wallet.domain.util.ApplicationConstants.VC_JWT;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomeVpTokenServiceImplTest {

    @Mock
    private AttestationExchangeCommonWorkflow attestationExchangeCommonWorkflow;

    @InjectMocks
    private DomeVpTokenServiceImpl domeVpTokenService;

    @Test
    void getVpRequestShouldReturnVcSelectorRequest() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            String authorizationToken = "authToken";
            ZonedDateTime expirationDate = ZonedDateTime.now().plusDays(30);
            AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
            when(authorizationRequest.scope()).thenReturn(Arrays.asList("scope1", "scope2"));
            when(authorizationRequest.redirectUri()).thenReturn("https://redirectUri.com");
            when(authorizationRequest.state()).thenReturn("state123");

            String userId = "userId";
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userId));


            List<CredentialsBasicInfo> selectableVCs = List.of(
                    new CredentialsBasicInfo("vcId1", List.of("vcType1"), CredentialStatus.VALID,List.of(VC_JWT),JsonNodeFactory.instance.objectNode().put("example", "data"), expirationDate)
            );
            when(attestationExchangeCommonWorkflow.getSelectableCredentialsRequiredToBuildThePresentation(processId,authorizationToken,authorizationRequest.scope())).thenReturn(Mono.just(selectableVCs));

            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder()
                    .selectableVcList(selectableVCs)
                    .redirectUri("https://redirectUri.com")
                    .state("state123")
                    .build();

            StepVerifier.create(domeVpTokenService.getVpRequest(processId, authorizationToken, authorizationRequest))
                    .expectNextMatches(vcSelectorRequest ->
                            vcSelectorRequest.selectableVcList().equals(expectedVcSelectorRequest.selectableVcList()) &&
                                    vcSelectorRequest.redirectUri().equals(expectedVcSelectorRequest.redirectUri()) &&
                                    vcSelectorRequest.state().equals(expectedVcSelectorRequest.state())
                    )
                    .verifyComplete();

            verify(attestationExchangeCommonWorkflow).getSelectableCredentialsRequiredToBuildThePresentation(processId,authorizationToken,authorizationRequest.scope());
    }
    }
    @Test
    void getVpRequestShouldReturnVcSelectorRequestWithDomeScope() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            String authorizationToken = "authToken";
            ZonedDateTime expirationDate = ZonedDateTime.now().plusDays(30);
            AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
            when(authorizationRequest.scope()).thenReturn(Arrays.asList("didRead", "defaultScope"));
            when(authorizationRequest.redirectUri()).thenReturn("https://redirectUri.com");
            when(authorizationRequest.state()).thenReturn("state123");

            String userId = "userId";
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userId));

            List<CredentialsBasicInfo> selectableVCs = List.of(
                    new CredentialsBasicInfo("vcId1", List.of("vcType1"), CredentialStatus.VALID,List.of(VC_JWT),JsonNodeFactory.instance.objectNode().put("example", "data"), expirationDate)
            );
            when(attestationExchangeCommonWorkflow.getSelectableCredentialsRequiredToBuildThePresentation(processId,authorizationToken,DEFAULT_VC_TYPES_FOR_DOME_VERIFIER)).thenReturn(Mono.just(selectableVCs));

            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder()
                    .selectableVcList(selectableVCs)
                    .redirectUri("https://redirectUri.com")
                    .state("state123")
                    .build();

            StepVerifier.create(domeVpTokenService.getVpRequest(processId, authorizationToken, authorizationRequest))
                    .expectNextMatches(vcSelectorRequest ->
                            vcSelectorRequest.selectableVcList().equals(expectedVcSelectorRequest.selectableVcList()) &&
                                    vcSelectorRequest.redirectUri().equals(expectedVcSelectorRequest.redirectUri()) &&
                                    vcSelectorRequest.state().equals(expectedVcSelectorRequest.state())
                    )
                    .verifyComplete();

            verify(attestationExchangeCommonWorkflow).getSelectableCredentialsRequiredToBuildThePresentation(processId,authorizationToken,DEFAULT_VC_TYPES_FOR_DOME_VERIFIER);
        }
    }
}

