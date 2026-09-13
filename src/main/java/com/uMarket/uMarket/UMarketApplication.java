package com.uMarket.uMarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(UMarketApplication.class, args);
	}

}
