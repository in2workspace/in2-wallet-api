package es.in2.wallet.api.config;

import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    private SwaggerConfig swaggerConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        swaggerConfig = new SwaggerConfig();
    }

    @Test
    void testPublicApi() {
        GroupedOpenApi publicApi = swaggerConfig.publicApi();
        assertThat(publicApi).isNotNull();
        assertThat(publicApi.getGroup()).isEqualTo("Public API");
        assertThat(publicApi.getPathsToMatch()).containsExactly("/**");

        // Verify the customizer is configured correctly
        OpenApiCustomizer customizer = publicApi.getOpenApiCustomizers().get(0);
        assertThat(customizer).isNotNull();
    }

    @Test
    void testPrivateApi() {
        GroupedOpenApi privateApi = swaggerConfig.privateApi();
        assertThat(privateApi).isNotNull();
        assertThat(privateApi.getGroup()).isEqualTo("Private API");
        assertThat(privateApi.getPathsToMatch()).containsExactly("/**");

        // Verify the customizer is configured correctly
        OpenApiCustomizer customizer = privateApi.getOpenApiCustomizers().get(0);
        assertThat(customizer).isNotNull();
    }
}
