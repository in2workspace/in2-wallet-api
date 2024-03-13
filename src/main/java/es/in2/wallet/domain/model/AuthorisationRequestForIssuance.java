package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record AuthorisationRequestForIssuance(
        @JsonProperty("response_type") String responseType,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("redirect_uri") String redirectUri,
        @JsonProperty("scope") String scope,
        @JsonProperty("issuer_state") String issuerState,
        @JsonProperty("state") String state,
        @JsonProperty("authorization_details") List<AuthorizationDetail> authorizationDetails,
        @JsonProperty("code_challenge") String codeChallenge,
        @JsonProperty("code_challenge_method") String codeChallengeMethod
) {

    @Builder
    public record AuthorizationDetail(
            @JsonProperty("type") String type,
            @JsonProperty("locations") List<String> locations,
            @JsonProperty("format") String format,
            @JsonProperty("types") List<String> types) {
    }
}
