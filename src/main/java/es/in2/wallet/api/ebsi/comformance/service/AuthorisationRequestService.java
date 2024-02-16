package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface AuthorisationRequestService {
    Mono<Tuple2<String, String>> getRequestWithOurGeneratedCodeVerifier(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata, String did);
}
