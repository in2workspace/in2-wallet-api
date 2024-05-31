package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record WebSocketServerMessage(
        @JsonProperty("tx_code") CredentialOffer.Grant.PreAuthorizedCodeGrant.TxCode txCode,
        @JsonProperty("pin") Boolean pin
) {
}
