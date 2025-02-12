package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.List;

@Builder
public record DomeVerifiablePresentation(
        @JsonProperty("@context")
        List<String> context,

        @JsonProperty("type")
        List<String> type,

        @JsonProperty("holder")
        String holder,
        @JsonProperty("nonce")
        String nonce,

        @JsonProperty("verifiableCredential")
        List<JsonNode> verifiableCredential
) {
}
