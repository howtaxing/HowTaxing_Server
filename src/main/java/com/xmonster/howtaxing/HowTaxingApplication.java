package com.xmonster.howtaxing;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableBatchProcessing
@EnableFeignClients
@EnableJpaAuditing
@SpringBootApplication
public class HowTaxingApplication {
	public static void main(String[] args) {
		SpringApplication.run(HowTaxingApplication.class, args);
	}
}