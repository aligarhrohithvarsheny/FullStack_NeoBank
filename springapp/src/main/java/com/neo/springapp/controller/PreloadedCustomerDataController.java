package com.neo.springapp.controller;

import com.neo.springapp.model.PreloadedCustomerData;
import com.neo.springapp.service.PreloadedCustomerDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/preloaded-customer-data")
@CrossOrigin(origins = "*")
public class PreloadedCustomerDataController {

    @Autowired
    private PreloadedCustomerDataService service;

    // ==================== Upload ====================

    @PostMapping("/upload-excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only .xlsx and .xls files are supported"));
        }

        Map<String, Object> result = service.parseAndSaveExcel(file, uploadedBy);
        return ResponseEntity.ok(result);
    }

    // ==================== Lookup ====================

    @GetMapping("/lookup/aadhar/{aadharNumber}")
    public ResponseEntity<?> lookupByAadhar(@PathVariable String aadharNumber) {
        Optional<PreloadedCustomerData> data = service.findByAadhar(aadharNumber);
        if (data.isPresent()) {
            return ResponseEntity.ok(data.get());
        }
        return ResponseEntity.ok(Map.of("found", false, "message", "No preloaded data found for this Aadhaar number"));
    }

    @GetMapping("/lookup/pan/{panNumber}")
    public ResponseEntity<?> lookupByPan(@PathVariable String panNumber) {
        Optional<PreloadedCustomerData> data = service.findByPan(panNumber);
        if (data.isPresent()) {
            return ResponseEntity.ok(data.get());
        }
        return ResponseEntity.ok(Map.of("found", false, "message", "No preloaded data found for this PAN number"));
    }

    @GetMapping("/exists/aadhar/{aadharNumber}")
    public ResponseEntity<Map<String, Boolean>> existsByAadhar(@PathVariable String aadharNumber) {
        boolean exists = service.existsByAadhar(aadharNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ==================== List / Stats ====================

    @GetMapping("/all")
    public ResponseEntity<List<PreloadedCustomerData>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/unused")
    public ResponseEntity<List<PreloadedCustomerData>> getUnused() {
        return ResponseEntity.ok(service.getUnused());
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<PreloadedCustomerData>> getByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(service.getByBatch(batchId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(service.getStatistics());
    }

    // ==================== Mark as Used ====================

    @PutMapping("/mark-used/{id}")
    public ResponseEntity<Map<String, String>> markAsUsed(@PathVariable Long id, @RequestParam String usedBy) {
        service.markAsUsed(id, usedBy);
        return ResponseEntity.ok(Map.of("message", "Record marked as used"));
    }

    // ==================== Delete ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Record deleted successfully"));
    }

    @DeleteMapping("/batch/{batchId}")
    public ResponseEntity<Map<String, String>> deleteBatch(@PathVariable String batchId) {
        service.deleteByBatch(batchId);
        return ResponseEntity.ok(Map.of("message", "Batch deleted successfully"));
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
        long count = service.deleteAllPermanently();
        return ResponseEntity.ok(Map.of("message", "All records deleted permanently", "deletedCount", count));
    }

    // ==================== Template Download ====================

    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam(defaultValue = "Savings") String accountType) {
        byte[] template = service.generateTemplate(accountType);
        String filename = accountType.toLowerCase() + "_account_template.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }
}
