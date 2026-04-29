package com.neo.springapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpringappApplicationTests {

	@Test
	void contextLoads() {
		// This test will pass if the Spring context loads successfully
	}

}
