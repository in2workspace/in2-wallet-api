package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionDataAttribute(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("deferred_endpoint") String deferredEndpoint
){
}
