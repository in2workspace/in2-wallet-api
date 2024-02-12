package es.in2.wallet.api.config;

import id.walt.servicematrix.ServiceMatrix;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WaltIdConf {

    @Tag(name = "WaltidConfig", description = "Injects Walt.id services at runtime")
    @Bean
    public ServiceMatrix instanceServiceMatrix() {
        return new ServiceMatrix("service-matrix.properties");
    }

}
