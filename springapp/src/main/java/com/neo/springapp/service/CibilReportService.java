package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

@Service
public class CibilReportService {

    private final CibilReportRepository cibilReportRepository;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final UserRepository userRepository;
    private final CreditScorePredictorService creditScorePredictorService;

    public CibilReportService(CibilReportRepository cibilReportRepository,
                              AccountRepository accountRepository,
                              SalaryAccountRepository salaryAccountRepository,
                              CurrentAccountRepository currentAccountRepository,
                              UserRepository userRepository,
                              CreditScorePredictorService creditScorePredictorService) {
        this.cibilReportRepository = cibilReportRepository;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.userRepository = userRepository;
        this.creditScorePredictorService = creditScorePredictorService;
    }

    // ==================== EXCEL UPLOAD ====================

    @Transactional
    public Map<String, Object> parseAndSaveExcel(MultipartFile file, String uploadedBy) {
        Map<String, Object> result = new HashMap<>();
        String batchId = "CIBIL_" + System.currentTimeMillis();
        int savedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.put("success", false);
                result.put("error", "Empty spreadsheet — no header row found");
                return result;
            }

            // Map column headers
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = getCellStringValue(cell).toLowerCase().replaceAll("[^a-z0-9]", "_").trim();
                    columnMap.put(header, i);
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String pan = cleanUpperCase(getColumnValue(row, columnMap, "pan_number", "pan", "pannumber", "pan_no", "pancard"));
                    String name = getColumnValue(row, columnMap, "name", "full_name", "fullname", "customer_name", "user_name");

                    if (pan == null || pan.isEmpty() || !isValidPan(pan)) {
                        skippedCount++;
                        errors.add("Row " + (i + 1) + ": Invalid or missing PAN number");
                        continue;
                    }
                    if (name == null || name.isEmpty()) {
                        skippedCount++;
                        errors.add("Row " + (i + 1) + ": Missing name");
                        continue;
                    }

                    Double salary = getColumnDoubleValue(row, columnMap, "salary", "monthly_salary", "income", "monthly_income", "annual_salary");
                    Integer cibilScore = getColumnIntValue(row, columnMap, "cibil_score", "cibil", "credit_score", "score");
                    Double approvalLimit = getColumnDoubleValue(row, columnMap, "approval_limit", "limit", "credit_limit", "approved_limit");
                    String status = cleanUpperCase(getColumnValue(row, columnMap, "status", "approval_status", "result", "eligibility"));

                    // Normalize status
                    if (status != null) {
                        status = normalizeStatus(status);
                    }

                    String remarks = getColumnValue(row, columnMap, "remarks", "reason", "comment", "notes");

                    CibilReport report = new CibilReport();
                    report.setPanNumber(pan);
                    report.setName(name);
                    report.setSalary(salary);
                    report.setCibilScore(cibilScore);
                    report.setApprovalLimit(approvalLimit);
                    report.setStatus(status != null ? status : "PENDING");
                    report.setRemarks(remarks);
                    report.setUploadedBy(uploadedBy);
                    report.setUploadBatchId(batchId);
                    report.setUploadFileName(file.getOriginalFilename());
                    report.setUploadType("EXCEL");

                    // Cross-reference accounts and run ML analysis
                    crossReferenceAccounts(report);
                    runMlAnalysis(report);

                    cibilReportRepository.save(report);
                    savedCount++;
                } catch (Exception e) {
                    skippedCount++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("batchId", batchId);
            result.put("savedCount", savedCount);
            result.put("skippedCount", skippedCount);
            result.put("totalRows", sheet.getLastRowNum());
            if (!errors.isEmpty()) result.put("errors", errors.subList(0, Math.min(errors.size(), 20)));
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to parse Excel: " + e.getMessage());
        }
        return result;
    }

    // ==================== PDF UPLOAD ====================

    @Transactional
    public Map<String, Object> parseAndSavePdf(MultipartFile file, String uploadedBy) {
        Map<String, Object> result = new HashMap<>();
        String batchId = "CIBIL_PDF_" + System.currentTimeMillis();
        int savedCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            byte[] bytes = file.getBytes();
            PDDocument document = Loader.loadPDF(bytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();

            // Parse CIBIL report text — extract PAN-based records
            List<Map<String, String>> records = extractRecordsFromText(text);

            if (records.isEmpty()) {
                result.put("success", false);
                result.put("error", "No valid CIBIL records found in PDF. Ensure it contains PAN numbers and credit data.");
                return result;
            }

            for (Map<String, String> record : records) {
                try {
                    String pan = cleanUpperCase(record.get("pan"));
                    if (pan == null || !isValidPan(pan)) continue;

                    CibilReport report = new CibilReport();
                    report.setPanNumber(pan);
                    report.setName(record.getOrDefault("name", "Unknown"));
                    report.setSalary(parseDoubleSafe(record.get("salary")));
                    report.setCibilScore(parseIntSafe(record.get("cibilScore")));
                    report.setApprovalLimit(parseDoubleSafe(record.get("approvalLimit")));
                    report.setStatus(normalizeStatus(record.getOrDefault("status", "PENDING")));
                    report.setRemarks(record.get("remarks"));
                    report.setUploadedBy(uploadedBy);
                    report.setUploadBatchId(batchId);
                    report.setUploadFileName(file.getOriginalFilename());
                    report.setUploadType("PDF");

                    crossReferenceAccounts(report);
                    runMlAnalysis(report);

                    cibilReportRepository.save(report);
                    savedCount++;
                } catch (Exception e) {
                    errors.add("Record: " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("batchId", batchId);
            result.put("savedCount", savedCount);
            result.put("extractedRecords", records.size());
            if (!errors.isEmpty()) result.put("errors", errors);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to parse PDF: " + e.getMessage());
        }
        return result;
    }

    // ==================== IMAGE UPLOAD (store as manual entry) ====================

    @Transactional
    public Map<String, Object> saveImageUpload(MultipartFile file, String uploadedBy, Map<String, String> manualData) {
        Map<String, Object> result = new HashMap<>();
        String batchId = "CIBIL_IMG_" + System.currentTimeMillis();

        try {
            // Save image file
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "cibil-reports" + File.separator;
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String fileName = batchId + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            File dest = new File(dir, fileName);
            file.transferTo(dest);

            // Create record from manual data provided with image
            String pan = cleanUpperCase(manualData.get("panNumber"));
            if (pan == null || !isValidPan(pan)) {
                result.put("success", false);
                result.put("error", "Valid PAN number is required with image upload");
                return result;
            }

            CibilReport report = new CibilReport();
            report.setPanNumber(pan);
            report.setName(manualData.getOrDefault("name", "Unknown"));
            report.setSalary(parseDoubleSafe(manualData.get("salary")));
            report.setCibilScore(parseIntSafe(manualData.get("cibilScore")));
            report.setApprovalLimit(parseDoubleSafe(manualData.get("approvalLimit")));
            report.setStatus(normalizeStatus(manualData.getOrDefault("status", "PENDING")));
            report.setRemarks(manualData.get("remarks"));
            report.setUploadedBy(uploadedBy);
            report.setUploadBatchId(batchId);
            report.setUploadFileName(fileName);
            report.setUploadType("IMAGE");

            crossReferenceAccounts(report);
            runMlAnalysis(report);

            cibilReportRepository.save(report);

            result.put("success", true);
            result.put("batchId", batchId);
            result.put("savedCount", 1);
            result.put("report", report);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to process image: " + e.getMessage());
        }
        return result;
    }

    // ==================== PAN LOOKUP (for credit card apply) ====================

    public Map<String, Object> lookupByPan(String panNumber) {
        Map<String, Object> result = new HashMap<>();
        String pan = cleanUpperCase(panNumber);

        if (pan == null || !isValidPan(pan)) {
            result.put("success", false);
            result.put("error", "Invalid PAN number format");
            return result;
        }

        Optional<CibilReport> reportOpt = cibilReportRepository.findFirstByPanNumberOrderByCreatedAtDesc(pan);
        if (reportOpt.isEmpty()) {
            result.put("success", false);
            result.put("found", false);
            result.put("error", "No CIBIL report found for this PAN number");
            return result;
        }

        CibilReport report = reportOpt.get();

        // Refresh cross-references with latest account data
        crossReferenceAccounts(report);
        runMlAnalysis(report);
        cibilReportRepository.save(report);

        result.put("success", true);
        result.put("found", true);

        // CIBIL data
        result.put("panNumber", report.getPanNumber());
        result.put("name", report.getName());
        result.put("salary", report.getSalary());
        result.put("cibilScore", report.getCibilScore());
        result.put("approvalLimit", report.getApprovalLimit());
        result.put("status", report.getStatus());
        result.put("remarks", report.getRemarks());

        // Account cross-references
        Map<String, Object> accounts = new HashMap<>();
        if (report.getSavingsAccountNumber() != null) {
            accounts.put("savings", Map.of(
                "accountNumber", report.getSavingsAccountNumber(),
                "balance", report.getSavingsBalance() != null ? report.getSavingsBalance() : 0
            ));
        }
        if (report.getSalaryAccountNumber() != null) {
            accounts.put("salary", Map.of(
                "accountNumber", report.getSalaryAccountNumber(),
                "balance", report.getSalaryBalance() != null ? report.getSalaryBalance() : 0
            ));
        }
        if (report.getCurrentAccountNumber() != null) {
            accounts.put("current", Map.of(
                "accountNumber", report.getCurrentAccountNumber(),
                "balance", report.getCurrentBalance() != null ? report.getCurrentBalance() : 0
            ));
        }
        result.put("accounts", accounts);

        // ML analysis
        Map<String, Object> mlAnalysis = new HashMap<>();
        mlAnalysis.put("riskScore", report.getRiskScore());
        mlAnalysis.put("riskCategory", report.getRiskCategory());
        mlAnalysis.put("debtToIncomeRatio", report.getDebtToIncomeRatio());
        mlAnalysis.put("recommendedLimit", report.getRecommendedLimit());
        mlAnalysis.put("eligibilityReason", report.getEligibilityReason());

        // Get ML prediction from CreditScorePredictorService
        Map<String, Object> prediction = creditScorePredictorService.getCreditCardPrediction(pan, report.getSalary());
        mlAnalysis.put("predictedCibil", prediction.get("predictedCibil"));
        mlAnalysis.put("panBasedLimit", prediction.get("panBasedLimit"));
        mlAnalysis.put("cibilBasedLimit", prediction.get("cibilBasedLimit"));
        mlAnalysis.put("suggestedLimit", prediction.get("suggestedLimit"));
        mlAnalysis.put("eligibility", prediction.get("eligibility"));
        mlAnalysis.put("cardType", prediction.get("cardType"));
        mlAnalysis.put("interestRate", prediction.get("interestRate"));
        mlAnalysis.put("annualFee", prediction.get("annualFee"));

        result.put("mlAnalysis", mlAnalysis);

        return result;
    }

    // ==================== CROSS-REFERENCE ACCOUNTS ====================

    private void crossReferenceAccounts(CibilReport report) {
        String pan = report.getPanNumber();

        final String[] savingsHolderName = { null };

        // Check savings accounts (via User → Account)
        userRepository.findByPan(pan).ifPresent(user -> {
            Account acc = user.getAccount();
            if (acc != null) {
                report.setSavingsAccountNumber(acc.getAccountNumber());
                report.setSavingsBalance(acc.getBalance());
                savingsHolderName[0] = acc.getName();
            }
        });

        // Salary account: same PAN may exist for savings + salary — only link both when holder names match
        SalaryAccount salAcc = salaryAccountRepository.findByPanNumber(pan);
        if (salAcc != null) {
            String savingsName = savingsHolderName[0];
            String employeeName = salAcc.getEmployeeName();
            boolean linkSalaryWithSavings;
            if (savingsName == null || savingsName.isBlank()) {
                linkSalaryWithSavings = true;
            } else {
                linkSalaryWithSavings = employeeName != null && !employeeName.isBlank()
                        && normalizePersonName(savingsName).equals(normalizePersonName(employeeName));
            }
            if (linkSalaryWithSavings) {
                report.setSalaryAccountNumber(salAcc.getAccountNumber());
                report.setSalaryBalance(salAcc.getBalance());
                if (report.getSalary() == null && salAcc.getMonthlySalary() != null) {
                    report.setSalary(salAcc.getMonthlySalary());
                }
            } else {
                report.setSalaryAccountNumber(null);
                report.setSalaryBalance(null);
            }
        }

        // Check current accounts
        currentAccountRepository.findByPanNumber(pan).ifPresent(ca -> {
            report.setCurrentAccountNumber(ca.getAccountNumber());
            report.setCurrentBalance(ca.getBalance());
        });
    }

    /** Trim, lowercase, collapse internal whitespace for comparing account holder vs salary employee name. */
    private static String normalizePersonName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    // ==================== ML ANALYSIS (Rule-based ML Simulation) ====================

    private void runMlAnalysis(CibilReport report) {
        double riskScore = 0.0;
        StringBuilder reason = new StringBuilder();

        int cibil = report.getCibilScore() != null ? report.getCibilScore() : 0;
        double salary = report.getSalary() != null ? report.getSalary() : 0;
        double totalBalance = 0;
        if (report.getSavingsBalance() != null) totalBalance += report.getSavingsBalance();
        if (report.getSalaryBalance() != null) totalBalance += report.getSalaryBalance();
        if (report.getCurrentBalance() != null) totalBalance += report.getCurrentBalance();

        // Feature 1: CIBIL Score weight (40%)
        if (cibil >= 750) {
            riskScore += 10;
            reason.append("Excellent CIBIL (").append(cibil).append("). ");
        } else if (cibil >= 700) {
            riskScore += 25;
            reason.append("Good CIBIL (").append(cibil).append("). ");
        } else if (cibil >= 650) {
            riskScore += 45;
            reason.append("Fair CIBIL (").append(cibil).append("). ");
        } else if (cibil >= 600) {
            riskScore += 65;
            reason.append("Below average CIBIL (").append(cibil).append("). ");
        } else if (cibil > 0) {
            riskScore += 85;
            reason.append("Poor CIBIL (").append(cibil).append("). ");
        } else {
            riskScore += 50;
            reason.append("No CIBIL score available. ");
        }

        // Feature 2: Income-to-limit ratio (25%)
        double approvalLimit = report.getApprovalLimit() != null ? report.getApprovalLimit() : 0;
        if (salary > 0 && approvalLimit > 0) {
            double ratio = approvalLimit / (salary * 12);
            report.setDebtToIncomeRatio(Math.round(ratio * 100.0) / 100.0);
            if (ratio <= 0.3) {
                riskScore -= 5;
                reason.append("Low debt-to-income ratio (").append(String.format("%.0f%%", ratio * 100)).append("). ");
            } else if (ratio <= 0.5) {
                riskScore += 5;
                reason.append("Moderate debt-to-income ratio. ");
            } else {
                riskScore += 20;
                reason.append("High debt-to-income ratio. ");
            }
        }

        // Feature 3: Account portfolio (20%)
        int accountCount = 0;
        if (report.getSavingsAccountNumber() != null) accountCount++;
        if (report.getSalaryAccountNumber() != null) accountCount++;
        if (report.getCurrentAccountNumber() != null) accountCount++;

        if (accountCount >= 3) {
            riskScore -= 10;
            reason.append("Strong account portfolio (3 accounts). ");
        } else if (accountCount >= 2) {
            riskScore -= 5;
            reason.append("Good account portfolio (2 accounts). ");
        } else if (accountCount == 1) {
            riskScore += 5;
            reason.append("Single account holder. ");
        } else {
            riskScore += 15;
            reason.append("No linked bank accounts found. ");
        }

        // Feature 4: Balance stability (15%)
        if (totalBalance >= 500000) {
            riskScore -= 10;
            reason.append("High combined balance. ");
        } else if (totalBalance >= 100000) {
            riskScore -= 5;
            reason.append("Moderate combined balance. ");
        } else if (totalBalance > 0) {
            riskScore += 5;
            reason.append("Low combined balance. ");
        }

        // Clamp risk score to 0-100
        riskScore = Math.max(0, Math.min(100, riskScore));
        report.setRiskScore(Math.round(riskScore * 100.0) / 100.0);

        // Risk category
        if (riskScore <= 25) {
            report.setRiskCategory("LOW");
        } else if (riskScore <= 50) {
            report.setRiskCategory("MEDIUM");
        } else if (riskScore <= 75) {
            report.setRiskCategory("HIGH");
        } else {
            report.setRiskCategory("VERY_HIGH");
        }

        // Recommended limit calculation
        double recommendedLimit = 0;
        if (salary > 0) {
            recommendedLimit = salary * 3; // Base: 3x monthly salary
            if (cibil >= 750) recommendedLimit *= 1.5;
            else if (cibil >= 700) recommendedLimit *= 1.2;
            else if (cibil >= 650) recommendedLimit *= 1.0;
            else if (cibil >= 600) recommendedLimit *= 0.7;
            else recommendedLimit *= 0.4;

            // Portfolio bonus
            recommendedLimit *= (1 + accountCount * 0.1);
        }
        report.setRecommendedLimit((double) Math.round(recommendedLimit));

        // Auto-determine status if PENDING
        if ("PENDING".equals(report.getStatus())) {
            if (cibil >= 700 && riskScore <= 35) {
                report.setStatus("APPROVED");
                reason.append("Auto-approved: Good CIBIL and low risk. ");
            } else if (cibil < 550 || riskScore > 70) {
                report.setStatus("NOT_ELIGIBLE");
                reason.append("Not eligible: Poor creditworthiness indicators. ");
            } else if (cibil < 600 || riskScore > 55) {
                report.setStatus("REJECTED");
                reason.append("Rejected: Insufficient credit score. ");
            }
        }

        report.setEligibilityReason(reason.toString().trim());
    }

    // ==================== PDF TEXT EXTRACTION HELPERS ====================

    private List<Map<String, String>> extractRecordsFromText(String text) {
        List<Map<String, String>> records = new ArrayList<>();

        // Pattern: Look for PAN numbers (ABCDE1234F format)
        Pattern panPattern = Pattern.compile("([A-Z]{5}[0-9]{4}[A-Z])");
        Matcher panMatcher = panPattern.matcher(text.toUpperCase());

        // Try line-by-line parsing
        String[] lines = text.split("\\n");
        for (String line : lines) {
            Matcher linePanMatcher = panPattern.matcher(line.toUpperCase());
            if (linePanMatcher.find()) {
                Map<String, String> record = new HashMap<>();
                record.put("pan", linePanMatcher.group(1));

                // Extract name (words before or after PAN)
                String remaining = line.replaceAll(linePanMatcher.group(1), "").trim();

                // Extract numbers that could be salary/score/limit
                Pattern numberPattern = Pattern.compile("\\b(\\d{3,})\\b");
                Matcher numMatcher = numberPattern.matcher(remaining);
                List<String> numbers = new ArrayList<>();
                while (numMatcher.find()) numbers.add(numMatcher.group(1));

                // Remove numbers from remaining to get name
                String nameCandidate = remaining.replaceAll("\\b\\d+\\b", "").replaceAll("[^a-zA-Z\\s]", "").trim();
                if (!nameCandidate.isEmpty()) record.put("name", nameCandidate);

                // Heuristic: first 3-digit number could be CIBIL, larger numbers could be salary/limit
                for (String num : numbers) {
                    int val = Integer.parseInt(num);
                    if (val >= 300 && val <= 900 && !record.containsKey("cibilScore")) {
                        record.put("cibilScore", num);
                    } else if (val > 5000 && !record.containsKey("salary")) {
                        record.put("salary", num);
                    } else if (val > 10000 && !record.containsKey("approvalLimit")) {
                        record.put("approvalLimit", num);
                    }
                }

                // Check for status keywords
                String upper = line.toUpperCase();
                if (upper.contains("APPROVED") || upper.contains("APPROVE")) record.put("status", "APPROVED");
                else if (upper.contains("REJECTED") || upper.contains("REJECT")) record.put("status", "REJECTED");
                else if (upper.contains("NOT ELIGIBLE") || upper.contains("INELIGIBLE")) record.put("status", "NOT_ELIGIBLE");

                records.add(record);
            }
        }

        return records;
    }

    // ==================== TEMPLATE GENERATION ====================

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CIBIL Reports");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"PAN Number", "Name", "Salary", "CIBIL Score", "Approval Limit", "Status", "Remarks"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Sample row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("ABCDE1234F");
            sampleRow.createCell(1).setCellValue("John Doe");
            sampleRow.createCell(2).setCellValue(50000);
            sampleRow.createCell(3).setCellValue(750);
            sampleRow.createCell(4).setCellValue(200000);
            sampleRow.createCell(5).setCellValue("APPROVED");
            sampleRow.createCell(6).setCellValue("Good credit history");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ==================== STATISTICS ====================

    public Map<String, Object> getStatistics(String uploadedBy) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", cibilReportRepository.countByUploadedBy(uploadedBy));
        stats.put("approved", cibilReportRepository.countByUploadedByAndStatus(uploadedBy, "APPROVED"));
        stats.put("rejected", cibilReportRepository.countByUploadedByAndStatus(uploadedBy, "REJECTED"));
        stats.put("notEligible", cibilReportRepository.countByUploadedByAndStatus(uploadedBy, "NOT_ELIGIBLE"));
        stats.put("pending", cibilReportRepository.countByUploadedByAndStatus(uploadedBy, "PENDING"));
        return stats;
    }

    // ==================== UTILITY METHODS ====================

    private boolean isValidPan(String pan) {
        return pan != null && pan.matches("[A-Z]{5}[0-9]{4}[A-Z]");
    }

    private String normalizeStatus(String status) {
        if (status == null) return "PENDING";
        status = status.toUpperCase().trim();
        if (status.contains("APPROVED") || status.equals("APPROVE") || status.equals("YES")) return "APPROVED";
        if (status.contains("REJECTED") || status.equals("REJECT") || status.equals("NO")) return "REJECTED";
        if (status.contains("NOT") || status.contains("INELIGIBLE")) return "NOT_ELIGIBLE";
        if (status.contains("PENDING") || status.contains("REVIEW")) return "PENDING";
        return "PENDING";
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getLocalDateTimeCellValue().toString();
                double num = cell.getNumericCellValue();
                yield num == Math.floor(num) ? String.valueOf((long) num) : String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private String getColumnValue(Row row, Map<String, Integer> columnMap, String... aliases) {
        for (String alias : aliases) {
            Integer idx = columnMap.get(alias);
            if (idx != null) {
                String val = getCellStringValue(row.getCell(idx));
                if (!val.isEmpty()) return val;
            }
        }
        return null;
    }

    private Double getColumnDoubleValue(Row row, Map<String, Integer> columnMap, String... aliases) {
        String val = getColumnValue(row, columnMap, aliases);
        return parseDoubleSafe(val);
    }

    private Integer getColumnIntValue(Row row, Map<String, Integer> columnMap, String... aliases) {
        String val = getColumnValue(row, columnMap, aliases);
        return parseIntSafe(val);
    }

    private String cleanUpperCase(String s) {
        return s != null ? s.trim().toUpperCase() : null;
    }

    private Double parseDoubleSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return null; }
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return (int) Double.parseDouble(s.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return null; }
    }
}
