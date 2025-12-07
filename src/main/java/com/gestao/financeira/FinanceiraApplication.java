package com.gestao.financeira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinanceiraApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceiraApplication.class, args);
	}

}
