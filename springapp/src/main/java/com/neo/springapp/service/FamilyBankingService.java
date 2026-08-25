package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    public FamilyBankingService(JointAccountInvitationRepository invitationRepository,
                                JointTransferApprovalRepository transferRepository,
                                MinorAccountApplicationRepository minorRepository,
                                GuardianLinkRepository guardianRepository,
                                UserRepository userRepository,
                                AccountService accountService,
                                JointAccountProfileRepository jointProfileRepository,
                                TransactionService transactionService) {
        this.invitationRepository = invitationRepository;
        this.transferRepository = transferRepository;
        this.minorRepository = minorRepository;
        this.guardianRepository = guardianRepository;
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.jointProfileRepository = jointProfileRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public JointAccountInvitation invite(Long inviterId, String accountNumber, String inviteeEmail) {
        requireUser(inviterId);
        User invitee = userRepository.findByEmailIgnoreCase(inviteeEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invitee email is not registered"));
        if (Objects.equals(inviterId, invitee.getId())) throw new IllegalArgumentException("You cannot invite yourself");
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) throw new IllegalArgumentException("Account not found");
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
        return invitationRepository.save(invitation);
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
        return invitationRepository.save(invitation);
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
        return transferRepository.save(approval);
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
        return transferRepository.save(transfer);
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
        return minorRepository.save(app);
    }

    public List<MinorAccountApplication> guardianApplications(Long guardianId) {
        requireUser(guardianId); return minorRepository.findByGuardianUserIdOrderByCreatedAtDesc(guardianId);
    }

    @Transactional
    public MinorAccountApplication reviewMinor(Long applicationId, String adminEmail, boolean approve, String reason) {
        if (adminEmail == null || adminEmail.isBlank()) throw new SecurityException("Admin identity is required");
        MinorAccountApplication app = minorRepository.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!"PENDING".equals(app.getStatus())) throw new IllegalArgumentException("Application is no longer pending");
        app.setStatus(approve ? "ACTIVE" : "DECLINED"); app.setReviewedBy(adminEmail); app.setReviewedAt(LocalDateTime.now()); app.setRejectionReason(reason);
        if (approve) {
            User guardian = userRepository.findById(app.getGuardianUserId()).orElseThrow();
            Account guardianAccount = guardian.getAccount();
            Account childAccount = new Account();
            childAccount.setAccountNumber("MINOR" + accountService.generateUniqueAccountNumberForNewAccount());
            childAccount.setName(app.getMinorName()); childAccount.setDob(app.getDateOfBirth().toString()); childAccount.setAge(0);
            childAccount.setAccountType("MINOR_SAVINGS"); childAccount.setStatus("ACTIVE"); childAccount.setBalance(0.0);
            childAccount.setAadharNumber("MINOR-AADHAR-" + app.getId()); childAccount.setPan("MINORPAN" + String.format("%04d", app.getId() % 10000) + "X"); childAccount.setPhone("MINOR" + app.getId());
            childAccount.setParentAccountId(guardianAccount == null ? null : guardianAccount.getId()); childAccount.setOccupation("Minor");
            Account savedAccount = accountService.saveAccount(childAccount);
            User child = new User(); child.setUsername(app.getMinorName()); child.setEmail("minor." + app.getId() + "@neobank.local"); child.setStatus("APPROVED"); child.setAccount(savedAccount); child.setParentUser(guardian); userRepository.save(child);
            GuardianLink link = new GuardianLink(); link.setGuardianUserId(app.getGuardianUserId()); link.setChildUserId(child.getId()); link.setStatus("ACTIVE"); guardianRepository.save(link);
        }
        return minorRepository.save(app);
    }

    public List<MinorAccountApplication> pendingMinorApplications() {
        return minorRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    public List<GuardianLink> guardianLinks(Long guardianId) { requireUser(guardianId); return guardianRepository.findByGuardianUserIdAndStatus(guardianId, "ACTIVE"); }

    private boolean isJointOwner(Long userId, String accountNumber) {
        return invitationRepository.findByAccountNumberAndStatus(accountNumber, "ACCEPTED").stream()
                .anyMatch(i -> Objects.equals(i.getInviterUserId(), userId) || Objects.equals(i.getInviteeUserId(), userId));
    }
    private void requireUser(Long id) { if (id == null || !userRepository.existsById(id)) throw new SecurityException("Authenticated user is required"); }
    private void requireOwner(Long actual, Long expected) { requireUser(actual); if (!Objects.equals(actual, expected)) throw new SecurityException("Not authorized for this resource"); }
    private void validateAmount(Double amount) { if (amount == null || amount <= 0 || amount > 1_000_000) throw new IllegalArgumentException("Invalid amount"); }
}
