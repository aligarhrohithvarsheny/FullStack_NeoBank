package com.neo.springapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SpringappApplicationTests {

	@Autowired
	private com.neo.springapp.service.BranchAccountService branchAccountService;

	@Test
	void contextLoads() {
		// This test will pass if the Spring context loads successfully
	}

	@Test
	void centralAccountSummaryExecutesAgainstTheDatabase() {
		var summary = branchAccountService.getBranchAccountSummary();

		assertThat(summary).containsKeys("accountNumber", "balance", "totalCredits", "totalDebits");
	}

}
