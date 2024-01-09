package es.in2.wallet.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiApplication::main).with(TestApiApplication.class).run(args);
	}

}
