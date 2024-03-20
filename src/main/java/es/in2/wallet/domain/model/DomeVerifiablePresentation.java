package es.in2.wallet.domain.model;

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

        @JsonProperty("verifiableCredential")
        List<JsonNode> verifiableCredential
) {
}
