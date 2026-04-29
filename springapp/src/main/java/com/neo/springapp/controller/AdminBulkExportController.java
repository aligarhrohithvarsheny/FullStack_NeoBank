package com.neo.springapp.controller;

import com.neo.springapp.service.AdminBulkExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bulk-export")
@SuppressWarnings("null")
public class AdminBulkExportController {

    private final AdminBulkExportService adminBulkExportService;

    public AdminBulkExportController(AdminBulkExportService adminBulkExportService) {
        this.adminBulkExportService = adminBulkExportService;
    }

    @PostMapping
    public ResponseEntity<byte[]> export(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> raw = (List<String>) body.get("accountNumbers");
        String adminName = body.get("adminName") != null ? String.valueOf(body.get("adminName")) : "Admin";
        String format = body.get("format") != null ? String.valueOf(body.get("format")).toLowerCase() : "pdf";

        List<String> accountNumbers = adminBulkExportService.sanitizeAccountNumbers(raw);
        if (accountNumbers.isEmpty()) {
            return ResponseEntity.badRequest().body("No account numbers provided".getBytes(StandardCharsets.UTF_8));
        }

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        try {
            if ("excel".equals(format) || "xlsx".equals(format)) {
                byte[] data = adminBulkExportService.buildExcel(accountNumbers, adminName);
                String filename = "NeoBank-users-export-" + stamp + ".xlsx";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8).build().toString())
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(data);
            }
            byte[] pdf = adminBulkExportService.buildPdf(accountNumbers, adminName);
            String filename = "NeoBank-users-export-" + stamp + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("Export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
