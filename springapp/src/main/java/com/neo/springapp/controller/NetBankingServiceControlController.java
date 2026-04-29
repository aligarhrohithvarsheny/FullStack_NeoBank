package com.neo.springapp.controller;

import com.neo.springapp.model.NetBankingServiceControl;
import com.neo.springapp.model.NetBankingServiceAudit;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.repository.NetBankingServiceControlRepository;
import com.neo.springapp.repository.NetBankingServiceAuditRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/net-banking-control")
public class NetBankingServiceControlController {

    @Autowired
    private NetBankingServiceControlRepository controlRepository;

    @Autowired
    private NetBankingServiceAuditRepository auditRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    /**
     * Get status of all net banking services
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        List<NetBankingServiceControl> controls = controlRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        for (NetBankingServiceControl control : controls) {
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("enabled", control.getEnabled());
            serviceInfo.put("updatedBy", control.getUpdatedBy());
            serviceInfo.put("updatedAt", control.getUpdatedAt());
            response.put(control.getServiceType(), serviceInfo);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get status of a specific service type (SAVINGS_ACCOUNT or CURRENT_ACCOUNT)
     */
    @GetMapping("/status/{serviceType}")
    public ResponseEntity<Map<String, Object>> getServiceStatus(@PathVariable String serviceType) {
        Optional<NetBankingServiceControl> opt = controlRepository.findByServiceType(serviceType.toUpperCase());
        Map<String, Object> response = new HashMap<>();
        if (opt.isPresent()) {
            NetBankingServiceControl control = opt.get();
            response.put("serviceType", control.getServiceType());
            response.put("enabled", control.getEnabled());
            response.put("updatedBy", control.getUpdatedBy());
            response.put("updatedAt", control.getUpdatedAt());
            return ResponseEntity.ok(response);
        }
        response.put("error", "Service type not found");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Toggle net banking service ON or OFF
     */
    @PutMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleService(@RequestBody Map<String, Object> request) {
        String serviceType = (String) request.get("serviceType");
        Boolean enabled = (Boolean) request.get("enabled");
        String changedBy = (String) request.get("changedBy");
        String remarks = (String) request.get("remarks");

        if (serviceType == null || enabled == null || changedBy == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "serviceType, enabled, and changedBy are required");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<NetBankingServiceControl> opt = controlRepository.findByServiceType(serviceType.toUpperCase());
        if (opt.isEmpty()) {
            // Create new entry if it doesn't exist
            NetBankingServiceControl newControl = new NetBankingServiceControl();
            newControl.setServiceType(serviceType.toUpperCase());
            newControl.setEnabled(enabled);
            newControl.setUpdatedBy(changedBy);
            controlRepository.save(newControl);

            // Record audit
            NetBankingServiceAudit audit = new NetBankingServiceAudit();
            audit.setServiceType(serviceType.toUpperCase());
            audit.setOldStatus(true); // default was true
            audit.setNewStatus(enabled);
            audit.setChangedBy(changedBy);
            audit.setRemarks(remarks);
            auditRepository.save(audit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("serviceType", serviceType.toUpperCase());
            response.put("enabled", enabled);
            response.put("message", "Net banking service created and set to " + (enabled ? "ON" : "OFF"));
            return ResponseEntity.ok(response);
        }

        NetBankingServiceControl control = opt.get();
        Boolean oldStatus = control.getEnabled();

        // Update the control
        control.setEnabled(enabled);
        control.setUpdatedBy(changedBy);
        controlRepository.save(control);

        // Record audit log with timestamp
        NetBankingServiceAudit audit = new NetBankingServiceAudit();
        audit.setServiceType(serviceType.toUpperCase());
        audit.setOldStatus(oldStatus);
        audit.setNewStatus(enabled);
        audit.setChangedBy(changedBy);
        audit.setRemarks(remarks);
        auditRepository.save(audit);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("serviceType", control.getServiceType());
        response.put("enabled", control.getEnabled());
        response.put("updatedBy", control.getUpdatedBy());
        response.put("updatedAt", control.getUpdatedAt());
        response.put("message", "Net banking for " + serviceType + " has been turned " + (enabled ? "ON" : "OFF"));
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit history for all services
     */
    @GetMapping("/audit")
    public ResponseEntity<List<NetBankingServiceAudit>> getAuditHistory() {
        return ResponseEntity.ok(auditRepository.findAllByOrderByChangedAtDesc());
    }

    /**
     * Get audit history for a specific service type
     */
    @GetMapping("/audit/{serviceType}")
    public ResponseEntity<List<NetBankingServiceAudit>> getServiceAuditHistory(@PathVariable String serviceType) {
        return ResponseEntity.ok(auditRepository.findByServiceTypeOrderByChangedAtDesc(serviceType.toUpperCase()));
    }

    /**
     * Check if a specific service type is enabled (used by login endpoints)
     */
    @GetMapping("/is-enabled/{serviceType}")
    public ResponseEntity<Map<String, Object>> isServiceEnabled(@PathVariable String serviceType) {
        Optional<NetBankingServiceControl> opt = controlRepository.findByServiceType(serviceType.toUpperCase());
        Map<String, Object> response = new HashMap<>();
        if (opt.isPresent()) {
            response.put("enabled", opt.get().getEnabled());
        } else {
            // Default to enabled if no record exists
            response.put("enabled", true);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Search savings account customers for net banking management
     */
    @GetMapping("/customers/savings")
    public ResponseEntity<Map<String, Object>> searchSavingsCustomers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Account> accounts;
        if (search.isEmpty()) {
            accounts = accountRepository.findAll(PageRequest.of(page, size));
        } else {
            accounts = accountRepository.searchAccounts(search, PageRequest.of(page, size));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("customers", accounts.getContent().stream().map(a -> {
            Map<String, Object> customer = new HashMap<>();
            customer.put("id", a.getId());
            customer.put("name", a.getName());
            customer.put("accountNumber", a.getAccountNumber());
            customer.put("customerId", a.getCustomerId());
            customer.put("phone", a.getPhone());
            customer.put("accountType", a.getAccountType());
            customer.put("status", a.getStatus());
            customer.put("netBankingEnabled", a.getNetBankingEnabled() != null ? a.getNetBankingEnabled() : true);
            customer.put("netBankingToggledBy", a.getNetBankingToggledBy());
            customer.put("netBankingToggledAt", a.getNetBankingToggledAt());
            return customer;
        }).toList());
        response.put("totalElements", accounts.getTotalElements());
        response.put("totalPages", accounts.getTotalPages());
        response.put("currentPage", accounts.getNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Search current account customers for net banking management
     */
    @GetMapping("/customers/current")
    public ResponseEntity<Map<String, Object>> searchCurrentCustomers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CurrentAccount> accounts;
        if (search.isEmpty()) {
            accounts = currentAccountRepository.findAll(PageRequest.of(page, size));
        } else {
            accounts = currentAccountRepository.searchAccounts(search, PageRequest.of(page, size));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("customers", accounts.getContent().stream().map(a -> {
            Map<String, Object> customer = new HashMap<>();
            customer.put("id", a.getId());
            customer.put("name", a.getOwnerName());
            customer.put("businessName", a.getBusinessName());
            customer.put("accountNumber", a.getAccountNumber());
            customer.put("customerId", a.getCustomerId());
            customer.put("mobile", a.getMobile());
            customer.put("status", a.getStatus());
            customer.put("netBankingEnabled", a.getNetBankingEnabled() != null ? a.getNetBankingEnabled() : true);
            customer.put("netBankingToggledBy", a.getNetBankingToggledBy());
            customer.put("netBankingToggledAt", a.getNetBankingToggledAt());
            return customer;
        }).toList());
        response.put("totalElements", accounts.getTotalElements());
        response.put("totalPages", accounts.getTotalPages());
        response.put("currentPage", accounts.getNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Toggle net banking for an individual customer
     */
    @PutMapping("/customer/toggle")
    public ResponseEntity<Map<String, Object>> toggleCustomerNetBanking(@RequestBody Map<String, Object> request) {
        String accountNumber = (String) request.get("accountNumber");
        String serviceType = (String) request.get("serviceType");
        Boolean enabled = (Boolean) request.get("enabled");
        String changedBy = (String) request.get("changedBy");
        String remarks = (String) request.get("remarks");

        if (accountNumber == null || serviceType == null || enabled == null || changedBy == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "accountNumber, serviceType, enabled, and changedBy are required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> response = new HashMap<>();

        if ("SAVINGS_ACCOUNT".equalsIgnoreCase(serviceType)) {
            Account account = accountRepository.findByAccountNumber(accountNumber);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Savings account not found");
                return ResponseEntity.badRequest().body(response);
            }
            Boolean oldStatus = account.getNetBankingEnabled() != null ? account.getNetBankingEnabled() : true;
            account.setNetBankingEnabled(enabled);
            account.setNetBankingToggledBy(changedBy);
            account.setNetBankingToggledAt(LocalDateTime.now());
            accountRepository.save(account);

            // Record audit
            NetBankingServiceAudit audit = new NetBankingServiceAudit();
            audit.setServiceType("SAVINGS_ACCOUNT");
            audit.setOldStatus(oldStatus);
            audit.setNewStatus(enabled);
            audit.setChangedBy(changedBy);
            audit.setAccountNumber(accountNumber);
            audit.setCustomerName(account.getName());
            audit.setRemarks(remarks != null ? remarks : "Individual customer toggle");
            auditRepository.save(audit);

            response.put("success", true);
            response.put("customerName", account.getName());
            response.put("accountNumber", accountNumber);
            response.put("netBankingEnabled", enabled);
            response.put("message", "Net banking for savings account " + accountNumber + " (" + account.getName() + ") has been turned " + (enabled ? "ON" : "OFF"));

        } else if ("CURRENT_ACCOUNT".equalsIgnoreCase(serviceType)) {
            Optional<CurrentAccount> opt = currentAccountRepository.findByAccountNumber(accountNumber);
            if (opt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Current account not found");
                return ResponseEntity.badRequest().body(response);
            }
            CurrentAccount account = opt.get();
            Boolean oldStatus = account.getNetBankingEnabled() != null ? account.getNetBankingEnabled() : true;
            account.setNetBankingEnabled(enabled);
            account.setNetBankingToggledBy(changedBy);
            account.setNetBankingToggledAt(LocalDateTime.now());
            currentAccountRepository.save(account);

            // Record audit
            NetBankingServiceAudit audit = new NetBankingServiceAudit();
            audit.setServiceType("CURRENT_ACCOUNT");
            audit.setOldStatus(oldStatus);
            audit.setNewStatus(enabled);
            audit.setChangedBy(changedBy);
            audit.setAccountNumber(accountNumber);
            audit.setCustomerName(account.getOwnerName());
            audit.setRemarks(remarks != null ? remarks : "Individual customer toggle");
            auditRepository.save(audit);

            response.put("success", true);
            response.put("customerName", account.getOwnerName());
            response.put("accountNumber", accountNumber);
            response.put("netBankingEnabled", enabled);
            response.put("message", "Net banking for current account " + accountNumber + " (" + account.getOwnerName() + ") has been turned " + (enabled ? "ON" : "OFF"));

        } else {
            response.put("success", false);
            response.put("message", "Invalid service type. Must be SAVINGS_ACCOUNT or CURRENT_ACCOUNT");
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
