package com.neo.springapp.service;

import com.neo.springapp.model.PreloadedCustomerData;
import com.neo.springapp.repository.PreloadedCustomerDataRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PreloadedCustomerDataService {

    @Autowired
    private PreloadedCustomerDataRepository repository;

    // ==================== Lookup ====================

    public Optional<PreloadedCustomerData> findByAadhar(String aadharNumber) {
        return repository.findFirstByAadharNumberAndUsedFalseOrderByCreatedAtDesc(aadharNumber);
    }

    public Optional<PreloadedCustomerData> findByPan(String panNumber) {
        return repository.findFirstByPanNumberAndUsedFalseOrderByCreatedAtDesc(panNumber);
    }

    public boolean existsByAadhar(String aadharNumber) {
        return repository.existsByAadharNumberAndUsedFalse(aadharNumber);
    }

    // ==================== Mark as Used ====================

    @Transactional
    public void markAsUsed(Long id, String usedBy) {
        repository.findById(id).ifPresent(data -> {
            data.setUsed(true);
            data.setUsedBy(usedBy);
            data.setUsedAt(LocalDateTime.now());
            repository.save(data);
        });
    }

    @Transactional
    public void markAsUsedByAadhar(String aadharNumber, String usedBy) {
        repository.findFirstByAadharNumberAndUsedFalseOrderByCreatedAtDesc(aadharNumber)
                .ifPresent(data -> {
                    data.setUsed(true);
                    data.setUsedBy(usedBy);
                    data.setUsedAt(LocalDateTime.now());
                    repository.save(data);
                });
    }

    // ==================== CRUD ====================

    public List<PreloadedCustomerData> getAll() {
        return repository.findAllOrderByCreatedAtDesc();
    }

    public List<PreloadedCustomerData> getUnused() {
        return repository.findAllUnused();
    }

    public List<PreloadedCustomerData> getByBatch(String batchId) {
        return repository.findByUploadBatchId(batchId);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", repository.count());
        stats.put("unused", repository.countByUsedFalse());
        stats.put("used", repository.countByUsedTrue());
        stats.put("batches", repository.findDistinctBatchIds());
        return stats;
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteByBatch(String batchId) {
        List<PreloadedCustomerData> batch = repository.findByUploadBatchId(batchId);
        repository.deleteAll(batch);
    }

    @Transactional
    public long deleteAllPermanently() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    // ==================== Excel Parsing ====================

    @Transactional
    public Map<String, Object> parseAndSaveExcel(MultipartFile file, String uploadedBy) {
        Map<String, Object> result = new HashMap<>();
        String batchId = "BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        int savedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                result.put("success", false);
                result.put("message", "Excel file is empty or has no header row");
                return result;
            }

            // Map column names to indices
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String colName = getCellStringValue(cell).trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
                    columnMap.put(colName, i);
                }
            }

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String aadhar = getColumnValue(row, columnMap, "aadhar_number", "aadhar", "aadhaar", "aadhaar_number", "aadhar_no");
                    if (aadhar == null || aadhar.trim().isEmpty()) {
                        skippedCount++;
                        continue;
                    }
                    aadhar = aadhar.replaceAll("[^0-9]", "");
                    if (aadhar.length() != 12) {
                        errors.add("Row " + (i + 1) + ": Invalid Aadhaar number '" + aadhar + "'");
                        skippedCount++;
                        continue;
                    }

                    PreloadedCustomerData data = new PreloadedCustomerData();
                    data.setAadharNumber(aadhar);
                    data.setPanNumber(cleanUpperCase(getColumnValue(row, columnMap, "pan_number", "pan", "pan_no")));
                    data.setAccountType(getColumnValueOrDefault(row, columnMap, "Savings", "account_type", "type"));
                    data.setFullName(getColumnValue(row, columnMap, "full_name", "name", "customer_name", "applicant_name"));
                    data.setDateOfBirth(getColumnValue(row, columnMap, "date_of_birth", "dob", "birth_date"));
                    data.setAge(getColumnIntValue(row, columnMap, "age"));
                    data.setGender(getColumnValue(row, columnMap, "gender", "sex"));
                    data.setOccupation(getColumnValue(row, columnMap, "occupation", "job"));
                    data.setIncome(getColumnDoubleValue(row, columnMap, "income", "annual_income"));
                    data.setPhone(cleanNumeric(getColumnValue(row, columnMap, "phone", "mobile", "mobile_number", "phone_number", "contact")));
                    data.setEmail(getColumnValue(row, columnMap, "email", "email_id", "email_address"));
                    data.setAddress(getColumnValue(row, columnMap, "address", "full_address"));
                    data.setCity(getColumnValue(row, columnMap, "city"));
                    data.setState(getColumnValue(row, columnMap, "state"));
                    data.setPincode(getColumnValue(row, columnMap, "pincode", "pin", "zip", "postal_code"));

                    // Current Account fields
                    data.setBusinessName(getColumnValue(row, columnMap, "business_name", "company", "firm_name"));
                    data.setBusinessType(getColumnValue(row, columnMap, "business_type", "firm_type"));
                    data.setBusinessRegistrationNumber(getColumnValue(row, columnMap, "business_registration_number", "registration_number", "reg_no"));
                    data.setGstNumber(cleanUpperCase(getColumnValue(row, columnMap, "gst_number", "gst", "gstin")));
                    data.setShopAddress(getColumnValue(row, columnMap, "shop_address", "business_address"));

                    // Salary Account fields
                    data.setCompanyName(getColumnValue(row, columnMap, "company_name", "employer", "employer_name"));
                    data.setCompanyId(getColumnValue(row, columnMap, "company_id", "emp_company_id"));
                    data.setDesignation(getColumnValue(row, columnMap, "designation", "position", "role"));
                    data.setMonthlySalary(getColumnDoubleValue(row, columnMap, "monthly_salary", "salary"));
                    data.setSalaryCreditDate(getColumnIntValue(row, columnMap, "salary_credit_date", "salary_date"));
                    data.setHrContactNumber(cleanNumeric(getColumnValue(row, columnMap, "hr_contact_number", "hr_contact", "hr_phone")));
                    data.setEmployerAddress(getColumnValue(row, columnMap, "employer_address"));

                    // Bank Details
                    String branch = getColumnValue(row, columnMap, "branch_name", "branch");
                    if (branch != null && !branch.isEmpty()) data.setBranchName(branch);
                    String ifsc = getColumnValue(row, columnMap, "ifsc_code", "ifsc");
                    if (ifsc != null && !ifsc.isEmpty()) data.setIfscCode(ifsc);

                    data.setUploadedBy(uploadedBy);
                    data.setUploadBatchId(batchId);
                    data.setUploadFileName(file.getOriginalFilename());

                    repository.save(data);
                    savedCount++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    skippedCount++;
                }
            }

            result.put("success", true);
            result.put("message", "Excel processed successfully");
            result.put("batchId", batchId);
            result.put("savedCount", savedCount);
            result.put("skippedCount", skippedCount);
            result.put("totalRows", sheet.getLastRowNum());
            if (!errors.isEmpty()) {
                result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to process Excel file: " + e.getMessage());
        }

        return result;
    }

    // ==================== Excel Template Generation ====================

    public byte[] generateTemplate(String accountType) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(accountType + " Account Data");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            short headerColor;
            switch (accountType) {
                case "Current":
                    headerColor = IndexedColors.LIGHT_ORANGE.getIndex();
                    break;
                case "Salary":
                    headerColor = IndexedColors.LIGHT_GREEN.getIndex();
                    break;
                default:
                    headerColor = IndexedColors.LIGHT_BLUE.getIndex();
                    break;
            }
            headerStyle.setFillForegroundColor(headerColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers;
            String[] sampleData;

            switch (accountType) {
                case "Current":
                    headers = new String[]{
                        "aadhar_number", "pan_number", "full_name", "date_of_birth",
                        "age", "gender", "phone", "email",
                        "address", "city", "state", "pincode",
                        "business_name", "business_type", "business_registration_number",
                        "gst_number", "shop_address", "income",
                        "branch_name", "ifsc_code"
                    };
                    sampleData = new String[]{
                        "123456789012", "ABCDE1234F", "Rahul Sharma", "1985-03-20",
                        "41", "Male", "9876543210", "rahul@business.com",
                        "45 MG Road", "Bangalore", "Karnataka", "560001",
                        "Sharma Enterprises", "Proprietor", "KA2023REG456",
                        "29ABCDE1234F1Z5", "12 Commercial Street, Bangalore",
                        "1200000",
                        "NeoBank Main Branch", "EZYV000123"
                    };
                    break;
                case "Salary":
                    headers = new String[]{
                        "aadhar_number", "pan_number", "full_name", "date_of_birth",
                        "age", "gender", "phone", "email",
                        "address", "city", "state", "pincode",
                        "occupation", "income",
                        "company_name", "company_id", "designation",
                        "monthly_salary", "salary_credit_date",
                        "hr_contact_number", "employer_address",
                        "branch_name", "ifsc_code"
                    };
                    sampleData = new String[]{
                        "987654321098", "XYZAB5678C", "Priya Patel", "1992-07-10",
                        "33", "Female", "8765432109", "priya.patel@techcorp.com",
                        "78 Jubilee Hills", "Hyderabad", "Telangana", "500033",
                        "Software Engineer", "900000",
                        "TechCorp Solutions Pvt Ltd", "TCSP2020", "Senior Developer",
                        "75000", "1",
                        "9988776655", "Hitech City, Hyderabad 500081",
                        "NeoBank Main Branch", "EZYV000123"
                    };
                    break;
                default: // Savings
                    headers = new String[]{
                        "aadhar_number", "pan_number", "full_name", "date_of_birth",
                        "age", "gender", "occupation", "income",
                        "phone", "email",
                        "address", "city", "state", "pincode",
                        "branch_name", "ifsc_code"
                    };
                    sampleData = new String[]{
                        "123456789012", "ABCDE1234F", "John Doe", "1990-01-15",
                        "34", "Male", "Engineer", "600000",
                        "9876543210", "john@email.com",
                        "123 Main St", "Mumbai", "Maharashtra", "400001",
                        "NeoBank Main Branch", "EZYV000123"
                    };
                    break;
            }

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5500);
            }

            // Sample row
            Row sampleRow = sheet.createRow(1);
            for (int i = 0; i < sampleData.length; i++) {
                sampleRow.createCell(i).setCellValue(sampleData[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate template: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private String getColumnValue(Row row, Map<String, Integer> columnMap, String... possibleNames) {
        for (String name : possibleNames) {
            Integer idx = columnMap.get(name);
            if (idx != null) {
                Cell cell = row.getCell(idx);
                if (cell != null) {
                    String value = getCellStringValue(cell);
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }
        return null;
    }

    private String getColumnValueOrDefault(Row row, Map<String, Integer> columnMap, String defaultVal, String... possibleNames) {
        String value = getColumnValue(row, columnMap, possibleNames);
        return (value != null && !value.isEmpty()) ? value : defaultVal;
    }

    private Integer getColumnIntValue(Row row, Map<String, Integer> columnMap, String... possibleNames) {
        String value = getColumnValue(row, columnMap, possibleNames);
        if (value == null) return null;
        try {
            return (int) Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Double getColumnDoubleValue(Row row, Map<String, Integer> columnMap, String... possibleNames) {
        String value = getColumnValue(row, columnMap, possibleNames);
        if (value == null) return null;
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); }
                catch (Exception e) {
                    try { return String.valueOf((long) cell.getNumericCellValue()); }
                    catch (Exception e2) { return null; }
                }
            default: return null;
        }
    }

    private String cleanNumeric(String value) {
        if (value == null) return null;
        return value.replaceAll("[^0-9]", "");
    }

    private String cleanUpperCase(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase();
    }
}
