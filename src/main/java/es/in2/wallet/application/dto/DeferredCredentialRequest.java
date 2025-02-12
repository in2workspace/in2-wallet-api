package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DeferredCredentialRequest(
        @JsonProperty("transaction_id") String transactionId
) {
}
