package es.in2.wallet.domain.utils;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationUtilsTest {

    @Test
    void itShouldReturnCompleteUrl() {
        UriComponents originalUri = UriComponentsBuilder
                .newInstance()
                .scheme("https")
                .host("example.org")
                .port(80)
                .path("/example")
                .build();

        var expectedUrl = originalUri.toString();

        var result = ApplicationUtils.formatUrl(originalUri.getScheme(), originalUri.getHost(), originalUri.getPort(), originalUri.getPath());

        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    void itShouldNotSetPortWhenInputPortIs443() {
        UriComponents originalUri = UriComponentsBuilder
                .newInstance()
                .scheme("https")
                .host("example.org")
                .port(443)
                .path("/example")
                .build();

        UriComponents uriWithoutPort = UriComponentsBuilder
                .newInstance()
                .scheme(originalUri.getScheme())
                .host(originalUri.getHost())
                .path(Objects.requireNonNull(originalUri.getPath()))
                .build();

        var expectedUrl = uriWithoutPort.toString();

        var result = ApplicationUtils.formatUrl(originalUri.getScheme(), originalUri.getHost(), originalUri.getPort(), originalUri.getPath());

        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    void itShouldNotSetPortWhenInputPortIsNull() {
        UriComponents originalUri = UriComponentsBuilder
                .newInstance()
                .scheme("https")
                .host("example.org")
                .path("/example")
                .build();

        UriComponents uriWithoutPort = UriComponentsBuilder
                .newInstance()
                .scheme(originalUri.getScheme())
                .host(originalUri.getHost())
                .path(Objects.requireNonNull(originalUri.getPath()))
                .build();

        var expectedUrl = uriWithoutPort.toString();

        var result = ApplicationUtils.formatUrl(originalUri.getScheme(), originalUri.getHost(), null, originalUri.getPath());

        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    void itShouldNotSetPathWhenInputPathIsNull() {
        UriComponents originalUri = UriComponentsBuilder
                .newInstance()
                .scheme("https")
                .host("example.org")
                .port(80)
                .build();

        var expectedUrl = originalUri.toString();

        var result = ApplicationUtils.formatUrl(originalUri.getScheme(), originalUri.getHost(), originalUri.getPort(), null);

        assertThat(result).isEqualTo(expectedUrl);
    }
}