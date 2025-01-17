package es.in2.wallet.application.dto;

import lombok.Builder;
import org.springframework.http.HttpStatusCode;

@Builder
public record CredentialResponseWithStatus(
        CredentialResponse credentialResponse,
        HttpStatusCode statusCode
) {
}
