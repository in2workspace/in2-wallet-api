package es.in2.wallet.broker.adapter.config;

import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import es.in2.wallet.infrastructure.broker.config.BrokerProperties;
import es.in2.wallet.infrastructure.core.config.properties.UrlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerConfigImplTest {
        @Mock
        private BrokerProperties brokerProperties;

        @InjectMocks
        private BrokerConfig brokerConfig;

        @Test
        void testGetProvider() {
            var expectedProvider = "testProvider";
            when(brokerProperties.provider()).thenReturn(expectedProvider);

            var result = brokerConfig.getProvider();

            assertThat(result).isEqualTo(expectedProvider);
        }

        @Test
        void testGetInternalUrl() {
            initWithUrlProperties();

            var expectedUrl = "http://example.com:8080";

            var result = brokerConfig.getInternalUrl();

            assertThat(result).isEqualTo(expectedUrl);
        }

        @Test
        void testGetEntitiesPath() {
            initWithUrlPropertiesAndBrokerPathProperties();

            var expectedPath = "/entities";

            var result = brokerConfig.getEntitiesPath();

            assertThat(result).isEqualTo(expectedPath);
        }

    private void initWithUrlProperties() {
        setInternalUrl();

        setAndInitBrokerConfig();
    }

    private void initWithUrlPropertiesAndBrokerPathProperties() {
        setInternalUrl();

        setPaths();

        setAndInitBrokerConfig();
    }

    private void setPaths() {
        BrokerProperties.BrokerPathProperties brokerPathProperties = new BrokerProperties.BrokerPathProperties("/entities");
        when(brokerProperties.paths()).thenReturn(brokerPathProperties);
    }

    private void setInternalUrl() {
        UrlProperties urlProperties = new UrlProperties("http", "example.com", 8080, "/entities");
        when(brokerProperties.internalUrl()).thenReturn(urlProperties);
    }

    private void setAndInitBrokerConfig() {
        brokerConfig = new BrokerConfig(brokerProperties);
        brokerConfig.init();
    }
}
