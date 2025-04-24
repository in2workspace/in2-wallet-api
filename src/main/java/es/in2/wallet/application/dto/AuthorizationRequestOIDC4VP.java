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
        @JsonProperty("response_uri") String responseUri
        //@JsonProperty("dcql_query") DcqlQuery dcqlQuery

) {
   /* @Builder
    public record DcqlQuery(
            @JsonProperty("credentials") List<DcqlCredential> credentials
    ) {}

    @Builder
    public record DcqlCredential(
            @JsonProperty("id") String id,
            @JsonProperty("format") String format
    ) {}*/
}
