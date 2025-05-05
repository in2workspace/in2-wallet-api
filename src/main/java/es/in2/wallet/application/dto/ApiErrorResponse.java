package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ApiErrorResponse(
        @JsonProperty("type") String type,
        @JsonProperty("title") String title,
        @JsonProperty("status") int status,
        @JsonProperty("detail") String detail,
        @JsonProperty("instance") String instance
) {
}
