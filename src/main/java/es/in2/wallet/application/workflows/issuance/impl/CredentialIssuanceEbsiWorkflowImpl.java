package es.in2.wallet.application.workflows.issuance.impl;

import es.in2.wallet.application.dto.*;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.domain.services.*;
import es.in2.wallet.infrastructure.ebsi.config.EbsiConfig;
import es.in2.wallet.infrastructure.services.CredentialRepositoryService;
import es.in2.wallet.infrastructure.services.DeferredCredentialMetadataRepositoryService;
import es.in2.wallet.infrastructure.services.UserRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.utils.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.utils.ApplicationUtils.getUserIdFromToken;
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceEbsiWorkflowImpl implements CredentialIssuanceEbsiWorkflow {

    private final CredentialOfferService credentialOfferService;
    private final EbsiConfig ebsiConfig;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final CredentialService credentialService;
    private final PreAuthorizedService preAuthorizedService;
    private final EbsiIdTokenService ebsiIdTokenService;
    private final EbsiVpTokenService ebsiVpTokenService;
    private final ProofJWTService proofJWTService;
    private final EbsiAuthorisationService ebsiAuthorisationService;
    private final SignerService signerService;
    private final UserRepositoryService userRepositoryService;
    private final CredentialRepositoryService credentialRepositoryService;
    private final DeferredCredentialMetadataRepositoryService deferredCredentialMetadataRepositoryService;


    /**
     * Identifies the authorization method based on the QR content and proceeds with the credential exchange flow.
     * This method orchestrates the flow to obtain credentials by first retrieving the credential offer,
     * then fetching the issuer and authorisation server metadata, and finally obtaining the credential
     * either through a pre-authorized code grant or an authorization code flow.
     */
    @Override
    public Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent) {
        // get Credential Offer
        return credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)
                //get Issuer Server Metadata
                .flatMap(credentialOffer -> credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)
                        //get Authorisation Server Metadata
                        .flatMap(credentialIssuerMetadata -> authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId,credentialIssuerMetadata)
                                .flatMap(authorisationServerMetadata -> {
                                    if (credentialOffer.grant().preAuthorizedCodeGrant() != null){
                                        return getCredentialWithPreAuthorizedCodeEbsi(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                    else {
                                        return getCredentialWithAuthorizedCodeEbsi(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                })
                        )
                );

    }

    /**
     * Handles the credential acquisition flow using a pre-authorized code grant.
     * This method is chosen when the credential offer includes a pre-authorized code grant.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return ebsiConfig.getDid()
                .flatMap(did -> preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                        .flatMap(tokenResponse -> getCredential(
                                processId,authorizationToken,tokenResponse,credentialOffer,credentialIssuerMetadata,did,tokenResponse.cNonce())));
    }


    /**
     * Handles the credential acquisition flow using an authorization code grant.
     * This method is selected when the credential offer does not include a pre-authorized code grant,
     * requiring the user to go through an authorization code flow to obtain the credential.
     */
    private Mono<Void> getCredentialWithAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        return  ebsiConfig.getDid()
                .flatMap(did -> ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata,did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if (responseType.equals("id_token")){
                                        return ebsiIdTokenService.getIdTokenResponse(processId,did,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else if (responseType.equals("vp_token")){
                                       return ebsiVpTokenService.getVpRequest(processId,authorizationToken,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                                .flatMap(params -> ebsiAuthorisationService.sendTokenRequest(tuple.getT2(), did, authorisationServerMetadata,params))
                        )
                        // get Credentials
                        .flatMap(tokenResponse -> getCredential(
                                 processId,authorizationToken,tokenResponse, credentialOffer, credentialIssuerMetadata, did, tokenResponse.cNonce()
                        ))
                );
    }

    /**
     * Constructs a credential request using the nonce from the token response and the issuer's information.
     * The request is then signed using the generated DID and private key to ensure its authenticity.
     */
    private Mono<String> buildAndSignCredentialRequest(String nonce, String did, String issuer) {
        return proofJWTService.buildCredentialRequest(nonce, issuer,did)
                .flatMap(json -> signerService.buildJWTSFromJsonNode(json, did, "proof"));
    }

    private Mono<Void> getCredential(String processId, String authorizationToken, TokenResponse tokenResponse, CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, String did, String nonce) {
            return buildAndSignCredentialRequest(nonce, did, credentialIssuerMetadata.credentialIssuer())
                    .flatMap(jwt -> credentialService.getCredential(jwt, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types()))
                    .flatMap(credentialResponse -> handleCredentialResponse(processId,credentialResponse ,authorizationToken, tokenResponse, credentialIssuerMetadata));
    }

    private Mono<Void> handleCredentialResponse(
            String processId,
            CredentialResponseWithStatus credentialResponseWithStatus,
            String authorizationToken,
            TokenResponse tokenResponse,
            CredentialIssuerMetadata credentialIssuerMetadata
    ) {
        return getUserIdFromToken(authorizationToken)
                // Store the user
                .flatMap(userId -> userRepositoryService.storeUser(processId, userId))
                .doOnNext(userUuid ->
                        log.info("ProcessID: {} - Stored userUuid: {}", processId, userUuid.toString())
                )
                // Save the credential
                .flatMap(userUuid -> credentialRepositoryService.saveCredential(
                        processId,
                        userUuid,
                        credentialResponseWithStatus.credentialResponse()
                ))
                .doOnNext(credentialUuid ->
                        log.info("ProcessID: {} - Saved credentialUuid: {}", processId, credentialUuid.toString())
                )
                // If status is ACCEPTED, save deferred metadata; otherwise, skip
                .flatMap(credentialUuid -> {
                    if (credentialResponseWithStatus.statusCode().equals(HttpStatus.ACCEPTED)) {
                        log.info("ProcessID: {} - Status ACCEPTED, saving deferred credential metadata", processId);

                        return deferredCredentialMetadataRepositoryService.saveDeferredCredentialMetadata(
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
