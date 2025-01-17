package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SingleCredentialResponse(
        @JsonProperty("credential")
        String credential,
        @JsonProperty("c_nonce")
        String cNonce,
        @JsonProperty("c_nonce_expires_in")
        int cNonceExpiresIn
) {
}
