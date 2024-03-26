package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record EbsiCredentialOfferContent(@JsonProperty("credential_offer_uri") String credentialOfferUri
) {
}
