package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.AdminAttendanceRepository;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.AdminSalaryPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class AdminSalaryService {

    public static final double DAILY_SALARY_RS = 850.0;

    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private AdminAttendanceRepository attendanceRepository;
    @Autowired
    private AdminSalaryPaymentRepository salaryPaymentRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired(required = false)
    private BranchAccountService branchAccountService;

    /** Create a NeoBank salary account for the admin if not already present, and return the account number. */
    private String ensureSalaryAccountForAdmin(Admin admin) {
        if (admin == null) return null;
        if (admin.getSalaryAccountNumber() != null && !admin.getSalaryAccountNumber().trim().isEmpty()) {
            return admin.getSalaryAccountNumber().trim();
        }
        // Create synthetic salary account for admin
        Account account = new Account();
        account.setName(admin.getName() != null ? admin.getName() : "Admin Employee");
        account.setStatus("ACTIVE");
        account.setAccountType("Salary");
        account.setBalance(0.0);
        String base = admin.getEmployeeId() != null && !admin.getEmployeeId().trim().isEmpty()
            ? admin.getEmployeeId().trim() : String.valueOf(admin.getId());
        String suffix = base.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
        if (suffix.length() > 12) suffix = suffix.substring(0, 12);
        account.setAadharNumber("ADMINSLRYAADHAR" + suffix);
        account.setPan("ADMINSLRYPAN" + suffix);
        int hash = Math.abs((base + "admin-salary").hashCode() % 100000000);
        account.setPhone("97" + String.format("%08d", hash));
        account.setDob("01-01-1990");
        account.setAge(30);
        account.setOccupation("Admin Employee");
        account.setAddress("NeoBank Admin Salary Account");
        Account saved = accountService.saveAccount(account);
        admin.setSalaryAccountNumber(saved.getAccountNumber());
        adminRepository.save(admin);
        return saved.getAccountNumber();
    }

    @Transactional
    public Map<String, Object> verifyAttendanceByIdCard(String idCardNumber, String managerName) {
        Map<String, Object> result = new HashMap<>();
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "ID card number is required");
            return result;
        }
        Admin admin = adminRepository.findByIdCardNumber(idCardNumber.trim());
        if (admin == null) {
            result.put("success", false);
            result.put("message", "Admin not found for this ID card number");
            return result;
        }
        if (admin.getIdCardValidTill() != null && admin.getIdCardValidTill().isBefore(LocalDateTime.now())) {
            result.put("success", false);
            result.put("message", "ID card expired. Cannot verify attendance.");
            return result;
        }

        // Ensure admin has a salary account mapped (auto-create if missing)
        String salaryAcc = ensureSalaryAccountForAdmin(admin);
        result.put("salaryAccountNumber", salaryAcc);

        LocalDate today = LocalDate.now();
        Optional<AdminAttendance> existing = attendanceRepository.findByAdminIdAndAttendanceDate(admin.getId(), today);
        AdminAttendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
        } else {
            attendance = new AdminAttendance();
            attendance.setAdminId(admin.getId());
            attendance.setAdminName(admin.getName());
            attendance.setAdminEmail(admin.getEmail());
            attendance.setIdCardNumber(admin.getIdCardNumber());
            attendance.setAttendanceDate(today);
            attendance.setPresent(true);
            attendance.setVerifiedAt(LocalDateTime.now());
            attendance.setVerifiedByManager(managerName != null ? managerName : "Manager");
            attendanceRepository.save(attendance);
        }
        result.put("success", true);
        result.put("message", "Attendance verified");
        result.put("attendance", attendance);
        return result;
    }

    /** Daily attendance summary (red/green/blue status) for all admins. */
    public Map<String, Object> getAttendanceSummary(LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate target = date != null ? date : LocalDate.now();

        List<Admin> admins = adminRepository.findAll();
        List<AdminAttendance> presentList = attendanceRepository.findByAttendanceDate(target);
        Map<Long, AdminAttendance> presentByAdminId = new HashMap<>();
        for (AdminAttendance a : presentList) {
            presentByAdminId.put(a.getAdminId(), a);
        }

        List<AdminSalaryPayment> paidList = salaryPaymentRepository.findBySalaryDateBetween(target, target);
        Map<Long, AdminSalaryPayment> paidByAdminId = new HashMap<>();
        for (AdminSalaryPayment p : paidList) {
            paidByAdminId.put(p.getAdminId(), p);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Admin a : admins) {
            if (a == null) continue;
            if (a.getRole() == null) continue;
            if (!"ADMIN".equalsIgnoreCase(a.getRole())) continue; // exclude manager

            boolean present = presentByAdminId.containsKey(a.getId());
            boolean paid = paidByAdminId.containsKey(a.getId());
            String color = paid ? "blue" : (present ? "green" : "red");
            Map<String, Object> row = new HashMap<>();
            row.put("adminId", a.getId());
            row.put("adminName", a.getName());
            row.put("adminEmail", a.getEmail());
            row.put("idCardNumber", a.getIdCardNumber());
            row.put("salaryAccountNumber", a.getSalaryAccountNumber());
            row.put("date", target);
            row.put("present", present);
            row.put("paid", paid);
            row.put("statusColor", color);
            row.put("verifiedAt", present ? presentByAdminId.get(a.getId()).getVerifiedAt() : null);
            row.put("paidAt", paid ? paidByAdminId.get(a.getId()).getPaidAt() : null);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("adminName")), String.CASE_INSENSITIVE_ORDER));
        result.put("date", target);
        result.put("dailySalaryRs", DAILY_SALARY_RS);
        result.put("rows", rows);
        return result;
    }

    /** Per-admin attendance for a whole year (for calendar view). */
    public Map<String, Object> getAdminAttendanceForYear(Long adminId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        if (adminId == null) {
            result.put("success", false);
            result.put("message", "Admin ID is required");
            return result;
        }
        int y = (year != null && year > 1900) ? year : LocalDate.now().getYear();
        LocalDate from = LocalDate.of(y, 1, 1);
        LocalDate to = LocalDate.of(y, 12, 31);

        List<AdminAttendance> attendance = attendanceRepository.findByAdminIdAndAttendanceDateBetween(adminId, from, to);
        List<AdminSalaryPayment> payments = salaryPaymentRepository.findByAdminIdAndSalaryDateBetween(adminId, from, to);

        Map<LocalDate, AdminAttendance> attByDate = new HashMap<>();
        for (AdminAttendance a : attendance) {
            attByDate.put(a.getAttendanceDate(), a);
        }
        Map<LocalDate, AdminSalaryPayment> payByDate = new HashMap<>();
        for (AdminSalaryPayment p : payments) {
            payByDate.put(p.getSalaryDate(), p);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, AdminAttendance> entry : attByDate.entrySet()) {
            LocalDate d = entry.getKey();
            boolean paid = payByDate.containsKey(d);
            Map<String, Object> row = new HashMap<>();
            row.put("date", d);
            row.put("year", d.getYear());
            row.put("month", d.getMonthValue());
            row.put("day", d.getDayOfMonth());
            row.put("present", true);
            row.put("paid", paid);
            row.put("statusColor", paid ? "blue" : "green");
            rows.add(row);
        }
        rows.sort(Comparator.comparing(r -> (LocalDate) r.get("date")));

        result.put("success", true);
        result.put("year", y);
        result.put("adminId", adminId);
        result.put("rows", rows);
        return result;
    }

    /** Pay salary for selected present days for a specific admin. */
    @Transactional
    public Map<String, Object> paySelectedDays(Long adminId, List<LocalDate> dates, String paidBy) {
        Map<String, Object> result = new HashMap<>();
        if (adminId == null || dates == null || dates.isEmpty()) {
            result.put("success", false);
            result.put("message", "Admin ID and at least one date are required");
            return result;
        }
        String managerAccountNumber = branchAccountService != null ? branchAccountService.getDepositAccountNumber()
            : BranchAccountService.DEFAULT_NEOBANK_ACCOUNT;

        int success = 0, skipped = 0, failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (LocalDate d : dates) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", d);

            Optional<AdminAttendance> attOpt = attendanceRepository.findByAdminIdAndAttendanceDate(adminId, d);
            if (attOpt.isEmpty()) {
                skipped++;
                row.put("status", "SKIPPED_NO_ATTENDANCE");
                details.add(row);
                continue;
            }
            Optional<AdminSalaryPayment> alreadyPaid = salaryPaymentRepository.findByAdminIdAndSalaryDate(adminId, d);
            if (alreadyPaid.isPresent()) {
                skipped++;
                row.put("status", "SKIPPED_ALREADY_PAID");
                details.add(row);
                continue;
            }

            Admin admin = adminRepository.findById(adminId).orElse(null);
            if (admin == null) {
                failed++;
                row.put("status", "FAILED_ADMIN_NOT_FOUND");
                details.add(row);
                continue;
            }
            String adminSalaryAcc = ensureSalaryAccountForAdmin(admin);
            if (adminSalaryAcc == null) {
                failed++;
                row.put("status", "FAILED_NO_SALARY_ACCOUNT");
                details.add(row);
                continue;
            }
            Account adminAccount = accountService.getAccountByNumber(adminSalaryAcc);
            if (adminAccount == null) {
                failed++;
                row.put("status", "FAILED_SALARY_ACCOUNT_NOT_FOUND");
                details.add(row);
                continue;
            }

            Double managerNewBalance = accountService.debitBalance(managerAccountNumber, DAILY_SALARY_RS);
            if (managerNewBalance == null) {
                failed++;
                row.put("status", "FAILED_INSUFFICIENT_MANAGER_BALANCE");
                details.add(row);
                continue;
            }

            accountService.creditBalance(adminSalaryAcc, DAILY_SALARY_RS);
            Double adminBal = accountService.getBalanceByAccountNumber(adminSalaryAcc);
            Double mgrBal = accountService.getBalanceByAccountNumber(managerAccountNumber);

            Transaction mgrDebit = new Transaction();
            mgrDebit.setAccountNumber(managerAccountNumber);
            mgrDebit.setMerchant("Salary Payout - " + admin.getName());
            mgrDebit.setAmount(DAILY_SALARY_RS);
            mgrDebit.setType("Debit");
            mgrDebit.setDescription("Daily salary paid to " + admin.getName() + " (" + adminSalaryAcc + ") for " + d);
            mgrDebit.setRecipientAccountNumber(adminSalaryAcc);
            mgrDebit.setRecipientName(admin.getName());
            mgrDebit.setUserName("Manager Branch");
            mgrDebit.setBalance(mgrBal != null ? mgrBal : managerNewBalance);
            mgrDebit.setDate(LocalDateTime.now());
            mgrDebit.setStatus("Completed");
            Transaction savedMgrDebit = transactionService.saveTransaction(mgrDebit);

            Transaction adminCredit = new Transaction();
            adminCredit.setAccountNumber(adminSalaryAcc);
            adminCredit.setMerchant("Salary Credit - " + d);
            adminCredit.setAmount(DAILY_SALARY_RS);
            adminCredit.setType("Credit");
            adminCredit.setDescription("Daily salary credited from manager branch account (" + managerAccountNumber + ") for " + d);
            adminCredit.setSourceAccountNumber(managerAccountNumber);
            adminCredit.setUserName(admin.getName());
            adminCredit.setBalance(adminBal != null ? adminBal : DAILY_SALARY_RS);
            adminCredit.setDate(LocalDateTime.now());
            adminCredit.setStatus("Completed");
            Transaction savedAdminCredit = transactionService.saveTransaction(adminCredit);

            AdminSalaryPayment payment = new AdminSalaryPayment();
            payment.setAdminId(admin.getId());
            payment.setAdminName(admin.getName());
            payment.setAdminEmail(admin.getEmail());
            payment.setSalaryDate(d);
            payment.setAmount(DAILY_SALARY_RS);
            payment.setManagerBranchAccountNumber(managerAccountNumber);
            payment.setAdminSalaryAccountNumber(adminSalaryAcc);
            payment.setPaidByManager(paidBy != null ? paidBy : "Manager");
            payment.setPaidAt(LocalDateTime.now());
            payment.setManagerDebitTransactionDbId(savedMgrDebit.getId());
            payment.setAdminCreditTransactionDbId(savedAdminCredit.getId());
            payment.setStatus("PAID");
            salaryPaymentRepository.save(payment);

            success++;
            row.put("status", "PAID");
            details.add(row);
        }

        result.put("success", true);
        result.put("adminId", adminId);
        result.put("managerBranchAccountNumber", managerAccountNumber);
        result.put("successCount", success);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("details", details);
        return result;
    }

    /** Pay salary for verified attendance, transferring from manager branch account to admin salary account. */
    @Transactional
    public Map<String, Object> payDailySalary(LocalDate date, String paidBy) {
        Map<String, Object> result = new HashMap<>();
        LocalDate target = date != null ? date : LocalDate.now();

        String managerAccountNumber = branchAccountService != null ? branchAccountService.getDepositAccountNumber()
            : BranchAccountService.DEFAULT_NEOBANK_ACCOUNT;

        List<AdminAttendance> presentList = attendanceRepository.findByAttendanceDate(target);
        int success = 0;
        int skipped = 0;
        int failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (AdminAttendance att : presentList) {
            Map<String, Object> row = new HashMap<>();
            row.put("adminId", att.getAdminId());
            row.put("adminName", att.getAdminName());
            row.put("date", target);

            Optional<AdminSalaryPayment> alreadyPaid = salaryPaymentRepository.findByAdminIdAndSalaryDate(att.getAdminId(), target);
            if (alreadyPaid.isPresent()) {
                skipped++;
                row.put("status", "SKIPPED_ALREADY_PAID");
                details.add(row);
                continue;
            }

            Admin admin = adminRepository.findById(att.getAdminId()).orElse(null);
            if (admin == null || admin.getSalaryAccountNumber() == null || admin.getSalaryAccountNumber().trim().isEmpty()) {
                failed++;
                row.put("status", "FAILED_NO_SALARY_ACCOUNT");
                details.add(row);
                continue;
            }

            String adminSalaryAcc = admin.getSalaryAccountNumber().trim();
            Account adminAccount = accountService.getAccountByNumber(adminSalaryAcc);
            if (adminAccount == null) {
                failed++;
                row.put("status", "FAILED_SALARY_ACCOUNT_NOT_FOUND");
                details.add(row);
                continue;
            }

            Double managerNewBalance = accountService.debitBalance(managerAccountNumber, DAILY_SALARY_RS);
            if (managerNewBalance == null) {
                failed++;
                row.put("status", "FAILED_INSUFFICIENT_MANAGER_BALANCE");
                details.add(row);
                continue;
            }

            accountService.creditBalance(adminSalaryAcc, DAILY_SALARY_RS);
            Double adminBal = accountService.getBalanceByAccountNumber(adminSalaryAcc);
            Double mgrBal = accountService.getBalanceByAccountNumber(managerAccountNumber);

            Transaction mgrDebit = new Transaction();
            mgrDebit.setAccountNumber(managerAccountNumber);
            mgrDebit.setMerchant("Salary Payout - " + admin.getName());
            mgrDebit.setAmount(DAILY_SALARY_RS);
            mgrDebit.setType("Debit");
            mgrDebit.setDescription("Daily salary paid to " + admin.getName() + " (" + adminSalaryAcc + ") for " + target);
            mgrDebit.setRecipientAccountNumber(adminSalaryAcc);
            mgrDebit.setRecipientName(admin.getName());
            mgrDebit.setUserName("Manager Branch");
            mgrDebit.setBalance(mgrBal != null ? mgrBal : managerNewBalance);
            mgrDebit.setDate(LocalDateTime.now());
            mgrDebit.setStatus("Completed");
            Transaction savedMgrDebit = transactionService.saveTransaction(mgrDebit);

            Transaction adminCredit = new Transaction();
            adminCredit.setAccountNumber(adminSalaryAcc);
            adminCredit.setMerchant("Salary Credit - " + target);
            adminCredit.setAmount(DAILY_SALARY_RS);
            adminCredit.setType("Credit");
            adminCredit.setDescription("Daily salary credited from manager branch account (" + managerAccountNumber + ") for " + target);
            adminCredit.setSourceAccountNumber(managerAccountNumber);
            adminCredit.setUserName(admin.getName());
            adminCredit.setBalance(adminBal != null ? adminBal : DAILY_SALARY_RS);
            adminCredit.setDate(LocalDateTime.now());
            adminCredit.setStatus("Completed");
            Transaction savedAdminCredit = transactionService.saveTransaction(adminCredit);

            AdminSalaryPayment payment = new AdminSalaryPayment();
            payment.setAdminId(admin.getId());
            payment.setAdminName(admin.getName());
            payment.setAdminEmail(admin.getEmail());
            payment.setSalaryDate(target);
            payment.setAmount(DAILY_SALARY_RS);
            payment.setManagerBranchAccountNumber(managerAccountNumber);
            payment.setAdminSalaryAccountNumber(adminSalaryAcc);
            payment.setPaidByManager(paidBy != null ? paidBy : "Manager");
            payment.setPaidAt(LocalDateTime.now());
            payment.setManagerDebitTransactionDbId(savedMgrDebit.getId());
            payment.setAdminCreditTransactionDbId(savedAdminCredit.getId());
            payment.setStatus("PAID");
            salaryPaymentRepository.save(payment);

            success++;
            row.put("status", "PAID");
            row.put("adminSalaryAccountNumber", adminSalaryAcc);
            details.add(row);
        }

        result.put("success", true);
        result.put("date", target);
        result.put("dailySalaryRs", DAILY_SALARY_RS);
        result.put("managerBranchAccountNumber", managerAccountNumber);
        result.put("successCount", success);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("details", details);
        return result;
    }
}

