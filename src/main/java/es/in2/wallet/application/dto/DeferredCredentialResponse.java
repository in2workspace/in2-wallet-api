package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeferredCredentialResponse(
        @JsonProperty("transaction_id")
        String transactionId,
        @JsonProperty("c_nonce")
        String cNonce,
        @JsonProperty("c_nonce_expires_in")
        int cNonceExpiresIn
) {
}
