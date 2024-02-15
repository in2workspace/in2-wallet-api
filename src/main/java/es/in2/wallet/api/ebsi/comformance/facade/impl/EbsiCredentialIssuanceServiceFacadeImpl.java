package es.in2.wallet.api.ebsi.comformance.facade.impl;

import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.ebsi.comformance.configuration.EbsiConfig;
import es.in2.wallet.api.ebsi.comformance.facade.EbsiCredentialIssuanceServiceFacade;
import es.in2.wallet.api.ebsi.comformance.service.AuthorisationRequestService;
import es.in2.wallet.api.ebsi.comformance.service.CredentialEbsiService;
import es.in2.wallet.api.ebsi.comformance.service.IdTokenService;
import es.in2.wallet.api.ebsi.comformance.service.VpTokenService;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.service.*;
import es.in2.wallet.broker.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.api.util.MessageUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiCredentialIssuanceServiceFacadeImpl implements EbsiCredentialIssuanceServiceFacade {

    private final CredentialOfferService credentialOfferService;
    private final EbsiConfig ebsiConfig;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final CredentialEbsiService credentialEbsiService;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PreAuthorizedService preAuthorizedService;
    private final IdTokenService idTokenService;
    private final VpTokenService vpTokenService;
    private final AuthorisationRequestService authorisationRequestService;

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
                                })));

    }

    private Mono<Void> getCredentialWithPreAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        return preAuthorizedService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,authorizationToken)
                // get Credential
                .flatMap(tokenResponse -> credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata,authorizationToken,credentialOffer.credentials().get(0).format(),credentialOffer.credentials().get(0).types()))
                // save Credential
                .flatMap(credentialResponse -> ebsiConfig.getDid()
                        .flatMap(did -> processUserEntity(processId,authorizationToken,credentialResponse,did)));
    }

    private Mono<Void> getCredentialWithAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        return  ebsiConfig.getDid()
                .flatMap(did -> authorisationRequestService.getRequestWithOurGeneratedCodeVerifier(processId,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata,did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if (responseType.equals("id_token")){
                                        return idTokenService.getTokenResponse(processId,authorisationServerMetadata,did,tuple);
                                    }
                                    else if (responseType.equals("vp_token")){
                                       return vpTokenService.getVpRequest(processId,authorizationToken,authorisationServerMetadata,did,tuple);
                                    }
                                    else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                        )
                )
                // get Credential
                .flatMap(tokenResponse -> credentialEbsiService.getCredential(processId, tokenResponse, credentialIssuerMetadata,authorizationToken,credentialOffer.credentials().get(0).format(),credentialOffer.credentials().get(0).types()))
                // save Credential
                .flatMap(credentialResponse -> ebsiConfig.getDid()
                        .flatMap(did -> processUserEntity(processId,authorizationToken,credentialResponse,did)));
    }
    private Mono<String> extractResponseType(String jwt){
        return Mono.fromCallable(() -> {
            log.debug(jwt);
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            return signedJwt.getJWTClaimsSet().getClaim("response_type").toString();
        });
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
                                .map(entity -> updateEntity(processId, userId, credentialResponse, entity))
                                .orElseGet(() -> createAndUpdateUser(processId, userId, credentialResponse))
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the DID, updating the entity, retrieving the updated entity, saving the VC, and finally updating the entity again with the VC information.
     */
    private Mono<Void> updateEntity(String processId, String userId, CredentialResponse credentialResponse, String entity) {
        return userDataService.saveVC(entity, credentialResponse.credential())
                .flatMap(updatedEntity ->
                        brokerService.updateEntity(processId, userId, updatedEntity)
                );
    }

    /**
     * Handles the creation of a new user entity if it does not exist.
     * After creation, the entity is updated with the DID information.
     * This involves creating the user, posting the entity, saving the DID to the entity, updating the entity with the DID, retrieving the updated entity, saving the VC, and performing a final update with the VC information.
     */
    private Mono<Void> createAndUpdateUser(String processId, String userId, CredentialResponse credentialResponse) {
        return userDataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                .then(brokerService.getEntityById(processId, userId))
                .flatMap(optionalEntity ->
                        optionalEntity.map(entity ->
                                        userDataService.saveVC(entity, credentialResponse.credential())
                                                .flatMap(updatedEntity -> brokerService.updateEntity(processId, userId, updatedEntity))
                                )
                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation.")))
                );
    }

}
