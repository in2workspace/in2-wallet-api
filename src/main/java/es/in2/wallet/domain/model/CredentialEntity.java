package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialEntity(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("status") CredentialStatusAttribute credentialStatusAttribute,
        @JsonProperty("credentialType") CredentialTypeAttribute credentialTypeAttribute,
        @JsonProperty("jwt_vc") CredentialAttribute jwtCredentialAttribute,
        @JsonProperty("cwt_vc") CredentialAttribute cwtCredentialAttribute,
        // This field is required and must always be present as it contains the JSON
        // representation of the credential, which is necessary to visualize the credential's
        // data in the frontend.
        @JsonProperty("json_vc") @NotBlank CredentialAttribute jsonCredentialAttribute,
        @JsonProperty("belongsTo") RelationshipAttribute relationshipAttribute
) {
}
