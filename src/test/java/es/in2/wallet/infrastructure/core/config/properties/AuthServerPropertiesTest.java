package es.in2.wallet.infrastructure.core.config.properties;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;

class AuthServerPropertiesTest {

    @Test
    void shouldUseProvidedValuesWhenNotNull() {
        // Arrange
        UrlProperties externalUrlMock = Mockito.mock(UrlProperties.class);
        UrlProperties internalUrlMock = Mockito.mock(UrlProperties.class);

        // Act
        AuthServerProperties properties = new AuthServerProperties(externalUrlMock, internalUrlMock);

        // Assert
        assertEquals(externalUrlMock, properties.externalUrl());
        assertEquals(internalUrlMock, properties.internalUrl());
    }
}
