package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionEntity (
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,

        @JsonProperty("transactionData") EntityAttribute<TransactionDataAttribute> transactionDataAttribute,
        @JsonProperty("linkedTo") RelationshipAttribute relationshipAttribute
){
}
