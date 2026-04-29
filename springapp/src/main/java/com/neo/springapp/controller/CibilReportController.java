package com.neo.springapp.controller;

import com.neo.springapp.model.CibilReport;
import com.neo.springapp.repository.CibilReportRepository;
import com.neo.springapp.service.CibilReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cibil-reports")
public class CibilReportController {

    private final CibilReportService cibilReportService;
    private final CibilReportRepository cibilReportRepository;

    public CibilReportController(CibilReportService cibilReportService,
                                 CibilReportRepository cibilReportRepository) {
        this.cibilReportService = cibilReportService;
        this.cibilReportRepository = cibilReportRepository;
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Only Excel files (.xlsx, .xls) are supported"));
        }
        Map<String, Object> result = cibilReportService.parseAndSaveExcel(file, uploadedBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload-pdf")
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Only PDF files are supported"));
        }
        Map<String, Object> result = cibilReportService.parseAndSavePdf(file, uploadedBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("panNumber") String panNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) String cibilScore,
            @RequestParam(required = false) String approvalLimit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String remarks) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
        }
        Map<String, String> manualData = new java.util.HashMap<>();
        manualData.put("panNumber", panNumber);
        if (name != null) manualData.put("name", name);
        if (salary != null) manualData.put("salary", salary);
        if (cibilScore != null) manualData.put("cibilScore", cibilScore);
        if (approvalLimit != null) manualData.put("approvalLimit", approvalLimit);
        if (status != null) manualData.put("status", status);
        if (remarks != null) manualData.put("remarks", remarks);

        Map<String, Object> result = cibilReportService.saveImageUpload(file, uploadedBy, manualData);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/manager/{uploadedBy}")
    public ResponseEntity<List<CibilReport>> getByManager(@PathVariable String uploadedBy) {
        return ResponseEntity.ok(cibilReportRepository.findByUploadedByOrderByCreatedAtDesc(uploadedBy));
    }

    @GetMapping("/lookup/pan/{panNumber}")
    public ResponseEntity<Map<String, Object>> lookupByPan(@PathVariable String panNumber) {
        Map<String, Object> result = cibilReportService.lookupByPan(panNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/{uploadedBy}")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String uploadedBy) {
        return ResponseEntity.ok(cibilReportService.getStatistics(uploadedBy));
    }

    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] template = cibilReportService.generateTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cibil_upload_template.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(template);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<CibilReport>> getByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(cibilReportRepository.findByUploadBatchId(batchId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        if (cibilReportRepository.existsById(id)) {
            cibilReportRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Report deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/batch/{batchId}")
    public ResponseEntity<Map<String, Object>> deleteBatch(@PathVariable String batchId) {
        cibilReportRepository.deleteByUploadBatchId(batchId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Batch deleted"));
    }
}
