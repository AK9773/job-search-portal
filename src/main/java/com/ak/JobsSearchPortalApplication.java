package com.ak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class JobsSearchPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobsSearchPortalApplication.class, args);
	}

}
