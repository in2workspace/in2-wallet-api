package es.in2.wallet.api.facade.impl;

import es.in2.wallet.api.facade.CredentialIssuanceServiceFacade;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.service.*;
import es.in2.wallet.broker.service.BrokerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.api.util.MessageUtils.*;

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
    private Mono<Void> getCredentialWithPreAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                .flatMap(tokenResponse -> keyGenerationService.generateES256r1ECKeyPair()
                        .flatMap(didKeyGeneratorService::generateDidKeyFromKeyPair)
                        .flatMap(map -> vaultService.saveSecret(map).thenReturn(map))
                        .flatMap(map -> vaultService.getSecretByKey(map.get("did"), PRIVATE_KEY_TYPE)
                                .flatMap(privateKey -> proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer())
                                        .flatMap(json -> signerService.buildJWTSFromJsonNode(json, map.get("did"), "proof", privateKey))
                                )
                                .flatMap(signedJWT -> credentialService.getCredential(processId, signedJWT, tokenResponse,credentialIssuerMetadata)
                                        .flatMap(credentialResponse -> getUserIdFromToken(authorizationToken)
                                                .flatMap(userId -> brokerService.getEntityById(processId,userId)
                                                        .flatMap(entity -> userDataService.saveVC(entity, credentialResponse.credential())
                                                                .flatMap(updatedEntity -> brokerService.updateEntity(processId, userId, updatedEntity)))
                                                        .switchIfEmpty(
                                                                // Proceso de creación, posteo, y actualización
                                                                userDataService.createUserEntity(userId)
                                                                        .flatMap(createdUserId -> brokerService.postEntity(processId, authorizationToken, createdUserId))
                                                                        // Asegurar que postEntity se complete antes de seguir
                                                                        .then(brokerService.getEntityById(processId, userId))
                                                                        // Continuar con saveVC y updateEntity
                                                                        .flatMap(entity -> userDataService.saveVC(entity, credentialResponse.credential()))
                                                                        .flatMap(updatedEntity -> brokerService.updateEntity(processId, userId, updatedEntity))
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }


    private Mono<Void> getCredentialWithAuthorizedCode(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        return null;
    }


}
