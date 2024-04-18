package es.in2.wallet.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.service.CredentialIssuanceService;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static es.in2.wallet.domain.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceServiceImpl implements CredentialIssuanceService {

    private final CredentialOfferService credentialOfferService;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final PreAuthorizedService preAuthorizedService;
    private final CredentialService credentialService;
    private final DidKeyGeneratorService didKeyGeneratorService;
    private final ProofJWTService proofJWTService;
    private final SignerService signerService;
    private final BrokerService brokerService;
    private final UserDataService userDataService;
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
                        .flatMap(tokenResponse -> getCredentialRecursive(
                                tokenResponse, credentialOffer, credentialIssuerMetadata, did, tokenResponse.cNonce(), new ArrayList<>(), 0
                        ))
                        .flatMap(credentials -> processUserEntity(processId,authorizationToken,credentials))
                );
    }

    /**
     * Orchestrates the flow to obtain a credential with a pre-authorized code.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCodeDomeProfile(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        log.info("ProcessId: {} - Getting Credential with Pre-Authorized Code", processId);
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
                        .flatMap(tokenResponse -> getCredentialRecursive(
                                 tokenResponse, credentialOffer, credentialIssuerMetadata, did, tokenResponse.cNonce(), new ArrayList<>(), 0
                        ))
                        .flatMap(credentials -> processUserEntity(processId, authorizationToken, credentials)));
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
    private Mono<Void> processUserEntity(String processId, String authorizationToken, List<CredentialResponse> credentials) {
        log.info("ProcessId: {} - Processing User Entity", processId);
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getUserEntityById(processId, userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> persistCredential(processId, userId, credentials))
                                .orElseGet(() -> createUserEntityAndPersistCredential(processId, userId, credentials))
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the DID, updating the entity, retrieving the updated entity, saving the VC, and finally updating the entity again with the VC information.
     */
    private Mono<Void> persistCredential(String processId, String userId, List<CredentialResponse> credentials) {
        log.info("ProcessId: {} - Updating User Entity", processId);
        return userDataService.saveVC(userId, credentials)
                .flatMap(credentialEntity ->
                                brokerService.postEntity(processId, credentialEntity));
    }

    /**
     * Handles the creation of a new user entity if it does not exist.
     * After creation, the entity is updated with the DID information.
     * This involves creating the user, posting the entity, saving the DID to the entity, updating the entity with the DID, retrieving the updated entity, saving the VC, and performing a final update with the VC information.
     */
    private Mono<Void> createUserEntityAndPersistCredential(String processId, String userId, List<CredentialResponse> credentials) {
        log.info("ProcessId: {} - Creating and Updating User Entity", processId);
        return userDataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                .then(userDataService.saveVC(userId, credentials)
                        .flatMap(credentialEntity -> brokerService.postEntity(processId, credentialEntity))
                        .then());
    }

    private Mono<List<CredentialResponse>> getCredentialRecursive(TokenResponse tokenResponse, CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, String did, String nonce, List<CredentialResponse> credentialResponses, int index) {
        if (index >= credentialOffer.credentials().size()) {
            return Mono.just(credentialResponses);
        }
        CredentialOffer.Credential credential = credentialOffer.credentials().get(index);
        return buildAndSignCredentialRequest(nonce, did, credentialIssuerMetadata.credentialIssuer())
                .flatMap(jwt -> credentialService.getCredential(jwt, tokenResponse, credentialIssuerMetadata, credential.format(), credential.types()))
                .flatMap(credentialResponse -> {
                    credentialResponses.add(credentialResponse);
                    String newNonce = credentialResponse.c_nonce() != null ? credentialResponse.c_nonce() : nonce;
                    return getCredentialRecursive(
                             tokenResponse, credentialOffer, credentialIssuerMetadata, did, newNonce, credentialResponses, index + 1
                    );
                });
    }

    private Mono<String> retrieveCredentialFormatFromCredentialIssuerMetadataByCredentialConfigurationId(String credentialConfigurationId, CredentialIssuerMetadata credentialIssuerMetadata){
        return Mono.justOrEmpty(credentialIssuerMetadata.credentialsConfigurationsSupported())
                .map(configurationsSupported -> configurationsSupported.get(credentialConfigurationId))
                .map(CredentialIssuerMetadata.CredentialsConfigurationsSupported::format)
                .switchIfEmpty(Mono.error(new NoSuchElementException("No configuration found for ID: " + credentialConfigurationId)));
    }

    private Mono<Void> persistTransactionIdAndProcessUserEntityForDomeProfile(String processId, String authorizationToken, CredentialResponse credential, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata) {
        return processUserEntity(processId, authorizationToken, List.of(credential))
                .then(Mono.defer(() -> {
                    try {
                        JsonNode credentialJson = objectMapper.readTree(credential.credential().toString());
                        String credentialId = credentialJson.get("id").asText();
                        return userDataService.saveTransaction(credentialId, credential.transactionId(), tokenResponse.accessToken(), credentialIssuerMetadata.deferredCredentialEndpoint());
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing credential for transaction saving", e);
                        return Mono.error(new RuntimeException("Failed to deserialize credential JSON", e));
                    }
                }))
                .flatMap(transactionEntity -> brokerService.postEntity(processId, transactionEntity));
    }


}
