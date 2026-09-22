package com.neo.springapp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SpringappApplication {

	public static void main(String[] args) {
		String timeZone = System.getenv().getOrDefault("APP_TIME_ZONE", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timeZone)));
		SpringApplication app = new SpringApplication(SpringappApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.setLazyInitialization(Boolean.parseBoolean(
				System.getenv().getOrDefault("SPRING_MAIN_LAZY_INITIALIZATION", "false")));
		app.run(args);
	}

}
