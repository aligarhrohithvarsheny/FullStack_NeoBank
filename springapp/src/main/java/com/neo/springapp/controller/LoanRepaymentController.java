package com.neo.springapp.controller;

import com.neo.springapp.service.LoanRepaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/loans/repayment")
public class LoanRepaymentController {
    private final LoanRepaymentService repaymentService;

    public LoanRepaymentController(LoanRepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping("/lookup")
    public ResponseEntity<Map<String, Object>> lookup(@RequestBody LookupRequest request) {
        try {
            return ResponseEntity.ok(repaymentService.lookup(request.loanAccountNumber(), request.dob()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> pay(@RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(repaymentService.pay(request.loanAccountNumber(), request.dob(),
                    request.paymentMode(), request.emiNumber(), request.amount(), request.adjustment()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Payment receipt could not be generated."));
        }
    }

    public record LookupRequest(String loanAccountNumber, String dob) {}

    public record PaymentRequest(
            String loanAccountNumber,
            String dob,
            String paymentMode,
            Integer emiNumber,
            Double amount,
            String adjustment) {}
}
