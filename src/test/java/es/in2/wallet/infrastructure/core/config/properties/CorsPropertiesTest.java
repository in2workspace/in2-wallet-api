package es.in2.wallet.infrastructure.core.config.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void testUrls() {
        CorsProperties corsProperties = new CorsProperties(List.of("url1", "url2"));
        assertThat(corsProperties.allowedOrigins()).containsExactly("url1", "url2");
    }

}