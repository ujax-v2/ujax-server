package com.ujax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UjaxServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UjaxServerApplication.class, args);
	}

}
