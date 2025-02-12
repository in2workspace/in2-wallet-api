package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizationRequestOIDC4VP(
        @JsonProperty("scope") List<String> scope,
        @JsonProperty("response_type") String responseType, // always "vp_token"
        @JsonProperty("response_mode") String responseMode,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_id_schema") String clientIdSchema,
        @JsonProperty("state") String state,
        @JsonProperty("nonce") String nonce,
        @JsonProperty("response_uri") String responseUri,
        @JsonProperty("presentation_definition") String presentationDefinition,
        @JsonProperty("presentation_definition_uri") String presentationDefinitionUri,
        @JsonProperty("client_metadata") String clientMetadata

) {
}
