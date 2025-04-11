package es.in2.wallet.api.service;

import es.in2.wallet.domain.exceptions.JwtInvalidFormatException;
import es.in2.wallet.domain.services.impl.VerifierValidationServiceImpl;
import es.in2.wallet.infrastructure.appconfiguration.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class VerifierValidationServiceImplTest {
    @InjectMocks
    private VerifierValidationServiceImpl verifierValidationService;

    @Test
    void testParseAuthorizationRequestError() {
        String processId = "123";
        String invalidToken = "invalid_jwt";

        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, invalidToken))
                .expectErrorMatches(throwable -> throwable instanceof JwtInvalidFormatException)
                .verify();
    }

    @Test
    void testClient_id_not_found_Error() {
        String processId = "123";
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoicyIsImF1dGhfcmVxdWVzdCI6Im5uIiwiaWF0IjoxNTE2MjM5MDIyfQ.CCos8azCLWoYMAsj9k7_ceIJ6JY3E0fzBn3imxwR4Dw";

        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, invalidToken))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException)
                .verify();
    }

    @Test
    void testClient_id_not_equal_to_iss_or_sub_Error() {
        String processId = "123";
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoicyIsImF1dGhfcmVxdWVzdCI6Im9wZW5pZDovLz9zY29wZT1bc10mcmVzcG9uc2VfdHlwZT12cF90b2tlbiZyZXNwb25zZV9tb2RlPWRpcmVjdF9wb3N0JmNsaWVudF9pZD1kaWQ6a2V5OmZmJnN0YXRlPWJmZmYmbm9uY2U9OGY4ZiZyZWRpcmVjdF91cmk9aHR0cDovL2xvY2FsaG9zdDo4MDkyIiwiaWF0IjoxNTE2MjM5MDIyfQ.Lt0tUiH_CN0uYD8_U9tXctS7vu_szSk-zr3NYSnh9BI";
        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, invalidToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testDid_invalid_format_Error() {
        String processId = "123";
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDprZXk6ZmYifQ.eyJzdWIiOiJkaWQ6a2V5OmZmIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoiZGlkOmtleTpmZiIsImF1dGhfcmVxdWVzdCI6Im9wZW5pZDovLz9zY29wZT1bc10mcmVzcG9uc2VfdHlwZT12cF90b2tlbiZyZXNwb25zZV9tb2RlPWRpcmVjdF9wb3N0JmNsaWVudF9pZD1kaWQ6a2V5OmZmJnN0YXRlPWJmZmYmbm9uY2U9OGY4ZiZyZWRpcmVjdF91cmk9aHR0cDovL2xvY2FsaG9zdDo4MDkyIiwiaWF0IjoxNTE2MjM5MDIyfQ.bMiUlMoFWBJ03eZq3X95HGI8maApUigc1GZo6QlsK_U";
        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, invalidToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testIssuerOfTheAuthorizationRequestTest_VerificationFailed() {
        String processId = "123";
        String jwtAuthorizationRequest = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImRpZDprZXk6ekRuYWVoOVNXcXpjalpNYkZkdnRFVDJwTGlReHQ2Qm9YdDRwdjNXYmRSbmczZDJZaiJ9.eyJzdWIiOiJkaWQ6a2V5OmZmIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoiZGlkOmtleTpmZiIsImF1dGhfcmVxdWVzdCI6Im9wZW5pZDovLz9zY29wZT1bc10mcmVzcG9uc2VfdHlwZT12cF90b2tlbiZyZXNwb25zZV9tb2RlPWRpcmVjdF9wb3N0JmNsaWVudF9pZD1kaWQ6a2V5OmZmJnN0YXRlPWJmZmYmbm9uY2U9OGY4ZiZyZWRpcmVjdF91cmk9aHR0cDovL2xvY2FsaG9zdDo4MDkyIiwiaWF0IjoxNTE2MjM5MDIyfQ.MMNr3ar7HW0m8Tl6Eav5GCn3t0QZQeL1Vpi5wuUpzjY";
        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
    @Test
    void testMissingDcqlQueryParameterError() {
        String processId = "123";

        // Header con claim 'type' correcto
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\",\"type\":\"oauth-authz-req+jwt\"}".getBytes());

        // Payload sin el parámetro 'dcql_query'
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"did:key:xyz\",\"iss\":\"did:key:xyz\",\"aud\":\"http://localhost:8080\"}".getBytes());

        // Firma falsa (no importa en test de validación interna)
        String signature = "dummySignature";

        String tokenWithoutDcqlQuery = String.format("%s.%s.%s", header, payload, signature);

        StepVerifier.create(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, tokenWithoutDcqlQuery))
                .expectErrorMatches(throwable ->
                        throwable instanceof InvalidRequestException &&
                                throwable.getMessage().contains("dcql_query"))
                .verify();
    }
}
