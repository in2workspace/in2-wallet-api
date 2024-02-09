//package es.in2.wallet.api.facade.impl;
//
//import es.in2.wallet.api.facade.AttestationExchangeServiceFacade;
//import es.in2.wallet.api.model.*;
//import es.in2.wallet.api.service.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AttestationExchangeServiceFacadeImpl implements AttestationExchangeServiceFacade {
//
//    private final AuthorizationRequestService authorizationRequestService;
//    private final AuthorizationResponseService authorizationResponseService;
//    private final VerifierValidationService verifierValidationService;
//    private final PresentationService presentationService;
//    private final UserDataService userDataService;
//    private final
//
//    @Override
//    public Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent) {
//        log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
//        // Get Authorization Request executing the VC Login Request
//        return authorizationRequestService.getAuthorizationRequestFromVcLoginRequest(processId, qrContent)
//                // Validate the Verifier which issues the Authorization Request
//                .flatMap(jwtAuthorizationRequest ->
//                        verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest)
//                )
//                // Get the Authorization Request from the JWT Authorization Request Claim
//                .flatMap(jwtAuthorizationRequest ->
//                        authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestClaim(processId, jwtAuthorizationRequest))
//                // Check which Verifiable Credentials are selectable
//                .flatMap(authorizationRequest -> {
//                    SelectableVCsRequest selectableVCsRequest = SelectableVCsRequest.builder().vcTypes(authorizationRequest.scope()).build();
//                    return userDataService.getSelectableVCsByVcTypeList(selectableVCsRequest.vcTypes(), authorizationToken)// Build the SelectableVCsRequest
//                            .flatMap(selectableVCs -> buildSelectableVCsRequest(authorizationRequest,selectableVCs));
//                });
//    }
//
//    private Mono<VcSelectorRequest> buildSelectableVCsRequest(AuthorizationRequest authorizationRequest, List<CredentialsBasicInfo> selectableVCs) {
//        return Mono.fromCallable(() -> VcSelectorRequest.builder()
//                .redirectUri(authorizationRequest.redirectUri())
//                .state(authorizationRequest.state())
//                .selectableVcList(selectableVCs)
//                .build());
//    }
//
//
//    @Override
//    public Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
//        // Get the Verifiable Credentials which will be used for the Presentation from the Wallet Data Service
//        return presentationService.createSignedVerifiablePresentation(processId, authorizationToken, vcSelectorResponse)
//                // Build the Authentication Response
//                // todo: refactor to separate build and post
//                // Send the Authentication Response to the Verifier
//                .flatMap(verifiablePresentation ->
//                        authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation))
//                .then();
//    }
//}
