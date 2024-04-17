//package es.in2.wallet.api.service;
//
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import es.in2.wallet.application.port.BrokerService;
//import es.in2.wallet.domain.model.AuthorizationRequest;
//import es.in2.wallet.domain.model.CredentialsBasicInfo;
//import es.in2.wallet.domain.model.VcSelectorRequest;
//import es.in2.wallet.domain.service.UserDataService;
//import es.in2.wallet.domain.service.impl.DomeVpTokenServiceImpl;
//import es.in2.wallet.domain.util.ApplicationUtils;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.ZonedDateTime;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
//import static es.in2.wallet.domain.util.MessageUtils.VC_JWT;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DomeVpTokenServiceImplTest {
//
//    @Mock
//    private UserDataService userDataService;
//    @Mock
//    private BrokerService brokerService;
//
//    @InjectMocks
//    private DomeVpTokenServiceImpl domeVpTokenService;
//
//    @Test
//    void getVpRequestShouldReturnVcSelectorRequest() {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//            String authorizationToken = "authToken";
//            ZonedDateTime expirationDate = ZonedDateTime.now().plusDays(30);
//            AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
//            when(authorizationRequest.scope()).thenReturn(Arrays.asList("scope1", "scope2"));
//            when(authorizationRequest.redirectUri()).thenReturn("https://redirectUri.com");
//            when(authorizationRequest.state()).thenReturn("state123");
//
//            String userId = "userId";
//            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userId));
//
//            String userEntity = "userEntityId";
//            when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(Optional.of(userEntity)));
//
//            // Ajusta aquí para usar CredentialsBasicInfo
//            List<CredentialsBasicInfo> selectableVCs = List.of(
//                    new CredentialsBasicInfo("vcId1", List.of("vcType1"),List.of(VC_JWT),JsonNodeFactory.instance.objectNode().put("example", "data"), expirationDate)
//            );
//            when(userDataService.getSelectableVCsByVcTypeList(anyList(), eq(userEntity))).thenReturn(Mono.just(selectableVCs));
//
//            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder()
//                    .selectableVcList(selectableVCs)
//                    .redirectUri("https://redirectUri.com")
//                    .state("state123")
//                    .build();
//
//            StepVerifier.create(domeVpTokenService.getVpRequest(processId, authorizationToken, authorizationRequest))
//                    .expectNextMatches(vcSelectorRequest ->
//                            vcSelectorRequest.selectableVcList().equals(expectedVcSelectorRequest.selectableVcList()) &&
//                                    vcSelectorRequest.redirectUri().equals(expectedVcSelectorRequest.redirectUri()) &&
//                                    vcSelectorRequest.state().equals(expectedVcSelectorRequest.state())
//                    )
//                    .verifyComplete();
//
//            verify(userDataService).getSelectableVCsByVcTypeList(anyList(), eq(userEntity));
//            verify(brokerService).getEntityById(processId, userId);
//    }
//    }
//    @Test
//    void getVpRequestShouldReturnVcSelectorRequestWithDomeScope() {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//            String authorizationToken = "authToken";
//            ZonedDateTime expirationDate = ZonedDateTime.now().plusDays(30);
//            AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class);
//            when(authorizationRequest.scope()).thenReturn(Arrays.asList("didRead", "defaultScope"));
//            when(authorizationRequest.redirectUri()).thenReturn("https://redirectUri.com");
//            when(authorizationRequest.state()).thenReturn("state123");
//
//            String userId = "userId";
//            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userId));
//
//            String userEntity = "userEntityId";
//            when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(Optional.of(userEntity)));
//
//            // Ajusta aquí para usar CredentialsBasicInfo
//            List<CredentialsBasicInfo> selectableVCs = List.of(
//                    new CredentialsBasicInfo("vcId1", List.of("vcType1"),List.of(VC_JWT) ,JsonNodeFactory.instance.objectNode().put("example", "data"), expirationDate)
//            );
//            when(userDataService.getSelectableVCsByVcTypeList(anyList(), eq(userEntity))).thenReturn(Mono.just(selectableVCs));
//
//            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder()
//                    .selectableVcList(selectableVCs)
//                    .redirectUri("https://redirectUri.com")
//                    .state("state123")
//                    .build();
//
//            StepVerifier.create(domeVpTokenService.getVpRequest(processId, authorizationToken, authorizationRequest))
//                    .expectNextMatches(vcSelectorRequest ->
//                            vcSelectorRequest.selectableVcList().equals(expectedVcSelectorRequest.selectableVcList()) &&
//                                    vcSelectorRequest.redirectUri().equals(expectedVcSelectorRequest.redirectUri()) &&
//                                    vcSelectorRequest.state().equals(expectedVcSelectorRequest.state())
//                    )
//                    .verifyComplete();
//
//            verify(userDataService).getSelectableVCsByVcTypeList(anyList(), eq(userEntity));
//            verify(brokerService).getEntityById(processId, userId);
//        }
//    }
//}
//
