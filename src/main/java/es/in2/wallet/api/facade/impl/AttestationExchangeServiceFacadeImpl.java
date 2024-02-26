package es.in2.wallet.api.facade.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.facade.AttestationExchangeServiceFacade;
import es.in2.wallet.api.model.AuthorizationRequest;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.model.VcSelectorRequest;
import es.in2.wallet.api.model.VcSelectorResponse;
import es.in2.wallet.api.service.*;
import es.in2.wallet.broker.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationExchangeServiceFacadeImpl implements AttestationExchangeServiceFacade {

    private final AuthorizationRequestService authorizationRequestService;
    private final AuthorizationResponseService authorizationResponseService;
    private final VerifierValidationService verifierValidationService;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PresentationService presentationService;

    @Override
    public Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent) {
        log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
        // Get Authorization Request executing the VC Login Request
        return authorizationRequestService.getAuthorizationRequestFromVcLoginRequest(processId, qrContent)
                // Validate the Verifier which issues the Authorization Request
                .flatMap(jwtAuthorizationRequest ->
                        verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest)
                )
                // Get the Authorization Request from the JWT Authorization Request Claim
                .flatMap(jwtAuthorizationRequest ->
                        authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestClaim(processId, jwtAuthorizationRequest)
                )
                // Check which Verifiable Credentials are selectable
                .flatMap(authorizationRequest -> getUserIdFromToken(authorizationToken)
                    .flatMap(userId -> brokerService.getEntityById(processId, userId)
                            .flatMap(optionalEntity -> optionalEntity
                                    .map(entity ->
                                            userDataService.getSelectableVCsByVcTypeList(authorizationRequest.scope(), entity)
                                                    .flatMap(selectableVCs -> {
                                                        log.debug(selectableVCs.toString());
                                                        return buildSelectableVCsRequest(authorizationRequest, selectableVCs);
                                                    })
                                    )
                                    .orElseGet(() ->
                                            Mono.error(new RuntimeException("Entity not found for provided ID."))
                                    )
                            )
                    )
                );
    }

    private Mono<VcSelectorRequest> buildSelectableVCsRequest(AuthorizationRequest authorizationRequest, List<CredentialsBasicInfo> selectableVCs) {
        return Mono.fromCallable(() -> VcSelectorRequest.builder()
                .redirectUri(authorizationRequest.redirectUri())
                .state(authorizationRequest.state())
                .selectableVcList(selectableVCs)
                .build());
    }

    @Override
    public Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
        // Get the Verifiable Credentials which will be used for the Presentation from the Wallet Data Service
        return
                // Create the Verifiable Presentation
                generateNonce()
                        .flatMap(nonce -> generateAudience()
                                .flatMap(audience -> presentationService.createSignedVerifiablePresentation(processId, authorizationToken, vcSelectorResponse, nonce, audience)
                                )
                        )
                // Build the Authentication Response
                // Send the Authentication Response to the Verifier
                .flatMap(verifiablePresentation ->
                {
                    try {
                        return authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new FailedDeserializingException("Error while deserializing Credential: " + e));
                    }
                })
                .then();
    }

    private static Mono<String> generateNonce() {
        return Mono.fromCallable(() -> {
            UUID randomUUID = UUID.randomUUID();
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
            byteBuffer.putLong(randomUUID.getMostSignificantBits());
            byteBuffer.putLong(randomUUID.getLeastSignificantBits());
            byte[] uuidBytes = byteBuffer.array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);
        });
    }

    private static Mono<String> generateAudience() {
        return Mono.just("vpWeb");
    }

}
