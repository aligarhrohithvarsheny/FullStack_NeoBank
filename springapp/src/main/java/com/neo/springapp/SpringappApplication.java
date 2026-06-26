package com.neo.springapp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringappApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SpringappApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

}
