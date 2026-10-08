package com.neo.springapp.controller;

import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.service.HomeLoanAnalysisService;
import com.neo.springapp.service.HomeLoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/home-loans")
public class HomeLoanController {

    @Autowired private HomeLoanService service;
    @Autowired private HomeLoanAnalysisService analysisService;
    @Autowired private com.neo.springapp.repository.AdminFundTransferRepository transferRepository;

    private ResponseEntity<?> run(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    private static double d(Map<String, Object> b, String k) {
        return Double.parseDouble(String.valueOf(b.get(k)));
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody HomeLoan loan) { return run(() -> service.submit(loan)); }

    @GetMapping
    public ResponseEntity<?> all() { return run(service::all); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) { return run(() -> service.get(id)); }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<?> byAccount(@PathVariable String accountNumber) { return run(() -> service.byAccount(accountNumber)); }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> b) {
        return run(() -> analysisService.analyze(String.valueOf(b.get("accountNumber")),
                b.get("amount") == null ? null : d(b, "amount"),
                b.get("tenure") == null ? null : (int) d(b, "tenure"),
                b.get("interestRate") == null ? null : d(b, "interestRate"),
                b.get("propertyValue") == null ? null : d(b, "propertyValue")));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<?> analysis(@PathVariable Long id) { return run(() -> service.analysis(id)); }

    @GetMapping("/charges-preview")
    public ResponseEntity<?> charges(@RequestParam double amount) { return run(() -> service.chargesPreview(amount)); }

    // ----- admin workflow -----
    @PutMapping("/{id}/advance")
    public ResponseEntity<?> advance(@PathVariable Long id, @RequestParam String admin) { return run(() -> service.advance(id, admin)); }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestParam String admin, @RequestParam(required = false) String reason) {
        return run(() -> service.reject(id, admin, reason));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestParam String accountNumber, @RequestParam(required = false, defaultValue = "Customer") String by) {
        return run(() -> service.cancel(id, accountNumber, by));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestParam String admin) { return run(() -> service.approve(id, admin)); }

    @PutMapping("/{id}/admin-edit")
    public ResponseEntity<?> edit(@PathVariable Long id, @RequestParam String admin, @RequestBody Map<String, Object> fields) {
        return run(() -> service.adminEdit(id, fields, admin));
    }

    // ----- documents -----
    @PostMapping("/{id}/documents/{type}")
    public ResponseEntity<?> upload(@PathVariable Long id, @PathVariable String type, @RequestParam("file") MultipartFile file) {
        return run(() -> {
            try { return service.uploadDocument(id, type, file); } catch (java.io.IOException e) { throw new RuntimeException("Upload failed: " + e.getMessage()); }
        });
    }

    @PutMapping("/{id}/documents/{type}/verify")
    public ResponseEntity<?> verify(@PathVariable Long id, @PathVariable String type, @RequestParam String decision,
                                    @RequestParam(required = false) String remark, @RequestParam String admin) {
        return run(() -> service.verifyDocument(id, type, decision, remark, admin));
    }

    @GetMapping("/{id}/documents/{type}/file")
    public ResponseEntity<?> file(@PathVariable Long id, @PathVariable String type) {
        try {
            Path p = (Path) service.documentFile(id, type).get("path");
            String ct = Files.probeContentType(p);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ct == null ? "application/octet-stream" : ct))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + p.getFileName() + "\"")
                    .body(new FileSystemResource(p));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ----- repayments -----
    @PostMapping("/{id}/pay-emi")
    public ResponseEntity<?> payEmi(@PathVariable Long id, @RequestParam(defaultValue = "Customer") String by) { return run(() -> service.payEmi(id, by)); }

    @PostMapping("/{id}/pay-interest")
    public ResponseEntity<?> payInterest(@PathVariable Long id, @RequestParam(defaultValue = "Customer") String by) { return run(() -> service.payInterest(id, by)); }

    @GetMapping("/{id}/prepay-preview")
    public ResponseEntity<?> prepayPreview(@PathVariable Long id, @RequestParam double amount, @RequestParam(defaultValue = "REDUCE_TENURE") String adjustment) { return run(() -> service.prepayPreview(id, amount, adjustment)); }

    @PostMapping("/{id}/prepay")
    public ResponseEntity<?> prepay(@PathVariable Long id, @RequestParam double amount, @RequestParam(defaultValue = "REDUCE_TENURE") String adjustment, @RequestParam(defaultValue = "Customer") String by) {
        return run(() -> service.prepay(id, amount, adjustment, by, null));
    }

    @GetMapping("/{id}/closure-preview")
    public ResponseEntity<?> closurePreview(@PathVariable Long id) { return run(() -> service.closurePreview(id)); }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id, @RequestParam(defaultValue = "Customer") String by) { return run(() -> service.close(id, by)); }

    @PostMapping("/{id}/renew")
    public ResponseEntity<?> renew(@PathVariable Long id, @RequestParam int months, @RequestParam(defaultValue = "Customer") String by) {
        return run(() -> service.renew(id, months, by));
    }

    // ----- schedule / statements -----
    @GetMapping("/{id}/payments")
    public ResponseEntity<?> payments(@PathVariable Long id) {
        return run(() -> {
            String no = service.get(id).getLoanAccountNumber();
            return no == null ? java.util.List.of()
                    : transferRepository.findByLoanAccountNumberAndTransferCategoryOrderByPerformedAtDesc(no, "HOME_LOAN");
        });
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<?> schedule(@PathVariable Long id) { return run(() -> service.schedule(id)); }

    @GetMapping("/{id}/statement")
    public ResponseEntity<?> statement(@PathVariable Long id) { return run(() -> service.statement(id)); }

    @GetMapping("/account/{accountNumber}/consolidated-statement")
    public ResponseEntity<?> consolidated(@PathVariable String accountNumber) { return run(() -> service.consolidated(accountNumber)); }
}
