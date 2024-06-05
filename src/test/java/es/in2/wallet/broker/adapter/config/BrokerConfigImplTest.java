package es.in2.wallet.broker.adapter.config;

import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import es.in2.wallet.infrastructure.broker.config.BrokerProperties;
import es.in2.wallet.infrastructure.core.config.properties.UrlProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
            UrlProperties urlProperties = new UrlProperties("http", "example.com", 8080, "/entities");
            when(brokerProperties.internalUrl()).thenReturn(urlProperties);

            BrokerProperties.BrokerPathProperties brokerPathProperties = new BrokerProperties.BrokerPathProperties("/entities");
            when(brokerProperties.paths()).thenReturn(brokerPathProperties);

            brokerConfig = new BrokerConfig(brokerProperties);
            brokerConfig.init();
        }

        @Test
        void testGetProvider() {
            when(brokerProperties.provider()).thenReturn("testProvider");
            assertEquals("testProvider", brokerConfig.getProvider());
        }

        @Test
        void testGetInternalUrl() {
            String expectedUrl = "http://example.com:8080";
            assertEquals(expectedUrl, brokerConfig.getInternalUrl());
        }

        @Test
        void testGetEntitiesPath() {
            String expectedPath = "/entities";
            assertEquals(expectedPath, brokerConfig.getEntitiesPath());
        }
    }
