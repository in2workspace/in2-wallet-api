package es.in2.wallet.api.facade.impl;

import es.in2.wallet.api.facade.CredentialIssuanceServiceFacade;
import es.in2.wallet.api.model.*;
import es.in2.wallet.api.service.*;
import es.in2.wallet.broker.service.BrokerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceServiceFacadeImpl implements CredentialIssuanceServiceFacade {

    private final CredentialOfferService credentialOfferService;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final PreAuthorizedService preAuthorizedService;
    private final CredentialService credentialService;
    private final KeyGenerationService keyGenerationService;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final VaultService vaultService;
    private final ProofJWTService proofJWTService;
    private final SignerService signerService;
    private final BrokerService brokerService;
    private final UserDataService userDataService;

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
        return getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                .flatMap(tokenResponse -> generateAndSaveKeyPair()
                        .flatMap(map -> buildAndSignCredentialRequest(tokenResponse, map, credentialIssuerMetadata)
                                .flatMap(signedJWT -> getCredential(processId, signedJWT, tokenResponse, credentialIssuerMetadata)
                                        .flatMap(credentialResponse -> processUserEntity(processId, authorizationToken, credentialResponse,map.get("did")))
                                )
                        )
                );
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
    private Mono<Map<String, String>> generateAndSaveKeyPair() {
        return keyGenerationService.generateES256r1ECKeyPair()
                .flatMap(didKeyGeneratorService::generateDidKeyFromKeyPair)
                .flatMap(map -> vaultService.saveSecret(map).thenReturn(map));
    }

    /**
     * Constructs a credential request using the nonce from the token response and the issuer's information.
     * The request is then signed using the generated DID and private key to ensure its authenticity.
     */
    private Mono<String> buildAndSignCredentialRequest(TokenResponse tokenResponse, Map<String, String> map, CredentialIssuerMetadata credentialIssuerMetadata) {
        return proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer())
                .flatMap(json -> signerService.buildJWTSFromJsonNode(json, map.get("did"), "proof", map.get("privateKey")));
    }

    /**
     * Retrieves the credential based on the signed JWT, token response, and issuer metadata.
     * This step involves communicating with the credential service to obtain the actual credential.
     */
    private Mono<CredentialResponse> getCredential(String processId, String signedJWT, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata) {
        return credentialService.getCredential(processId, signedJWT, tokenResponse, credentialIssuerMetadata);
    }

    /**
     * Processes the user entity based on the credential response.
     * If the user entity exists, it is updated with the new credential.
     * If not, a new user entity is created and then updated with the credential.
     */
    private Mono<Void> processUserEntity(String processId, String authorizationToken, CredentialResponse credentialResponse, String did) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId, userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> updateEntity(processId, userId, credentialResponse, entity,did)) // Si está presente, actualiza
                                .orElseGet(() -> createAndUpdateUser(processId, authorizationToken, userId, credentialResponse,did)) // Si no, crea y actualiza
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the DID, updating the entity, retrieving the updated entity, saving the VC, and finally updating the entity again with the VC information.
     */
    private Mono<Void> updateEntity(String processId, String userId, CredentialResponse credentialResponse, String entity, String did) {
        return userDataService.saveDid(entity, did, "did:key")
                .flatMap(updatedEntity ->
                        brokerService.updateEntity(processId, userId, updatedEntity)
                )
                .then(
                        brokerService.getEntityById(processId, userId)
                                .flatMap(optionalEntity ->
                                        optionalEntity.map(updatedEntity ->
                                                        userDataService.saveVC(updatedEntity, credentialResponse.credential())
                                                                .flatMap(vcUpdatedEntity ->
                                                                        brokerService.updateEntity(processId, userId, vcUpdatedEntity)
                                                                )
                                        ).orElseGet(() -> Mono.error(new RuntimeException("Failed to retrieve entity after initial update.")))
                                )
                );
    }

    /**
     * Handles the creation of a new user entity if it does not exist.
     * After creation, the entity is updated with the DID information.
     * This involves creating the user, posting the entity, saving the DID to the entity, updating the entity with the DID, retrieving the updated entity, saving the VC, and performing a final update with the VC information.
     */
    private Mono<Void> createAndUpdateUser(String processId, String authorizationToken, String userId, CredentialResponse credentialResponse, String did) {
        return userDataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, authorizationToken, createdUserId))
                .then(brokerService.getEntityById(processId, userId))
                .flatMap(optionalEntity ->
                        optionalEntity.map(entity ->
                                        userDataService.saveDid(entity, did, "did:key")
                                                .flatMap(didUpdatedEntity -> brokerService.updateEntity(processId, userId, didUpdatedEntity))
                                                .then(brokerService.getEntityById(processId, userId))
                                                .flatMap(updatedOptionalEntity ->
                                                        updatedOptionalEntity.map(updatedEntity ->
                                                                        userDataService.saveVC(updatedEntity, credentialResponse.credential())
                                                                                .flatMap(vcUpdatedEntity -> brokerService.updateEntity(processId, userId, vcUpdatedEntity))
                                                                                .then()
                                                                )
                                                                .orElseGet(() -> Mono.error(new RuntimeException("Failed to retrieve entity after update.")))
                                                )
                                )
                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation.")))
                );
    }


    private Mono<Void> getCredentialWithAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        log.debug(processId);
        log.debug(authorizationToken);
        log.debug(credentialOffer.toString());
        log.debug(authorisationServerMetadata.toString());
        log.debug(credentialIssuerMetadata.toString());
        return Mono.error(new RuntimeException("Not implemented yet."));
    }


}
