package es.in2.wallet.api.ebsi.comformance.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CredentialOfferContent(
        @JsonProperty("credential_offer_uri")
        String credentialOfferUri
) {
}
