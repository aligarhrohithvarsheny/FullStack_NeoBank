package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FamilyBankingService {
    private final JointAccountInvitationRepository invitationRepository;
    private final JointTransferApprovalRepository transferRepository;
    private final MinorAccountApplicationRepository minorRepository;
    private final GuardianLinkRepository guardianRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final JointAccountProfileRepository jointProfileRepository;
    private final TransactionService transactionService;
    private final FamilyBankingNotificationRepository notificationRepository;
    private final FamilyBankingAuditLogRepository auditRepository;
    private final TransactionRepository transactionRepository;

    public FamilyBankingService(JointAccountInvitationRepository invitationRepository,
                                JointTransferApprovalRepository transferRepository,
                                MinorAccountApplicationRepository minorRepository,
                                GuardianLinkRepository guardianRepository,
                                UserRepository userRepository,
                                AccountService accountService,
                                JointAccountProfileRepository jointProfileRepository,
                                TransactionService transactionService,
                                FamilyBankingNotificationRepository notificationRepository,
                                FamilyBankingAuditLogRepository auditRepository,
                                TransactionRepository transactionRepository) {
        this.invitationRepository = invitationRepository;
        this.transferRepository = transferRepository;
        this.minorRepository = minorRepository;
        this.guardianRepository = guardianRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.jointProfileRepository = jointProfileRepository;
        this.transactionService = transactionService;
        this.notificationRepository = notificationRepository;
        this.auditRepository = auditRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public JointAccountInvitation invite(Long inviterId, String accountNumber, String inviteeEmail) {
        return invite(inviterId, accountNumber, inviteeEmail, null);
    }

    @Transactional
    public JointAccountInvitation invite(Long inviterId, String accountNumber, String inviteeEmail, String expectedName) {
        requireUser(inviterId);
        User invitee = userRepository.findByEmailIgnoreCase(inviteeEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invitee email is not registered"));
        if (Objects.equals(inviterId, invitee.getId())) throw new IllegalArgumentException("You cannot invite yourself");
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) throw new IllegalArgumentException("Account not found");

        // Verify name matching: the provided name must match the invitee's account/user name
        if (expectedName != null && !expectedName.isBlank()) {
            String provided = expectedName.trim().replaceAll("\\s+", " ").toLowerCase();
            String inviteeAccountName = invitee.getAccount() != null && invitee.getAccount().getName() != null
                    ? invitee.getAccount().getName().trim().replaceAll("\\s+", " ").toLowerCase() : "";
            String inviteeUsername = invitee.getUsername() != null
                    ? invitee.getUsername().trim().replaceAll("\\s+", " ").toLowerCase() : "";
            if (!provided.equals(inviteeAccountName) && !provided.equals(inviteeUsername)) {
                throw new IllegalArgumentException("Name does not match the account holder. Please enter the exact registered name.");
            }
        }

        User inviter = userRepository.findById(inviterId).orElseThrow(() -> new SecurityException("Authenticated user is required"));
        if (inviter.getAccount() == null || !accountNumber.equals(inviter.getAccount().getAccountNumber()))
            throw new SecurityException("Only the account owner can create a joint invitation");
        if (invitationRepository.findByAccountNumberAndStatus(accountNumber, "ACCEPTED").stream()
                .anyMatch(i -> Objects.equals(i.getInviterUserId(), inviterId) || Objects.equals(i.getInviteeUserId(), inviterId))) {
            throw new IllegalArgumentException("User is already a joint owner");
        }
        JointAccountInvitation invitation = new JointAccountInvitation();
        invitation.setAccountNumber(accountNumber);
        invitation.setInviterUserId(inviterId);
        invitation.setInviteeUserId(invitee.getId());
        invitation.setOperatingMode("JOINTLY");
        JointAccountInvitation saved = invitationRepository.save(invitation);
        notify(invitee.getId(), "JOINT_INVITATION", "Joint account invitation", "You have been invited to join account " + accountNumber + ".");
        audit(inviterId, "JOINT_INVITATION_CREATED", "INVITATION", saved.getId(), accountNumber);
        return saved;
    }

    @Transactional
    public JointAccountInvitation respondToInvitation(Long userId, Long invitationId, boolean accept) {
        JointAccountInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        requireOwner(userId, invitation.getInviteeUserId());
        if (!"PENDING".equals(invitation.getStatus())) throw new IllegalArgumentException("Invitation is no longer pending");
        invitation.setStatus(accept ? "ACCEPTED" : "DECLINED");
        invitation.setRespondedAt(LocalDateTime.now());
        if (accept) {
            String jointNumber = "JOINT" + accountService.generateUniqueAccountNumberForNewAccount();
            Account original = accountService.getAccountByNumber(invitation.getAccountNumber());
            Account joint = new Account();
            joint.setAccountNumber(jointNumber);
            joint.setName(original.getName() + " & " + userRepository.findById(invitation.getInviteeUserId()).orElseThrow().getUsername());
            joint.setAccountType("JOINT_SAVINGS");
            joint.setStatus("ACTIVE");
            joint.setBalance(0.0);
            joint.setAadharNumber("JOINT-AADHAR-" + invitation.getId());
            joint.setPan("JOINTPAN" + String.format("%04d", invitation.getId() % 10000) + "X");
            joint.setPhone("JOINT" + invitation.getId());
            accountService.saveAccount(joint);
            invitation.setJointAccountNumber(jointNumber);
            JointAccountProfile profile = new JointAccountProfile();
            profile.setJointAccountNumber(jointNumber);
            profile.setPrimaryHolderUserId(invitation.getInviterUserId());
            profile.setJointHolderUserId(invitation.getInviteeUserId());
            profile.setOperatingMode("JOINTLY");
            jointProfileRepository.save(profile);
        }
        JointAccountInvitation saved = invitationRepository.save(invitation);
        notify(invitation.getInviterUserId(), "JOINT_INVITATION_DECISION", "Joint invitation " + (accept ? "accepted" : "declined"), "Your joint account invitation was " + (accept ? "accepted." : "declined."));
        audit(userId, "JOINT_INVITATION_DECIDED", "INVITATION", invitationId, accept ? "accepted" : "declined");
        return saved;
    }

    public List<JointAccountInvitation> invitations(Long userId) {
        requireUser(userId);
        return invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(userId);
    }

    public List<JointAccountProfile> jointAccounts(Long userId) {
        requireUser(userId);
        return jointProfileRepository.findByPrimaryHolderUserIdOrJointHolderUserId(userId, userId);
    }

    @Transactional
    public JointTransferApproval requestTransfer(Long userId, String accountNumber, String toAccountNumber, Double amount, String note) {
        requireUser(userId);
        validateAmount(amount);
        if (accountService.getAccountByNumber(accountNumber) == null || accountService.getAccountByNumber(toAccountNumber) == null)
            throw new IllegalArgumentException("Source or destination account not found");
        JointAccountProfile profile = jointProfileRepository.findByJointAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Joint account not found"));
        if (!Objects.equals(profile.getPrimaryHolderUserId(), userId) && !Objects.equals(profile.getJointHolderUserId(), userId)) throw new SecurityException("User is not an approved joint owner");
        Long approver = Objects.equals(profile.getPrimaryHolderUserId(), userId) ? profile.getJointHolderUserId() : profile.getPrimaryHolderUserId();
        if (accountService.getBalanceByAccountNumber(accountNumber) < amount) throw new IllegalArgumentException("Insufficient balance");
        JointTransferApproval approval = new JointTransferApproval();
        approval.setAccountNumber(accountNumber); approval.setFromUserId(userId); approval.setApproverUserId(approver);
        approval.setToAccountNumber(toAccountNumber); approval.setAmount(amount); approval.setNote(note);
        JointTransferApproval saved = transferRepository.save(approval);
        notify(approver, "TRANSFER_APPROVAL", "Joint transfer needs approval", "A transfer of " + amount + " from " + accountNumber + " is waiting for your decision.");
        audit(userId, "JOINT_TRANSFER_REQUESTED", "TRANSFER", saved.getId(), accountNumber);
        return saved;
    }

    public List<JointTransferApproval> pendingTransfers(Long userId) {
        requireUser(userId);
        return transferRepository.findByApproverUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");
    }

    @Transactional
    public JointTransferApproval decideTransfer(Long userId, Long transferId, boolean approve) {
        JointTransferApproval transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer approval not found"));
        requireOwner(userId, transfer.getApproverUserId());
        if (!"PENDING".equals(transfer.getStatus())) throw new IllegalArgumentException("Transfer is no longer pending");
        if (approve) {
            if (accountService.getBalanceByAccountNumber(transfer.getAccountNumber()) < transfer.getAmount())
                throw new IllegalArgumentException("Insufficient balance");
            Double sourceBalance = accountService.debitBalance(transfer.getAccountNumber(), transfer.getAmount());
            Double destinationBalance = accountService.creditBalance(transfer.getToAccountNumber(), transfer.getAmount());
            transactionService.createTransferTransaction(transfer.getAccountNumber(), "Joint transfer to " + transfer.getToAccountNumber(), transfer.getAmount(), "Debit", sourceBalance);
            transactionService.createTransferTransaction(transfer.getToAccountNumber(), "Joint transfer from " + transfer.getAccountNumber(), transfer.getAmount(), "Credit", destinationBalance);
            transfer.setStatus("APPROVED");
            transfer.setTransactionReference("FAM-" + transfer.getId() + "-" + System.currentTimeMillis());
        } else transfer.setStatus("DECLINED");
        transfer.setDecidedAt(LocalDateTime.now());
        JointTransferApproval saved = transferRepository.save(transfer);
        notify(transfer.getFromUserId(), "TRANSFER_DECISION", "Joint transfer " + (approve ? "approved" : "declined"), "Your joint transfer request was " + (approve ? "approved." : "declined."));
        audit(userId, "JOINT_TRANSFER_DECIDED", "TRANSFER", transferId, approve ? "approved" : "declined");
        return saved;
    }

    @Transactional
    public MinorAccountApplication applyForMinor(Long guardianId, String minorName, LocalDate dateOfBirth, Double monthlyLimit, Double dailyLimit) {
        requireUser(guardianId);
        if (dateOfBirth == null || !dateOfBirth.isAfter(LocalDate.now().minusYears(18))) throw new IllegalArgumentException("Applicant must be a minor");
        if (monthlyLimit == null || monthlyLimit <= 0 || dailyLimit == null || dailyLimit <= 0 || dailyLimit > monthlyLimit)
            throw new IllegalArgumentException("Invalid spending limits");
        MinorAccountApplication app = new MinorAccountApplication();
        app.setGuardianUserId(guardianId); app.setMinorName(minorName); app.setDateOfBirth(dateOfBirth);
        app.setMonthlyLimit(monthlyLimit); app.setDailyLimit(dailyLimit);
        MinorAccountApplication saved = minorRepository.save(app);
        audit(guardianId, "MINOR_APPLICATION_CREATED", "MINOR_APPLICATION", saved.getId(), minorName);
        return saved;
    }

    public List<MinorAccountApplication> guardianApplications(Long guardianId) {
        requireUser(guardianId); return minorRepository.findByGuardianUserIdOrderByCreatedAtDesc(guardianId);
    }

    @Transactional
    public MinorAccountApplication reviewMinor(Long applicationId, String adminEmail, boolean approve, String reason) {
        return reviewMinor(applicationId, adminEmail, approve, reason, null, null, null, null);
    }

    @Transactional
    public MinorAccountApplication reviewMinor(Long applicationId, String adminEmail, boolean approve, String reason,
                                               String editedMinorName, LocalDate editedDob, Double editedMonthlyLimit, Double editedDailyLimit) {
        if (adminEmail == null || adminEmail.isBlank()) throw new SecurityException("Admin identity is required");
        MinorAccountApplication app = minorRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!"PENDING".equals(app.getStatus())) throw new IllegalArgumentException("Application is no longer pending");

        // Apply admin edits (before approval) and track the changes for history
        StringBuilder editDetails = new StringBuilder();
        if (editedMinorName != null && !editedMinorName.isBlank() && !editedMinorName.equals(app.getMinorName())) {
            editDetails.append("minorName: '").append(app.getMinorName()).append("' → '").append(editedMinorName).append("'; ");
            app.setMinorName(editedMinorName.trim());
        }
        if (editedDob != null && !editedDob.equals(app.getDateOfBirth())) {
            if (!editedDob.isAfter(LocalDate.now().minusYears(18))) {
                throw new IllegalArgumentException("Edited date of birth must still be a minor (under 18)");
            }
            editDetails.append("dateOfBirth: ").append(app.getDateOfBirth()).append(" → ").append(editedDob).append("; ");
            app.setDateOfBirth(editedDob);
        }
        if (editedMonthlyLimit != null && editedMonthlyLimit > 0 && !editedMonthlyLimit.equals(app.getMonthlyLimit())) {
            editDetails.append("monthlyLimit: ").append(app.getMonthlyLimit()).append(" → ").append(editedMonthlyLimit).append("; ");
            app.setMonthlyLimit(editedMonthlyLimit);
        }
        if (editedDailyLimit != null && editedDailyLimit > 0 && !editedDailyLimit.equals(app.getDailyLimit())) {
            if (editedDailyLimit > app.getMonthlyLimit()) {
                throw new IllegalArgumentException("Daily limit cannot exceed monthly limit");
            }
            editDetails.append("dailyLimit: ").append(app.getDailyLimit()).append(" → ").append(editedDailyLimit).append("; ");
            app.setDailyLimit(editedDailyLimit);
        }
        if (editDetails.length() > 0) {
            audit(app.getGuardianUserId(), "MINOR_APPLICATION_EDITED", "MINOR_APPLICATION", applicationId,
                    "edited by " + adminEmail + " before review: " + editDetails.toString().trim());
        }

        app.setStatus(approve ? "ACTIVE" : "DECLINED"); app.setReviewedBy(adminEmail); app.setReviewedAt(LocalDateTime.now()); app.setRejectionReason(reason);
        if (approve) {
            User guardian = userRepository.findById(app.getGuardianUserId()).orElseThrow();
            Account guardianAccount = guardian.getAccount();

            // Auto-generate a real unique account number and customer ID for the minor
            String minorAccountNumber = accountService.generateUniqueAccountNumberForNewAccount();

            Account childAccount = new Account();
            childAccount.setAccountNumber(minorAccountNumber);
            childAccount.setName(app.getMinorName()); childAccount.setDob(app.getDateOfBirth().toString()); childAccount.setAge(0);
            childAccount.setAccountType("MINOR_SAVINGS"); childAccount.setStatus("ACTIVE"); childAccount.setBalance(0.0);
            childAccount.setAadharNumber("MINOR-AADHAR-" + app.getId()); childAccount.setPan("MINORPAN" + String.format("%04d", app.getId() % 10000) + "X"); childAccount.setPhone("MINOR" + app.getId());
            childAccount.setParentAccountId(guardianAccount == null ? null : guardianAccount.getId()); childAccount.setOccupation("Minor");
            // Auto-generate unique 9-digit customer ID
            childAccount.setCustomerId(accountService.generateCustomerIdForAccount(childAccount));
            Account savedAccount = accountService.saveAccount(childAccount);
            app.setAssignedAccountNumber(savedAccount.getAccountNumber());
            app.setAssignedCustomerId(savedAccount.getCustomerId());

            // Auto-generate a unique login email for the minor (avoid unique constraint violations)
            String minorEmail = generateUniqueMinorEmail(app.getId(), app.getMinorName());
            User child = new User(); child.setUsername(app.getMinorName()); child.setEmail(minorEmail); child.setStatus("APPROVED"); child.setAccount(savedAccount); child.setParentUser(guardian); userRepository.save(child);
            GuardianLink link = new GuardianLink(); link.setGuardianUserId(app.getGuardianUserId()); link.setChildUserId(child.getId()); link.setStatus("ACTIVE"); guardianRepository.save(link);
        }
        MinorAccountApplication saved = minorRepository.save(app);
        notify(app.getGuardianUserId(), "MINOR_APPLICATION_DECISION", "Minor application " + (approve ? "approved" : "declined"), "The Family Banking minor application was " + (approve ? "approved." : "declined."));
        audit(app.getGuardianUserId(), approve ? "MINOR_APPLICATION_APPROVED" : "MINOR_APPLICATION_DECLINED", "MINOR_APPLICATION", applicationId,
                (approve ? "approved" : "declined") + " by " + adminEmail + (reason != null && !reason.isBlank() ? " (reason: " + reason + ")" : ""));
        return saved;
    }

    // Generate a unique email for a minor's auto-created login account
    private String generateUniqueMinorEmail(Long applicationId, String minorName) {
        String base = "minor." + applicationId;
        String candidate = base + "@neobank.local";
        int suffix = 1;
        while (userRepository.findByEmailIgnoreCase(candidate).isPresent()) {
            candidate = base + "." + suffix + "@neobank.local";
            suffix++;
        }
        return candidate;
    }

    public List<MinorAccountApplication> pendingMinorApplications() {
        return minorRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    public List<MinorAccountApplication> allMinorApplications() {
        return minorRepository.findAll();
    }

    public List<FamilyBankingAuditLog> minorHistory() {
        return auditRepository.findAll().stream()
                .filter(log -> "MINOR_APPLICATION".equals(log.getResourceType()))
                .sorted((a, b) -> b.getCreatedAt() == null || a.getCreatedAt() == null ? 0 : b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public List<GuardianLink> guardianLinks(Long guardianId) { requireUser(guardianId); return guardianRepository.findByGuardianUserIdAndStatus(guardianId, "ACTIVE"); }

    // All account numbers linked to a user: own account + joint accounts + guardian/minor child accounts
    public List<Map<String, Object>> linkedAccounts(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new SecurityException("Authenticated user is required"));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        // Own account
        if (user.getAccount() != null && user.getAccount().getAccountNumber() != null) {
            result.add(linkedAccountEntry(user.getAccount(), "OWN", "Your account"));
            seen.add(user.getAccount().getAccountNumber());
        }

        // Joint accounts (as primary or joint holder)
        for (JointAccountProfile profile : jointProfileRepository.findByPrimaryHolderUserIdOrJointHolderUserId(userId, userId)) {
            Account acc = accountService.getAccountByNumber(profile.getJointAccountNumber());
            if (acc != null && !seen.contains(acc.getAccountNumber())) {
                String relation = java.util.Objects.equals(profile.getPrimaryHolderUserId(), userId) ? "JOINT_PRIMARY" : "JOINT_HOLDER";
                result.add(linkedAccountEntry(acc, "JOINT", relation + " · " + profile.getOperatingMode()));
                seen.add(acc.getAccountNumber());
            }
        }

        // Minor child accounts (as guardian)
        for (GuardianLink link : guardianRepository.findByGuardianUserIdAndStatus(userId, "ACTIVE")) {
            userRepository.findById(link.getChildUserId()).ifPresent(child -> {
                Account acc = child.getAccount();
                if (acc != null && !seen.contains(acc.getAccountNumber())) {
                    result.add(linkedAccountEntry(acc, "MINOR", "Guardian of " + acc.getName()));
                    seen.add(acc.getAccountNumber());
                }
            });
        }

        // Guardian accounts (as minor child)
        for (GuardianLink link : guardianRepository.findByChildUserIdAndStatus(userId, "ACTIVE")) {
            userRepository.findById(link.getGuardianUserId()).ifPresent(guardian -> {
                Account acc = guardian.getAccount();
                if (acc != null && !seen.contains(acc.getAccountNumber())) {
                    result.add(linkedAccountEntry(acc, "GUARDIAN", "Guardian " + acc.getName()));
                    seen.add(acc.getAccountNumber());
                }
            });
        }

        return result;
    }

    private Map<String, Object> linkedAccountEntry(Account acc, String type, String relation) {
        Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("accountNumber", acc.getAccountNumber());
        entry.put("name", acc.getName());
        entry.put("accountType", acc.getAccountType());
        entry.put("customerId", acc.getCustomerId());
        entry.put("balance", acc.getBalance());
        entry.put("status", acc.getStatus());
        entry.put("linkType", type);
        entry.put("relation", relation);
        return entry;
    }

    public Map<String, Object> lookupAccount(Long userId, String accountNumber) {
        requireUser(userId);
        Account account = accountService.getAccountByNumber(accountNumber == null ? "" : accountNumber.trim());
        if (account == null) throw new IllegalArgumentException("Account number not found");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("accountNumber", account.getAccountNumber()); result.put("name", account.getName());
        boolean minor = account.isChildAccount() || "MINOR_SAVINGS".equalsIgnoreCase(account.getAccountType());
        result.put("accountType", minor ? "MINOR_SAVINGS" : account.getAccountType()); result.put("isMinor", minor);
        result.put("status", account.getStatus()); result.put("balance", account.getBalance());
        return result;
    }

    public List<FamilyBankingNotification> notifications(Long userId) { requireUser(userId); return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId); }

    @Transactional
    public FamilyBankingNotification markNotificationRead(Long userId, Long notificationId) {
        requireUser(userId);
        FamilyBankingNotification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        requireOwner(userId, notification.getRecipientUserId());
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public List<Transaction> jointHistory(Long userId, String accountNumber) {
        requireJointOwner(userId, accountNumber);
        return transactionRepository.findByAccountNumberOrderByDateDesc(accountNumber);
    }

    @Transactional
    public JointAccountProfile updateJointSettings(Long userId, String accountNumber, String operatingMode) {
        requireJointOwner(userId, accountNumber);
        if (!List.of("JOINTLY", "EITHER_OR_SURVIVOR").contains(operatingMode)) throw new IllegalArgumentException("Unsupported operating mode");
        JointAccountProfile profile = jointProfileRepository.findByJointAccountNumber(accountNumber).orElseThrow();
        profile.setOperatingMode(operatingMode);
        audit(userId, "JOINT_SETTINGS_UPDATED", "JOINT_ACCOUNT", profile.getId(), operatingMode);
        return jointProfileRepository.save(profile);
    }

    private boolean isJointOwner(Long userId, String accountNumber) {
        if (jointProfileRepository.findByJointAccountNumber(accountNumber)
            .map(profile -> Objects.equals(profile.getPrimaryHolderUserId(), userId)
                || Objects.equals(profile.getJointHolderUserId(), userId))
            .orElse(false)) {
            return true;
        }
        return invitationRepository.findByAccountNumberAndStatus(accountNumber, "ACCEPTED").stream()
                .anyMatch(i -> Objects.equals(i.getInviterUserId(), userId) || Objects.equals(i.getInviteeUserId(), userId));
    }
    private void requireJointOwner(Long userId, String accountNumber) { requireUser(userId); if (!isJointOwner(userId, accountNumber)) throw new SecurityException("User is not an approved joint owner"); }
    private void notify(Long userId, String type, String title, String message) { FamilyBankingNotification n = new FamilyBankingNotification(); n.setRecipientUserId(userId); n.setType(type); n.setTitle(title); n.setMessage(message); notificationRepository.save(n); }
    private void audit(Long userId, String action, String resourceType, Long resourceId, String details) { FamilyBankingAuditLog log = new FamilyBankingAuditLog(); log.setActorUserId(userId); log.setAction(action); log.setResourceType(resourceType); log.setResourceId(resourceId == null ? null : resourceId.toString()); log.setDetails(details); auditRepository.save(log); }
    private void requireUser(Long id) { if (id == null || !userRepository.existsById(id)) throw new SecurityException("Authenticated user is required"); }
    private void requireOwner(Long actual, Long expected) { requireUser(actual); if (!Objects.equals(actual, expected)) throw new SecurityException("Not authorized for this resource"); }
    private void validateAmount(Double amount) { if (amount == null || amount <= 0 || amount > 1_000_000) throw new IllegalArgumentException("Invalid amount"); }
}
