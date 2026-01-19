package com.diddycart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class DiddycartApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiddycartApplication.class, args);
	}

}
