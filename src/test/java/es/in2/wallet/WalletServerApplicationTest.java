package es.in2.wallet;

import org.springframework.boot.SpringApplication;

public class WalletServerApplicationTest {

	public static void main(String[] args) {
		SpringApplication.from(WalletServerApplication::main).with(WalletServerApplicationTest.class).run(args);
	}

}
