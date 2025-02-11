package com.xpertcash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XpertcashApplication {

	public static void main(String[] args) {
		SpringApplication.run(XpertcashApplication.class, args);
	}

}
