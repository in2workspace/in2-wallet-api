package es.in2.wallet;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WalletServerApplication {

	private static final ObjectMapper OBJECT_MAPPER =
			JsonMapper.builder().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).build();

	public static void main(String[] args) {
		SpringApplication.run(WalletServerApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return OBJECT_MAPPER;
	}

}
