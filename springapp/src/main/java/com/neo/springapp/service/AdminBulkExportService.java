package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds consolidated PDF/Excel reports for selected retail users (savings + salary + current,
 * loans, cheques, cards, gold loans, recent transactions) for admin download.
 */
@Service
@SuppressWarnings("null")
public class AdminBulkExportService {

    private static final int MAX_USERS = 200;
    private static final int TXN_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final LoanRepository loanRepository;
    private final ChequeRepository chequeRepository;
    private final CardRepository cardRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final TransactionRepository transactionRepository;

    public AdminBulkExportService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            SalaryAccountRepository salaryAccountRepository,
            CurrentAccountRepository currentAccountRepository,
            LoanRepository loanRepository,
            ChequeRepository chequeRepository,
            CardRepository cardRepository,
            GoldLoanRepository goldLoanRepository,
            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.loanRepository = loanRepository;
        this.chequeRepository = chequeRepository;
        this.cardRepository = cardRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public byte[] buildPdf(List<String> accountNumbers, String adminName) throws IOException {
        String html = buildHtmlReport(accountNumbers, adminName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, out);
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] buildExcel(List<String> accountNumbers, String adminName) throws IOException {
        Map<String, UserBundle> cache = new LinkedHashMap<>();
        for (String acc : accountNumbers) {
            cache.put(acc, loadBundle(acc));
        }
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font hf = wb.createFont();
            hf.setBold(true);
            headerStyle.setFont(hf);

            Sheet summary = wb.createSheet("Users Summary");
            int r = 0;
            Row meta = summary.createRow(r++);
            meta.createCell(0).setCellValue("Generated (IST)");
            meta.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            meta = summary.createRow(r++);
            meta.createCell(0).setCellValue("Prepared by (Admin)");
            meta.createCell(1).setCellValue(adminName != null ? adminName : "");

            String[] sumHeaders = {"Account #", "Name", "Email", "Savings Balance", "Salary (Y/N)", "Current (Y/N)", "Loans", "Cheques", "Cards", "Gold Loans"};
            Row hrow = summary.createRow(r++);
            for (int i = 0; i < sumHeaders.length; i++) {
                Cell c = hrow.createCell(i);
                c.setCellValue(sumHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            for (String acc : accountNumbers) {
                UserBundle b = cache.get(acc);
                Row row = summary.createRow(r++);
                row.createCell(0).setCellValue(safe(b.accountNumber));
                row.createCell(1).setCellValue(safe(b.displayName));
                row.createCell(2).setCellValue(safe(b.userEmail));
                row.createCell(3).setCellValue(b.savingsBalance != null ? b.savingsBalance : 0);
                row.createCell(4).setCellValue(b.salary != null ? "Y" : "N");
                row.createCell(5).setCellValue(b.current != null ? "Y" : "N");
                row.createCell(6).setCellValue(b.loans.size());
                row.createCell(7).setCellValue(b.cheques.size());
                row.createCell(8).setCellValue(b.cards.size());
                row.createCell(9).setCellValue(b.goldLoans.size());
            }

            appendSheet(wb, "Loans", new String[]{"Account #", "Loan #", "Type", "Amount", "Status", "Tenure", "Rate"},
                    accountNumbers.stream().flatMap(a -> cache.get(a).loans.stream().map(l -> new Object[]{
                            a, l.getLoanAccountNumber(), l.getType(), l.getAmount(), l.getStatus(), l.getTenure(), l.getInterestRate()
                    })).collect(Collectors.toList()));

            appendSheet(wb, "Cheques", new String[]{"Account #", "Cheque #", "Amount", "Status", "Request Status"},
                    accountNumbers.stream().flatMap(a -> cache.get(a).cheques.stream().map(c -> new Object[]{
                            a, c.getChequeNumber(), c.getAmount(), c.getStatus(), c.getRequestStatus()
                    })).collect(Collectors.toList()));

            appendSheet(wb, "Cards", new String[]{"Account #", "Type", "Last4", "Status", "Blocked"},
                    accountNumbers.stream().flatMap(a -> cache.get(a).cards.stream().map(card -> new Object[]{
                            a, card.getCardType(), maskLast4(card.getCardNumber()), card.getStatus(), card.isBlocked()
                    })).collect(Collectors.toList()));

            appendSheet(wb, "Gold Loans", new String[]{"Account #", "Loan Acc #", "Amount", "Status"},
                    accountNumbers.stream().flatMap(a -> cache.get(a).goldLoans.stream().map(g -> new Object[]{
                            a, g.getLoanAccountNumber(), g.getLoanAmount(), g.getStatus()
                    })).collect(Collectors.toList()));

            appendSheet(wb, "Transactions", new String[]{"Account #", "Date", "Type", "Amount", "Description", "Balance"},
                    accountNumbers.stream().flatMap(a -> cache.get(a).recentTxns.stream().map(t -> new Object[]{
                            a,
                            t.getDate() != null ? t.getDate().toString() : "",
                            t.getType(),
                            t.getAmount(),
                            t.getDescription(),
                            t.getBalance()
                    })).collect(Collectors.toList()));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private void appendSheet(Workbook wb, String name, String[] headers, List<Object[]> rows) {
        Sheet sh = wb.createSheet(name);
        Row hr = sh.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            hr.createCell(i).setCellValue(headers[i]);
        }
        int rr = 1;
        for (Object[] row : rows) {
            Row xr = sh.createRow(rr++);
            for (int c = 0; c < row.length; c++) {
                Object v = row[c];
                if (v == null) xr.createCell(c).setCellValue("");
                else if (v instanceof Number) xr.createCell(c).setCellValue(((Number) v).doubleValue());
                else if (v instanceof Boolean) xr.createCell(c).setCellValue((Boolean) v);
                else xr.createCell(c).setCellValue(String.valueOf(v));
            }
        }
    }

    private String buildHtmlReport(List<String> accountNumbers, String adminName) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
        String ts = LocalDateTime.now().format(fmt);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>")
                .append("body{font-family:Arial,sans-serif;margin:24px;color:#111;}")
                .append(".seal{border:3px double #1e3a5f;padding:12px 24px;display:inline-block;border-radius:8px;font-weight:bold;color:#1e3a5f;margin:16px 0;}")
                .append("h1{color:#1e3a5f;} h2{color:#334155;margin-top:28px;border-bottom:2px solid #cbd5e1;padding-bottom:6px;}")
                .append("table{border-collapse:collapse;width:100%;margin:8px 0;font-size:11px;}")
                .append("th,td{border:1px solid #ccc;padding:6px;text-align:left;}")
                .append("th{background:#f1f5f9;}")
                .append(".meta{font-size:12px;color:#475569;margin:8px 0;}")
                .append("</style></head><body>");
        sb.append("<h1>NeoBank — Consolidated User Data Report</h1>");
        sb.append("<div class=\"seal\">OFFICIAL BANK RECORD — NeoBank</div>");
        sb.append("<div class=\"meta\"><strong>Generated:</strong> ").append(escape(ts))
                .append("</div><div class=\"meta\"><strong>Administrator:</strong> ").append(escape(adminName != null ? adminName : "—"))
                .append("</div><div class=\"meta\"><strong>Users in report:</strong> ").append(accountNumbers.size()).append("</div>");

        for (String acc : accountNumbers) {
            UserBundle b = loadBundle(acc);
            sb.append("<h2>Customer — ").append(escape(b.displayName)).append(" (").append(escape(b.accountNumber)).append(")</h2>");

            sb.append("<h3>Savings account</h3>");
            if (b.account != null) {
                sb.append("<table><tr><th>Name</th><th>Account #</th><th>Balance</th><th>Type</th><th>Phone</th><th>PAN</th></tr>");
                sb.append("<tr><td>").append(escape(b.account.getName())).append("</td><td>").append(escape(b.account.getAccountNumber()))
                        .append("</td><td>").append(b.account.getBalance() != null ? b.account.getBalance() : 0)
                        .append("</td><td>").append(escape(b.account.getAccountType()))
                        .append("</td><td>").append(escape(b.account.getPhone()))
                        .append("</td><td>").append(escape(b.account.getPan())).append("</td></tr></table>");
            } else {
                sb.append("<p><em>No savings account record.</em></p>");
            }

            sb.append("<h3>Salary account</h3>");
            if (b.salary != null) {
                sb.append("<table><tr><th>Employee</th><th>Company</th><th>Monthly salary</th><th>Balance</th><th>Status</th></tr>");
                sb.append("<tr><td>").append(escape(b.salary.getEmployeeName())).append("</td><td>").append(escape(b.salary.getCompanyName()))
                        .append("</td><td>").append(b.salary.getMonthlySalary() != null ? b.salary.getMonthlySalary() : 0)
                        .append("</td><td>").append(b.salary.getBalance() != null ? b.salary.getBalance() : 0)
                        .append("</td><td>").append(escape(b.salary.getStatus())).append("</td></tr></table>");
            } else {
                sb.append("<p><em>No salary account linked to this savings account number.</em></p>");
            }

            sb.append("<h3>Current account (business)</h3>");
            if (b.current != null) {
                sb.append("<table><tr><th>Business</th><th>Owner</th><th>Balance</th><th>GST</th><th>Status</th></tr>");
                sb.append("<tr><td>").append(escape(b.current.getBusinessName())).append("</td><td>").append(escape(b.current.getOwnerName()))
                        .append("</td><td>").append(b.current.getBalance() != null ? b.current.getBalance() : 0)
                        .append("</td><td>").append(escape(b.current.getGstNumber()))
                        .append("</td><td>").append(escape(b.current.getStatus())).append("</td></tr></table>");
            } else {
                sb.append("<p><em>No current account for this account number.</em></p>");
            }

            sb.append("<h3>Loans</h3>");
            if (!b.loans.isEmpty()) {
                sb.append("<table><tr><th>Loan #</th><th>Type</th><th>Amount</th><th>Status</th><th>Tenure</th></tr>");
                for (Loan l : b.loans) {
                    sb.append("<tr><td>").append(escape(l.getLoanAccountNumber())).append("</td><td>").append(escape(l.getType()))
                            .append("</td><td>").append(l.getAmount() != null ? l.getAmount() : 0)
                            .append("</td><td>").append(escape(l.getStatus()))
                            .append("</td><td>").append(l.getTenure() != null ? l.getTenure() : "").append("</td></tr>");
                }
                sb.append("</table>");
            } else {
                sb.append("<p><em>No loans.</em></p>");
            }

            sb.append("<h3>Cheques</h3>");
            if (!b.cheques.isEmpty()) {
                sb.append("<table><tr><th>Cheque #</th><th>Amount</th><th>Status</th><th>Request</th></tr>");
                for (Cheque c : b.cheques) {
                    sb.append("<tr><td>").append(escape(c.getChequeNumber())).append("</td><td>").append(c.getAmount() != null ? c.getAmount() : 0)
                            .append("</td><td>").append(escape(c.getStatus()))
                            .append("</td><td>").append(escape(c.getRequestStatus())).append("</td></tr>");
                }
                sb.append("</table>");
            } else {
                sb.append("<p><em>No cheques.</em></p>");
            }

            sb.append("<h3>Cards</h3>");
            if (!b.cards.isEmpty()) {
                sb.append("<table><tr><th>Type</th><th>Masked #</th><th>Status</th></tr>");
                for (Card card : b.cards) {
                    sb.append("<tr><td>").append(escape(card.getCardType())).append("</td><td>****").append(escape(maskLast4(card.getCardNumber())))
                            .append("</td><td>").append(escape(card.getStatus())).append("</td></tr>");
                }
                sb.append("</table>");
            } else {
                sb.append("<p><em>No cards.</em></p>");
            }

            sb.append("<h3>Gold loans</h3>");
            if (!b.goldLoans.isEmpty()) {
                sb.append("<table><tr><th>Loan #</th><th>Amount</th><th>Status</th></tr>");
                for (GoldLoan g : b.goldLoans) {
                    sb.append("<tr><td>").append(escape(g.getLoanAccountNumber())).append("</td><td>")
                            .append(g.getLoanAmount() != null ? g.getLoanAmount() : 0)
                            .append("</td><td>").append(escape(g.getStatus())).append("</td></tr>");
                }
                sb.append("</table>");
            } else {
                sb.append("<p><em>No gold loans.</em></p>");
            }

            sb.append("<h3>Recent transactions (last ").append(TXN_PAGE_SIZE).append(")</h3>");
            if (!b.recentTxns.isEmpty()) {
                sb.append("<table><tr><th>Date</th><th>Type</th><th>Amount</th><th>Description</th><th>Balance</th></tr>");
                for (Transaction t : b.recentTxns) {
                    sb.append("<tr><td>").append(t.getDate() != null ? escape(t.getDate().toString()) : "")
                            .append("</td><td>").append(escape(t.getType()))
                            .append("</td><td>").append(t.getAmount() != null ? t.getAmount() : 0)
                            .append("</td><td>").append(escape(t.getDescription()))
                            .append("</td><td>").append(t.getBalance() != null ? t.getBalance() : 0).append("</td></tr>");
                }
                sb.append("</table>");
            } else {
                sb.append("<p><em>No transactions.</em></p>");
            }
        }

        sb.append("<p style=\"margin-top:40px;font-size:11px;color:#64748b;\">This document was generated electronically and is valid for internal banking operations. "
                + "NeoBank — Confidential.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private UserBundle loadBundle(String accountNumber) {
        UserBundle b = new UserBundle();
        b.accountNumber = accountNumber;
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            b.userEmail = u.getEmail();
            if (u.getAccount() != null) {
                b.account = u.getAccount();
                b.displayName = u.getAccount().getName();
                b.savingsBalance = u.getAccount().getBalance();
            } else {
                b.displayName = u.getUsername();
            }
        }
        if (b.account == null) {
            Account acc = accountRepository.findByAccountNumber(accountNumber);
            if (acc != null) {
                b.account = acc;
                b.displayName = acc.getName();
                b.savingsBalance = acc.getBalance();
            }
        }
        if (b.displayName == null || b.displayName.isBlank()) {
            b.displayName = accountNumber;
        }

        b.salary = salaryAccountRepository.findByAccountNumber(accountNumber);
        b.current = currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        b.loans = loanRepository.findByAccountNumber(accountNumber);
        b.cheques = chequeRepository.findByAccountNumber(accountNumber);
        b.cards = cardRepository.findByAccountNumber(accountNumber);
        b.goldLoans = goldLoanRepository.findByAccountNumber(accountNumber);
        b.recentTxns = transactionRepository
                .findByAccountNumberOrderByDateDesc(accountNumber, PageRequest.of(0, TXN_PAGE_SIZE))
                .getContent();
        return b;
    }

    public List<String> sanitizeAccountNumbers(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(MAX_USERS)
                .collect(Collectors.toList());
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String maskLast4(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return cardNumber.substring(cardNumber.length() - 4);
    }

    private static class UserBundle {
        String accountNumber;
        String displayName;
        String userEmail;
        Account account;
        Double savingsBalance;
        SalaryAccount salary;
        CurrentAccount current;
        List<Loan> loans = List.of();
        List<Cheque> cheques = List.of();
        List<Card> cards = List.of();
        List<GoldLoan> goldLoans = List.of();
        List<Transaction> recentTxns = List.of();
    }
}
