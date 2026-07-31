package com.waffles.harbor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class HarborApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarborApplication.class, args);
		System.out.println("Hello worldicls");
	}

}
