package es.in2.wallet.broker.adapter.config;

import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import es.in2.wallet.infrastructure.broker.config.BrokerProperties;
import es.in2.wallet.infrastructure.core.config.properties.UrlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerConfigImplTest {
        @Mock
        private BrokerProperties brokerProperties;


        @InjectMocks
        private BrokerConfig brokerConfig;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);
            UrlProperties urlProperties = new UrlProperties("https","example.com",8080,"/entities");
            when(brokerProperties.internalUrl()).thenReturn(urlProperties);
//            // Mock broker properties
//            when(brokerProperties.internalUrl().scheme()).thenReturn("https");
//            when(brokerProperties.internalUrl().domain()).thenReturn("example.com");
//            when(brokerProperties.internalUrl().port()).thenReturn(443);

            // Initialize BrokerConfig
            brokerConfig = new BrokerConfig(brokerProperties);
            brokerConfig.init();
        }

        @Test
        void testGetProvider() {
            when(brokerProperties.provider()).thenReturn("testProvider");
            assertEquals("testProvider", brokerConfig.getProvider());
        }

        @Test
        void testGetExternalUrl() {
            String expectedUrl = "https://example.com";
            assertEquals(expectedUrl, brokerConfig.getExternalUrl());
        }

        @Test
        void testGetEntitiesPath() {
            when(brokerProperties.paths().entities()).thenReturn("/entities");
            String expectedPath = "/entities";
            assertEquals(expectedPath, brokerConfig.getEntitiesPath());
        }
    }
