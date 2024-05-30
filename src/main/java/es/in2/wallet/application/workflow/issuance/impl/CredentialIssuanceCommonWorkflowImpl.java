package es.in2.wallet.application.workflow.issuance.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import static es.in2.wallet.domain.util.ApplicationConstants.USER_ENTITY_PREFIX;
import static es.in2.wallet.domain.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceCommonWorkflowImpl implements CredentialIssuanceCommonWorkflow {

    private final CredentialOfferService credentialOfferService;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final PreAuthorizedService preAuthorizedService;
    private final CredentialService credentialService;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final ProofJWTService proofJWTService;
    private final SignerService signerService;
    private final BrokerService brokerService;
    private final DataService dataService;
    private final EbsiIdTokenService ebsiIdTokenService;
    private final EbsiVpTokenService ebsiVpTokenService;
    private final EbsiAuthorisationService ebsiAuthorisationService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent) {
        // get Credential Offer
        return credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)
                //get Issuer Server Metadata
                .flatMap(credentialOffer -> credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)
                        //get Authorisation Server Metadata
                        .flatMap(credentialIssuerMetadata -> authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId,credentialIssuerMetadata)
                                .flatMap(authorisationServerMetadata -> {
                                    if (credentialOffer.credentialConfigurationsIds() != null){
                                        return getCredentialWithPreAuthorizedCodeDomeProfile(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                    else if (credentialOffer.grant().preAuthorizedCodeGrant() != null){
                                        return getCredentialWithPreAuthorizedCode(processId,authorizationToken,credentialOffer,authorisationServerMetadata, credentialIssuerMetadata);
                                    }
                                    else {
                                        return getCredentialWithAuthorizedCode(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                })));

    }

    /**
     * Orchestrates the flow to obtain a credential with a pre-authorized code.
     * 1. Obtains a pre-authorized token.
     * 2. Generates and saves a key pair.
     * 3. Builds and signs a credential request.
     * 4. Retrieves the credential.
     * 5. Processes the user entity based on the obtained credential and DID.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        log.info("ProcessId: {} - Getting Credential with Pre-Authorized Code", processId);
        return generateDid().flatMap(did ->
                getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                        .flatMapMany(tokenResponse -> Flux.fromIterable(credentialOffer.credentials())
                                .concatMap(credential -> getCredential(processId, authorizationToken, tokenResponse, credentialIssuerMetadata, did, tokenResponse.cNonce(), credential))
                        )
                        .then());
    }

    /**
     * Orchestrates the flow to obtain a credential with a pre-authorized code.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCodeDomeProfile(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        log.info("ProcessId: {} - Getting Dome Profile Credential with Pre-Authorized Code", processId);
        return generateDid().flatMap(did ->
                getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                .flatMap(tokenResponse -> retrieveCredentialFormatFromCredentialIssuerMetadataByCredentialConfigurationId(credentialOffer.credentialConfigurationsIds().get(0),credentialIssuerMetadata)
                        .flatMap( format -> buildAndSignCredentialRequest(tokenResponse.cNonce(), did, credentialIssuerMetadata.credentialIssuer())
                                .flatMap(jwt -> credentialService.getCredential(jwt,tokenResponse,credentialIssuerMetadata,format,null))
                                .flatMap(credentialResponse -> persistTransactionIdAndProcessUserEntityForDomeProfile(processId,authorizationToken,credentialResponse,tokenResponse,credentialIssuerMetadata))
                )));
    }

    /**
     * Handles the credential acquisition flow using an authorization code grant.
     * This method is selected when the credential offer does not include a pre-authorized code grant,
     * requiring the user to go through an authorization code flow to obtain the credential.
     */
    private Mono<Void> getCredentialWithAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return generateDid()
                .flatMap(did -> ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if ("id_token".equals(responseType)) {
                                        return ebsiIdTokenService.getIdTokenResponse(processId, did, authorisationServerMetadata, tuple.getT1());
                                    } else if ("vp_token".equals(responseType)) {
                                        return ebsiVpTokenService.getVpRequest(processId, authorizationToken, authorisationServerMetadata, tuple.getT1());
                                    } else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                                .flatMap(params -> ebsiAuthorisationService.sendTokenRequest(tuple.getT2(), did, authorisationServerMetadata, params)))
                        // get Credentials
                        .flatMapMany(tokenResponse -> Flux.fromIterable(credentialOffer.credentials())
                                .concatMap(credential -> getCredential(processId, authorizationToken, tokenResponse, credentialIssuerMetadata, did, tokenResponse.cNonce(), credential))
                        )
                        .then());
    }

    /**
     * Retrieves a pre-authorized token from the authorization server.
     * This token is used in subsequent requests to authenticate and authorize operations.
     */
    private Mono<TokenResponse> getPreAuthorizedToken(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, String authorizationToken) {
        return preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken);
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
        return proofJWTService.buildCredentialRequest(nonce, issuer,did)
                .flatMap(json -> signerService.buildJWTSFromJsonNode(json, did, "proof"));
    }

    /**
     * Processes the user entity based on the credential response.
     * If the user entity exists, it is updated with the new credential.
     * If not, a new user entity is created and then updated with the credential.
     */
    private Mono<Void> saveCredential(String processId, String authorizationToken, CredentialResponse credentialResponse) {
        log.info("ProcessId: {} - Processing User Entity", processId);
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId, USER_ENTITY_PREFIX + userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> persistCredential(processId, userId, credentialResponse))
                                .orElseGet(() -> createUserEntity(processId, userId)
                                        .then(persistCredential(processId,userId,credentialResponse)))
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the DID, updating the entity, retrieving the updated entity, saving the VC, and finally updating the entity again with the VC information.
     */
    private Mono<Void> persistCredential(String processId, String userId, CredentialResponse credentialResponse) {
        log.info("ProcessId: {} - Updating User Entity", processId);
        return Mono.defer(() -> {
                    // Check if transactionId is present and choose the appropriate method to save the VC
                    if (credentialResponse.transactionId() == null) {
                        return dataService.saveVC(processId,userId, credentialResponse);
                    } else {
                        return dataService.saveDOMEUnsignedCredential(userId, credentialResponse.credential());
                    }
                })
                .flatMap(credentialEntity -> brokerService.postEntity(processId, credentialEntity))
                .then();
    }


    /**
     * Handles the creation of a new user entity if it does not exist.
     * This involves creating the user, posting the entity, saving the DID to the entity, updating the entity with the DID, retrieving the updated entity, saving the VC, and performing a final update with the VC information.
     */
    private Mono<Void> createUserEntity(String processId, String userId) {
        log.info("ProcessId: {} - Creating and Updating User Entity", processId);
        return dataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId));
    }


    private Mono<String> getCredential(String processId, String authorizationToken, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String did, String nonce, CredentialOffer.Credential credential) {
        return Mono.defer(() -> buildAndSignCredentialRequest(nonce, did, credentialIssuerMetadata.credentialIssuer())
                .flatMap(jwt -> credentialService.getCredential(jwt, tokenResponse, credentialIssuerMetadata, credential.format(), credential.types()))
                .flatMap(credentialResponse -> {
                    String newNonce = credentialResponse.c_nonce() != null ? credentialResponse.c_nonce() : nonce;
                    return saveCredential(processId, authorizationToken, credentialResponse)
                            .thenReturn(newNonce);  // Return the new nonce for the next iteration
                }))
                .onErrorResume(e -> {
                    log.error("Error while getting the credential at index {}", e.getMessage());
                    return Mono.empty(); // Continue with next credential even in case of error
                });
    }

    private Mono<String> retrieveCredentialFormatFromCredentialIssuerMetadataByCredentialConfigurationId(String credentialConfigurationId, CredentialIssuerMetadata credentialIssuerMetadata){
        return Mono.justOrEmpty(credentialIssuerMetadata.credentialsConfigurationsSupported())
                .map(configurationsSupported -> configurationsSupported.get(credentialConfigurationId))
                .map(CredentialIssuerMetadata.CredentialsConfigurationsSupported::format)
                .switchIfEmpty(Mono.error(new NoSuchElementException("No configuration found for ID: " + credentialConfigurationId)));
    }

    private Mono<Void> persistTransactionIdAndProcessUserEntityForDomeProfile(String processId, String authorizationToken, CredentialResponse credential, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata) {
        return saveCredential(processId, authorizationToken, credential)
                .then(Mono.defer(() -> {
                    try {
                        JsonNode credentialJson = objectMapper.readTree(credential.credential());
                        String credentialId = credentialJson.get("id").asText();
                        return dataService.saveTransaction(credentialId, credential.transactionId(), tokenResponse.accessToken(), credentialIssuerMetadata.deferredCredentialEndpoint());
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing credential for transaction saving", e);
                        return Mono.error(new RuntimeException("Failed to deserialize credential JSON", e));
                    }
                }))
                .flatMap(transactionEntity -> brokerService.postEntity(processId, transactionEntity));
    }

}
