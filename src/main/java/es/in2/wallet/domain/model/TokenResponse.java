package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("c_nonce") String cNonce,
        @JsonProperty("c_nonce_expires_in") String cNonceExpiresIn,
        @JsonProperty("authorization_details") List<AuthorizationDetail> authorizationDetails
) {
    @Builder
    public record AuthorizationDetail(
            @JsonProperty("type") String type,
            @JsonProperty("credential_configuration_id") String credentialConfigurationId
    ) {
    }
}
