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
import java.util.Optional;
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
    public ResponseEntity<?> downloadBlankForm(
            @PathVariable String formCode,
            @RequestParam(value = "adminName", required = false, defaultValue = "Admin") String adminName,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "accountType", required = false) String accountType,
            @RequestParam(value = "holderName", required = false) String holderName,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "aadhaarNumber", required = false) String aadhaarNumber,
            @RequestParam(value = "panNumber", required = false) String panNumber,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email) {
        try {
            byte[] pdf = bankFormService.buildBlankFormPdf(
                    formCode,
                    adminName,
                    accountNumber,
                    accountType,
                    holderName,
                    customerId,
                    aadhaarNumber,
                    panNumber,
                    phone,
                    email);
            if (pdf == null || pdf.length == 0) {
                return errorResponse(500, "PDF generation returned empty content");
            }
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = "NeoBank-" + formCode + "-" + stamp + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return errorResponse(400, e.getMessage());
        } catch (Exception e) {
            return errorResponse(500, "PDF generation failed: " + e.getMessage());
        }
    }

    @GetMapping("/verify-account")
    public ResponseEntity<Map<String, Object>> verifyAccount(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "regular") String accountType,
            @RequestParam(required = false) String customerId) {
        if ((accountNumber == null || accountNumber.isBlank()) && (customerId == null || customerId.isBlank())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Enter account number or customer ID");
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(bankFormService.verifyAccount(accountNumber, accountType, customerId));
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

    @GetMapping("/uploads/{id}/file")
    public ResponseEntity<?> downloadUploadedFile(@PathVariable Long id) {
        return serveUploadedFile(id, false);
    }

    @GetMapping("/uploads/{id}/view")
    public ResponseEntity<?> viewUploadedFile(@PathVariable Long id) {
        return serveUploadedFile(id, true);
    }

    @PutMapping(value = "/uploads/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> replaceUploadedFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replacedByAdmin", required = false, defaultValue = "Admin") String replacedByAdmin,
            @RequestParam(value = "remarks", required = false) String remarks) {
        Map<String, Object> response = new HashMap<>();
        try {
            BankFormUpload saved = bankFormService.replaceUploadedForm(id, file, replacedByAdmin, remarks);
            response.put("success", true);
            response.put("message", "Upload replaced and history saved");
            response.put("upload", bankFormService.toUploadDto(saved));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Replace failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/uploads/{id}/history")
    public ResponseEntity<Map<String, Object>> getUploadHistory(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("history", bankFormService.listUploadHistory(id));
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> serveUploadedFile(Long id, boolean inline) {
        try {
            Optional<BankFormUpload> uploadOpt = bankFormService.findUpload(id);
            if (uploadOpt.isEmpty()) {
                return errorResponse(404, "Upload record not found");
            }
            BankFormUpload upload = uploadOpt.get();
            byte[] fileBytes = bankFormService.readUploadedFile(upload);
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (upload.getContentType() != null && !upload.getContentType().isBlank()) {
                mediaType = MediaType.parseMediaType(upload.getContentType());
            }
            String filename = upload.getOriginalFileName() != null
                    ? upload.getOriginalFileName()
                    : "NeoBank-upload-" + id;
            ContentDisposition disposition = inline
                    ? ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build()
                    : ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(mediaType)
                    .body(fileBytes);
        } catch (IllegalArgumentException e) {
            return errorResponse(404, e.getMessage());
        } catch (Exception e) {
            return errorResponse(500, "Failed to read uploaded file: " + e.getMessage());
        }
    }

    @DeleteMapping("/uploads/{id}")
    public ResponseEntity<Map<String, Object>> deleteUpload(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        boolean deleted = bankFormService.deleteUpload(id);
        if (!deleted) {
            response.put("success", false);
            response.put("message", "Upload record not found");
            return ResponseEntity.status(404).body(response);
        }
        response.put("success", true);
        response.put("message", "Upload deleted successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/uploads")
    public ResponseEntity<Map<String, Object>> clearAllUploads() {
        Map<String, Object> response = new HashMap<>();
        int count = bankFormService.clearAllUploads();
        response.put("success", true);
        response.put("message", "Cleared " + count + " uploaded form(s)");
        response.put("deletedCount", count);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(int status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
