package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.service.FamilyBankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
public class FamilyBankingController {
    private final FamilyBankingService service;
    public FamilyBankingController(FamilyBankingService service) { this.service = service; }

    @PostMapping("/invitations")
    public ResponseEntity<JointAccountInvitation> invite(@RequestBody InvitationRequest request) {
        return ResponseEntity.ok(service.invite(request.userId(), request.accountNumber(), request.inviteeEmail()));
    }
    @GetMapping("/invitations")
    public List<JointAccountInvitation> invitations(@RequestParam Long userId) { return service.invitations(userId); }
    @PostMapping("/invitations/{id}/respond")
    public ResponseEntity<JointAccountInvitation> respond(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.respondToInvitation(request.userId(), id, request.approve()));
    }
    @GetMapping("/joint-accounts")
    public List<JointAccountProfile> jointAccounts(@RequestParam Long userId) { return service.jointAccounts(userId); }

    @PostMapping("/transfers")
    public ResponseEntity<JointTransferApproval> requestTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(service.requestTransfer(request.userId(), request.accountNumber(), request.toAccountNumber(), request.amount(), request.note()));
    }
    @GetMapping("/transfers/pending")
    public List<JointTransferApproval> pendingTransfers(@RequestParam Long userId) { return service.pendingTransfers(userId); }
    @PostMapping("/transfers/{id}/decide")
    public ResponseEntity<JointTransferApproval> decideTransfer(@PathVariable Long id, @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.decideTransfer(request.userId(), id, request.approve()));
    }

    @PostMapping("/minor-applications")
    public ResponseEntity<MinorAccountApplication> apply(@RequestBody MinorRequest request) {
        return ResponseEntity.ok(service.applyForMinor(request.guardianUserId(), request.minorName(), request.dateOfBirth(), request.monthlyLimit(), request.dailyLimit()));
    }
    @GetMapping("/minor-applications")
    public List<MinorAccountApplication> applications(@RequestParam Long guardianUserId) { return service.guardianApplications(guardianUserId); }
    @GetMapping("/guardian-links")
    public List<GuardianLink> links(@RequestParam Long guardianUserId) { return service.guardianLinks(guardianUserId); }
    @GetMapping("/account-lookup/{accountNumber}")
    public Map<String, Object> accountLookup(@PathVariable String accountNumber, @RequestParam Long userId) { return service.lookupAccount(userId, accountNumber); }
    @GetMapping("/notifications")
    public List<FamilyBankingNotification> notifications(@RequestParam Long userId) { return service.notifications(userId); }
    @PostMapping("/notifications/{id}/read")
    public FamilyBankingNotification markNotificationRead(@PathVariable Long id, @RequestBody DecisionRequest request) { return service.markNotificationRead(request.userId(), id); }
    @GetMapping("/joint-accounts/{accountNumber}/history")
    public List<Transaction> history(@PathVariable String accountNumber, @RequestParam Long userId) { return service.jointHistory(userId, accountNumber); }
    @PatchMapping("/joint-accounts/{accountNumber}/settings")
    public JointAccountProfile settings(@PathVariable String accountNumber, @RequestBody SettingsRequest request) { return service.updateJointSettings(request.userId(), accountNumber, request.operatingMode()); }
    @PostMapping("/admin/minor-applications/{id}/review")
    public ResponseEntity<MinorAccountApplication> review(@PathVariable Long id, @RequestHeader("X-Admin-Email") String adminEmail, @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(service.reviewMinor(id, adminEmail, request.approve(), request.reason()));
    }
    @GetMapping("/admin/minor-applications")
    public List<MinorAccountApplication> pendingApplications(@RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) throw new SecurityException("Admin identity is required. Please sign in again.");
        return service.pendingMinorApplications();
    }

    public record InvitationRequest(Long userId, String accountNumber, String inviteeEmail) {}
    public record DecisionRequest(Long userId, boolean approve, String reason) {}
    public record TransferRequest(Long userId, String accountNumber, String toAccountNumber, Double amount, String note) {}
    public record MinorRequest(Long guardianUserId, String minorName, LocalDate dateOfBirth, Double monthlyLimit, Double dailyLimit) {}
    public record SettingsRequest(Long userId, String operatingMode) {}
}
