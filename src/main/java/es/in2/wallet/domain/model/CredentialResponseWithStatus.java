package es.in2.wallet.domain.model;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record CredentialResponseWithStatus(
        String credentialResponse,
        HttpStatusCode statusCode
) {
}
