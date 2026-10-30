package com.neo.springapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.service.UserSessionTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerPortalLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserSessionTokenService tokenService;

    @Test
    @Transactional
    void hodGeneratedCredentialsAuthenticateManagerWithManagerScope() throws Exception {
        Admin manager = new Admin();
        manager.setName("Branch Manager");
        manager.setEmail("branch.manager.test@example.com");
        manager.setRole("MANAGER");
        manager.setAssignedCity("Test City");
        manager = adminRepository.saveAndFlush(manager);

        String hodToken = tokenService.issueAdmin(1L, "hod.test@example.com", "HOD");
        String provisionResponse = mockMvc.perform(post("/api/admins/hod/staff/{id}/manager-login", manager.getId())
                        .header("Authorization", "Bearer " + hodToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.centralAccountNumber").value("NEOBANK000001"))
                .andReturn().getResponse().getContentAsString();
        JsonNode credentials = objectMapper.readTree(provisionResponse);

        String loginResponse = mockMvc.perform(post("/api/admins/manager-user-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", credentials.path("username").asText())
                                .put("password", credentials.path("password").asText()).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.admin.managerLoginPasswordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode session = objectMapper.readTree(loginResponse);

        UserSessionTokenService.SessionPrincipal principal =
                tokenService.verify(session.path("sessionToken").asText());
        assertThat(principal).isNotNull();
        assertThat(principal.scope()).isEqualTo("MANAGER");
        assertThat(principal.userId()).isEqualTo(manager.getId());
        assertThat(principal.accountNumber()).isEqualTo(manager.getEmail());
    }

    @Test
    @Transactional
    void managerWithoutAssignedCityCannotLogin() throws Exception {
        Admin manager = new Admin();
        manager.setName("Unassigned Manager");
        manager.setEmail("unassigned.manager.test@example.com");
        manager.setRole("MANAGER");
        manager.setManagerLoginUsername("mgr-999999");
        manager.setManagerLoginPasswordHash("$2a$10$invalid-hash-value");
        adminRepository.saveAndFlush(manager);

        mockMvc.perform(post("/api/admins/manager-user-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mgr-999999\",\"password\":\"unused\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
