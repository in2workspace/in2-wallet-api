package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.model.CredentialOffer;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface EbsiAuthorisationRequestService {
    Mono<Tuple2<String, String>> getRequestWithOurGeneratedCodeVerifier(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata, String did);
}
