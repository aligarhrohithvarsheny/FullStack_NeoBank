package com.neo.springapp.controller;

import com.neo.springapp.model.BankFormUpload;
import com.neo.springapp.service.BankFormService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/bank-forms")
@SuppressWarnings("null")
public class BankFormController {

    private final BankFormService bankFormService;

    public BankFormController(BankFormService bankFormService) {
        this.bankFormService = bankFormService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> getCatalog() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("forms", bankFormService.listFormDefinitions());
        response.put("categories", bankFormService.listCategories());
        response.put("total", bankFormService.listFormDefinitions().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{formCode}")
    public ResponseEntity<byte[]> downloadBlankForm(
            @PathVariable String formCode,
            @RequestParam(value = "adminName", required = false, defaultValue = "Admin") String adminName) {
        try {
            byte[] pdf = bankFormService.buildBlankFormPdf(formCode, adminName);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = "NeoBank-" + formCode + "-" + stamp + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(("PDF generation failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/verify-account")
    public ResponseEntity<Map<String, Object>> verifyAccount(
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "regular") String accountType) {
        return ResponseEntity.ok(bankFormService.verifyAccount(accountNumber, accountType));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadForm(
            @RequestParam("formCode") String formCode,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam(value = "accountType", defaultValue = "regular") String accountType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedByAdmin", required = false, defaultValue = "Admin") String uploadedByAdmin,
            @RequestParam(value = "remarks", required = false) String remarks) {
        Map<String, Object> response = new HashMap<>();
        try {
            BankFormUpload saved = bankFormService.saveUploadedForm(
                    formCode, accountNumber, accountType, file, uploadedByAdmin, remarks);
            response.put("success", true);
            response.put("message", "Form uploaded and saved successfully");
            response.put("upload", bankFormService.toUploadDto(saved));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/uploads")
    public ResponseEntity<Map<String, Object>> listUploads(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String formCode) {
        List<Map<String, Object>> uploads = bankFormService.listUploads(accountNumber, formCode).stream()
                .map(bankFormService::toUploadDto)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("uploads", uploads);
        response.put("count", uploads.size());
        return ResponseEntity.ok(response);
    }
}
