package es.in2.wallet.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthorisationServerMetadataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization() throws JsonProcessingException {
        AuthorisationServerMetadata.AuthenticationMethodsSupported authMethodsSupported =
                AuthorisationServerMetadata.AuthenticationMethodsSupported.builder()
                        .authorizationEndpoint(List.of("auth_endpoint1", "auth_endpoint2"))
                        .build();

        AuthorisationServerMetadata.VpFormatsSupported.AlgValuesSupported algValuesSupported =
                AuthorisationServerMetadata.VpFormatsSupported.AlgValuesSupported.builder()
                        .algValuesSupported(List.of("RS256", "ES256"))
                        .build();

        AuthorisationServerMetadata.VpFormatsSupported vpFormatsSupported =
                AuthorisationServerMetadata.VpFormatsSupported.builder()
                        .jwtVp(algValuesSupported)
                        .jwtVc(algValuesSupported)
                        .build();

        AuthorisationServerMetadata metadata = AuthorisationServerMetadata.builder()
                .redirectUris(List.of("https://example.com/redirect"))
                .issuer("https://example.com")
                .authorizationEndpoint("https://example.com/authorize")
                .tokenEndpoint("https://example.com/token")
                .userInfoEndpoint("https://example.com/userinfo")
                .presentationDefinitionEndpoint("https://example.com/presentation")
                .jwksUri("https://example.com/jwks")
                .scopesSupported(List.of("openid", "profile"))
                .responseTypesSupported(List.of("code", "token"))
                .responseModesSupported(List.of("query", "fragment"))
                .grantTypesSupported(List.of("authorization_code", "client_credentials"))
                .subjectTypesSupported(List.of("public", "pairwise"))
                .idTokenSigningAlgValuesSupported(List.of("RS256", "ES256"))
                .requestObjectSigningAlgValuesSupported(List.of("RS256"))
                .requestParameterSupported(true)
                .requestUriParameterSupported(true)
                .tokenEndpointAuthMethodsSupported(List.of("client_secret_basic", "client_secret_post"))
                .requestAuthenticationMethodsSupported(authMethodsSupported)
                .vpFormatsSupported(vpFormatsSupported)
                .subjectSyntaxTypesSupported(List.of("subjectSyntax1", "subjectSyntax2"))
                .subjectSyntaxTypesDiscriminations(List.of("discrimination1", "discrimination2"))
                .subjectTrustFrameworksSupported(List.of("trustFramework1", "trustFramework2"))
                .idTokenTypesSupported(List.of("JWT", "ID_TOKEN"))
                .build();

        String json = objectMapper.writeValueAsString(metadata);
        assertThat(json).contains("https://example.com","RS256");
    }

    @Test
    void testDeserialization() throws JsonProcessingException {
        String json = """
                {
                    "redirect_uris": ["https://example.com/redirect"],
                    "issuer": "https://example.com",
                    "authorization_endpoint": "https://example.com/authorize",
                    "token_endpoint": "https://example.com/token",
                    "userinfo_endpoint": "https://example.com/userinfo",
                    "presentation_definition_endpoint": "https://example.com/presentation",
                    "jwks_uri": "https://example.com/jwks",
                    "scopes_supported": ["openid", "profile"],
                    "response_types_supported": ["code", "token"],
                    "response_modes_supported": ["query", "fragment"],
                    "grant_types_supported": ["authorization_code", "client_credentials"],
                    "subject_types_supported": ["public", "pairwise"],
                    "id_token_signing_alg_values_supported": ["RS256", "ES256"],
                    "request_object_signing_alg_values_supported": ["RS256"],
                    "request_parameter_supported": true,
                    "request_uri_parameter_supported": true,
                    "token_endpoint_auth_methods_supported": ["client_secret_basic", "client_secret_post"],
                    "request_authentication_methods_supported": {
                        "authorization_endpoint": ["auth_endpoint1", "auth_endpoint2"]
                    },
                    "vp_formats_supported": {
                        "jwt_vp": {
                            "alg_values_supported": ["RS256", "ES256"]
                        },
                        "jwt_vc": {
                            "alg_values_supported": ["RS256", "ES256"]
                        }
                    },
                    "subject_syntax_types_supported": ["subjectSyntax1", "subjectSyntax2"],
                    "subject_syntax_types_discriminations": ["discrimination1", "discrimination2"],
                    "subject_trust_frameworks_supported": ["trustFramework1", "trustFramework2"],
                    "id_token_types_supported": ["JWT", "ID_TOKEN"]
                }
                """;

        AuthorisationServerMetadata metadata = objectMapper.readValue(json, AuthorisationServerMetadata.class);

        assertThat(metadata.issuer()).isEqualTo("https://example.com");
        assertThat(metadata.idTokenSigningAlgValuesSupported()).contains("RS256", "ES256");
        assertThat(metadata.requestAuthenticationMethodsSupported().authorizationEndpoint()).contains("auth_endpoint1", "auth_endpoint2");
        assertThat(metadata.vpFormatsSupported().jwtVp().algValuesSupported()).contains("RS256", "ES256");
    }
}

