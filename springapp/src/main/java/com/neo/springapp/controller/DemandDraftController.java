package com.neo.springapp.controller;

import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.Account;
import com.neo.springapp.service.DemandDraftService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/demand-drafts")
@CrossOrigin(origins = "*")
public class DemandDraftController {
    private final DemandDraftService service;
    public DemandDraftController(DemandDraftService service) { this.service = service; }

    @GetMapping("/verify-cheque")
    public ResponseEntity<?> verifyCheque(@RequestParam String accountNumber, @RequestParam String chequeNumber) {
        Account account = service.verifyCheque(accountNumber, chequeNumber);
        if (account == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cheque was not found for this account"));
        return ResponseEntity.ok(Map.of("success", true, "accountNumber", account.getAccountNumber(), "accountHolder", account.getName(), "accountType", account.getAccountType(), "balance", account.getBalance(), "chequeNumber", chequeNumber));
    }

    @GetMapping("/account/{accountNumber}") public List<DemandDraft> getByAccount(@PathVariable String accountNumber) { return service.findByAccount(accountNumber); }
    @PostMapping("/account/{accountNumber}") public ResponseEntity<?> create(@PathVariable String accountNumber, @RequestBody DemandDraft request) { try { return ResponseEntity.ok(service.create(accountNumber, request)); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); } }
    @PutMapping("/admin/{id}") public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String,Object> request) { try { return ResponseEntity.ok(service.update(id, request, String.valueOf(request.getOrDefault("adminName", "Admin")))); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); } }
    @PostMapping("/admin/{id}/approve") public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required=false) Map<String,String> request) { try { return ResponseEntity.ok(service.approve(id, request == null ? "Admin" : request.getOrDefault("adminName", "Admin"))); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); } }
    @PostMapping("/admin/{id}/reject") public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String,String> request) { try { return ResponseEntity.ok(service.reject(id, request.getOrDefault("adminName", "Admin"), request.get("reason"))); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); } }
    @GetMapping("/admin/all") public List<DemandDraft> all() { return service.findAll(); }
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable Long id) throws Exception { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"demand-draft-" + id + ".pdf\"").contentType(MediaType.APPLICATION_PDF).body(service.pdf(id)); }
}
