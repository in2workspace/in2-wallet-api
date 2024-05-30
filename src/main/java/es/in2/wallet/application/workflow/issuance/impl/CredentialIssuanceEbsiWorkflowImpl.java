package es.in2.wallet.application.workflow.issuance.impl;

import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.*;
import es.in2.wallet.infrastructure.ebsi.config.EbsiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.util.ApplicationConstants.USER_ENTITY_PREFIX;
import static es.in2.wallet.domain.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialIssuanceEbsiWorkflowImpl implements CredentialIssuanceEbsiWorkflow {

    private final CredentialOfferService credentialOfferService;
    private final EbsiConfig ebsiConfig;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final CredentialService credentialService;
    private final DataService dataService;
    private final BrokerService brokerService;
    private final PreAuthorizedService preAuthorizedService;
    private final EbsiIdTokenService ebsiIdTokenService;
    private final EbsiVpTokenService ebsiVpTokenService;
    private final ProofJWTService proofJWTService;
    private final EbsiAuthorisationService ebsiAuthorisationService;
    private final SignerService signerService;


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
        return dataService.saveVC(processId,userId, credentialResponse)
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
                    .flatMap(credentialResponse -> saveCredential(processId,authorizationToken,credentialResponse));
    }

}
