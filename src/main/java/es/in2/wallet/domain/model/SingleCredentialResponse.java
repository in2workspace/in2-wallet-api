package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SingleCredentialResponse(
        @JsonProperty("credential")
        String credential,
        @JsonProperty("c_nonce_expires_in")
        int cNonceExpiresIn
) {
}
