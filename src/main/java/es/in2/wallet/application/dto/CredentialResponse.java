package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema(description = "Implements the credential response according to " +
        "https://github.com/hesusruiz/EUDIMVP/blob/main/issuance.md#credential-response")
@Builder
public record CredentialResponse(

        @JsonProperty("credentials") List<Credentials> credentials
) {
        @Builder
        public record Credentials(
                @Schema(example = "LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L",
                        description = "Contains issued Credential")
                @JsonProperty("credential") String credential

        ) {
        }
}



