package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorisationServerMetadata;
import es.in2.wallet.application.dto.CredentialIssuerMetadata;
import es.in2.wallet.application.dto.CredentialOffer;
import es.in2.wallet.application.dto.TokenResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Map;

public interface EbsiAuthorisationService {
    Mono<Tuple2<String, String>> getRequestWithOurGeneratedCodeVerifier(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata, String did);

    Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata, Map<String, String> params);
}
