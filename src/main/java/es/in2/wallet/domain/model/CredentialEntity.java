package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialEntity(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("status") CredentialStatusAttribute credentialStatusAttribute,
        @JsonProperty("credentialType") CredentialTypeAttribute credentialTypeAttribute,
        @JsonProperty("jwt_vc") CredentialAttribute jwtCredentialAttribute,
        @JsonProperty("cwt_vc") CredentialAttribute cwtCredentialAttribute,
        @JsonProperty("json_vc") CredentialAttribute jsonCredentialAttribute,
        @JsonProperty("belongsTo") RelationshipAttribute relationshipAttribute

) {
}
