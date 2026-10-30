package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceFeatureAccessTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void selectsAdminWithStoredPermissionsWhenLegacyDuplicateEmailsExist() {
        Admin noPermissions = new Admin();
        noPermissions.setId(8L);
        noPermissions.setEmail("sairohith669@gmail.com");
        noPermissions.setRole("ADMIN");

        Admin configuredAdmin = new Admin();
        configuredAdmin.setId(5L);
        configuredAdmin.setEmail("sairohith669@gmail.com");
        configuredAdmin.setRole("ADMIN");
        configuredAdmin.setFeatureAccessJson("[{\"id\":\"loans\",\"enabled\":true}]");

        Admin manager = new Admin();
        manager.setId(10L);
        manager.setEmail("sairohith669@gmail.com");
        manager.setRole("MANAGER");
        when(adminRepository.findAllByEmailIgnoreCaseOrderByIdDesc("sairohith669@gmail.com"))
                .thenReturn(List.of(manager, noPermissions, configuredAdmin));

        Admin result = adminService.getAdminForFeatureAccess("  SAIROHITH669@GMAIL.COM ");

        assertThat(result).isSameAs(configuredAdmin);
        verify(adminRepository).findAllByEmailIgnoreCaseOrderByIdDesc("sairohith669@gmail.com");
    }

    @Test
    void returnsNullWhenEmailHasNoAdminRole() {
        Admin manager = new Admin();
        manager.setId(10L);
        manager.setEmail("manager@example.com");
        manager.setRole("MANAGER");
        when(adminRepository.findAllByEmailIgnoreCaseOrderByIdDesc("manager@example.com"))
                .thenReturn(List.of(manager));

        assertThat(adminService.getAdminForFeatureAccess("manager@example.com")).isNull();
    }
}
