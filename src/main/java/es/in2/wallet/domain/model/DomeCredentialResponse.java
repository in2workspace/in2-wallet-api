package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DomeCredentialResponse(
        @JsonProperty("credential")
        String credential,
        @JsonProperty("transaction_id")
        String transactionId,
        @JsonProperty("c_nonce")
        String cNonce,
        @JsonProperty("c_nonce_expires_in")
        int cNonceExpiresIn
) {
}