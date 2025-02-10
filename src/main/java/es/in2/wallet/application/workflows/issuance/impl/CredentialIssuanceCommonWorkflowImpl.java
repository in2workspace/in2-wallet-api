package es.in2.wallet.application.workflows.issuance.impl;

import es.in2.wallet.application.dto.*;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.domain.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import static es.in2.wallet.domain.utils.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.utils.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceCommonWorkflowImpl implements CredentialIssuanceCommonWorkflow {

    private final CredentialOfferService credentialOfferService;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final PreAuthorizedService preAuthorizedService;
    private final OID4VCICredentialService OID4VCICredentialService;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final ProofJWTService proofJWTService;
    private final SignerService signerService;
    private final EbsiIdTokenService ebsiIdTokenService;
    private final EbsiVpTokenService ebsiVpTokenService;
    private final EbsiAuthorisationService ebsiAuthorisationService;
    private final UserService userService;
    private final CredentialService credentialService;
    private final DeferredCredentialMetadataService deferredCredentialMetadataService;


    @Override
    public Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent) {
        // get Credential Offer
        return credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)
                //get Issuer Server Metadata
                .flatMap(credentialOffer -> credentialIssuerMetadataService
                        .getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)
                        //get Authorisation Server Metadata
                        .flatMap(credentialIssuerMetadata -> authorisationServerMetadataService
                                .getAuthorizationServerMetadataFromCredentialIssuerMetadata(
                                        processId,
                                        credentialIssuerMetadata
                                )
                                .flatMap(authorisationServerMetadata -> {
                                    if (credentialOffer.credentialConfigurationsIds() != null) {
                                        return getCredentialWithPreAuthorizedCodeDomeProfile(
                                                processId,
                                                authorizationToken,
                                                credentialOffer,
                                                authorisationServerMetadata,
                                                credentialIssuerMetadata);
                                    } else if (credentialOffer.grant().preAuthorizedCodeGrant() != null) {
                                        return getCredentialWithPreAuthorizedCode(
                                                processId,
                                                authorizationToken,
                                                credentialOffer,
                                                authorisationServerMetadata,
                                                credentialIssuerMetadata);
                                    } else {
                                        return getCredentialWithAuthorizedCode(
                                                processId,
                                                authorizationToken,
                                                credentialOffer,
                                                authorisationServerMetadata,
                                                credentialIssuerMetadata);
                                    }
                                })));
    }

    /**
     * Orchestrates the flow to get a credential with a pre-authorized code.
     * 1. Get a pre-authorized token.
     * 2. Generates and saves a key pair.
     * 3. Build and sign a credential request.
     * 4. Retrieves the credential.
     * 5. Processes the user entity based on the obtained credential and DID.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        log.info("ProcessId: {} - Getting Credential with Pre-Authorized Code", processId);
        return generateDid().flatMap(did ->
                getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                        .flatMapMany(tokenResponse -> Flux.fromIterable(credentialOffer.credentials())
                                .concatMap(credential -> getCredential(
                                        processId,
                                        authorizationToken,
                                        tokenResponse,
                                        credentialIssuerMetadata,
                                        did,
                                        tokenResponse.cNonce(),
                                        credential))
                        )
                        .then());
    }

    /**
     * Orchestrates the flow to get a credential with a pre-authorized code.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCodeDomeProfile(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        log.info("ProcessId: {} - Getting Dome Profile Credential with Pre-Authorized Code", processId);
        return generateDid().flatMap(did ->
                getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                        .flatMap(tokenResponse -> retrieveCredentialFormatFromCredentialIssuerMetadataByCredentialConfigurationId(credentialOffer.credentialConfigurationsIds().get(0),credentialIssuerMetadata)
                                .flatMap( format -> buildAndSignCredentialRequest(tokenResponse.cNonce(), did, credentialIssuerMetadata.credentialIssuer())
                                        .flatMap(jwt -> OID4VCICredentialService.getCredential(jwt,tokenResponse,credentialIssuerMetadata,format,null))
                                        .flatMap(credentialResponseWithStatus -> handleCredentialResponse(processId, credentialResponseWithStatus, authorizationToken,tokenResponse,credentialIssuerMetadata, format))
                                )));
    }

    /**
     * Handles the credential acquisition flow using an authorization code grant.
     * This method is selected when the credential offer does not include a pre-authorized code grant,
     * requiring the user to go through an authorization code flow to get the credential.
     */
    private Mono<Void> getCredentialWithAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return generateDid()
                .flatMap(did -> ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(
                                processId,
                                credentialOffer,
                                authorisationServerMetadata,
                                credentialIssuerMetadata,
                                did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if ("id_token".equals(responseType)) {
                                        return ebsiIdTokenService.getIdTokenResponse(
                                                processId,
                                                did,
                                                authorisationServerMetadata,
                                                tuple.getT1());
                                    } else if ("vp_token".equals(responseType)) {
                                        return ebsiVpTokenService.getVpRequest(
                                                processId,
                                                authorizationToken,
                                                authorisationServerMetadata,
                                                tuple.getT1());
                                    } else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                                .flatMap(params -> ebsiAuthorisationService.sendTokenRequest(
                                        tuple.getT2(),
                                        did,
                                        authorisationServerMetadata,
                                        params)))
                        // get Credentials
                        .flatMapMany(tokenResponse -> Flux.fromIterable(credentialOffer.credentials())
                                .concatMap(credential -> getCredential(
                                        processId,
                                        authorizationToken,
                                        tokenResponse,
                                        credentialIssuerMetadata,
                                        did,
                                        tokenResponse.cNonce(),
                                        credential))
                        )
                        .then());
    }

    /**
     * Retrieves a pre-authorized token from the authorization server.
     * This token is used in later requests to authenticate and authorize operations.
     */
    private Mono<TokenResponse> getPreAuthorizedToken(String processId, CredentialOffer credentialOffer,
                                                      AuthorisationServerMetadata authorisationServerMetadata,
                                                      String authorizationToken) {
        return preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata,
                authorizationToken);
    }

    /**
     * Generates a new ES256r1 EC key pair for signing requests.
     * The generated key pair is then saved in a vault for secure storage and later retrieval.
     * The method returns a map containing key pair details, including the DID.
     */
    private Mono<String> generateDid() {
        return didKeyGeneratorService.generateDidKey();
    }

    /**
     * Constructs a credential request using the nonce from the token response and the issuer's information.
     * The request is then signed using the generated DID and private key to ensure its authenticity.
     */
    private Mono<String> buildAndSignCredentialRequest(String nonce, String did, String issuer) {
        return proofJWTService.buildCredentialRequest(nonce, issuer, did)
                .flatMap(json -> signerService.buildJWTSFromJsonNode(json, did, "proof"));
    }


    private Mono<String> getCredential(String processId, String authorizationToken, TokenResponse tokenResponse,
                                       CredentialIssuerMetadata credentialIssuerMetadata, String did, String nonce,
                                       CredentialOffer.Credential credential) {
        return Mono.defer(() -> buildAndSignCredentialRequest(nonce, did, credentialIssuerMetadata.credentialIssuer())
                        .flatMap(jwt -> OID4VCICredentialService.getCredential(jwt,
                                tokenResponse,
                                credentialIssuerMetadata,
                                credential.format(),
                                credential.types()))
                        .flatMap(credentialResponseWithStatus -> {
                            String newNonce = credentialResponseWithStatus.credentialResponse().c_nonce() != null ? credentialResponseWithStatus.credentialResponse().c_nonce() : nonce;
                            return handleCredentialResponse(processId, credentialResponseWithStatus, authorizationToken, tokenResponse, credentialIssuerMetadata, credential.format())
                                    .thenReturn(newNonce);  // Return the new nonce for the next iteration
                        }))
                .onErrorResume(e -> {
                    log.error("Error while getting the credential at index {}", e.getMessage());
                    return Mono.empty(); // Continue with next credential even in case of error
                });
    }

    private Mono<String> retrieveCredentialFormatFromCredentialIssuerMetadataByCredentialConfigurationId(
            String credentialConfigurationId, CredentialIssuerMetadata credentialIssuerMetadata) {
        return Mono.justOrEmpty(credentialIssuerMetadata.credentialsConfigurationsSupported())
                .map(configurationsSupported -> configurationsSupported.get(credentialConfigurationId))
                .map(CredentialIssuerMetadata.CredentialsConfigurationsSupported::format)
                .switchIfEmpty(Mono.error(new NoSuchElementException("No configuration found for ID: " + credentialConfigurationId)));
    }

    private Mono<Void> handleCredentialResponse(
            String processId,
            CredentialResponseWithStatus credentialResponseWithStatus,
            String authorizationToken,
            TokenResponse tokenResponse,
            CredentialIssuerMetadata credentialIssuerMetadata,
            String format
    ) {
        return getUserIdFromToken(authorizationToken)
                // Store the user
                .flatMap(userId -> userService.storeUser(processId, userId))
                .doOnNext(userUuid ->
                        log.info("ProcessID: {} - Stored userUuid: {}", processId, userUuid.toString())
                )
                // Save the credential
                .flatMap(userUuid -> credentialService.saveCredential(
                        processId,
                        userUuid,
                        credentialResponseWithStatus.credentialResponse(),
                        format
                ))
                .doOnNext(credentialUuid ->
                        log.info("ProcessID: {} - Saved credentialUuid: {}", processId, credentialUuid.toString())
                )
                // If status is ACCEPTED, save deferred metadata; otherwise, skip
                .flatMap(credentialUuid -> {
                    if (credentialResponseWithStatus.statusCode().equals(HttpStatus.ACCEPTED)) {
                        log.info("ProcessID: {} - Status ACCEPTED, saving deferred credential metadata", processId);

                        return deferredCredentialMetadataService.saveDeferredCredentialMetadata(
                                        processId,
                                        credentialUuid,
                                        credentialResponseWithStatus.credentialResponse().transactionId(),
                                        tokenResponse.accessToken(),
                                        credentialIssuerMetadata.deferredCredentialEndpoint()
                                )
                                .doOnNext(deferredUuid ->
                                        log.info("ProcessID: {} - Deferred credential metadata saved with UUID: {}", processId, deferredUuid.toString())
                                )
                                .then();
                    } else {
                        log.info("ProcessID: {} - Status is {}, skipping deferred metadata",
                                processId, credentialResponseWithStatus.statusCode());
                        return Mono.empty();
                    }
                })
                .doOnError(error ->
                        log.error("ProcessID: {} - handleCredentialResponse error: {}",
                                processId, error.getMessage(), error)
                )
                .then();
    }
}
