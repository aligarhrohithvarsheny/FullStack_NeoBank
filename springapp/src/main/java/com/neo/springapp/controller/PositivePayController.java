package com.neo.springapp.controller;

import com.neo.springapp.dto.*;
import com.neo.springapp.model.PositivePayStatus;
import com.neo.springapp.service.PositivePayService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/positive-pay")
public class PositivePayController {
    private final PositivePayService service;
    public PositivePayController(PositivePayService service) { this.service = service; }

    @GetMapping("/accounts/{accountNumber}/eligible-cheques")
    public ResponseEntity<?> eligible(@PathVariable String accountNumber, @RequestHeader("X-User-Id") Long userId) { return ResponseEntity.ok(service.eligibleCheques(accountNumber, userId)); }
    @PostMapping public ResponseEntity<?> create(@Valid @RequestBody PositivePayCreateRequest request, @RequestHeader(value="X-User-Id", required=false) Long userId, @RequestHeader(value="X-Forwarded-For", required=false) String ip) { return ResponseEntity.ok(service.create(request,userId,ip)); }
    @GetMapping("/account/{accountNumber}") public ResponseEntity<?> history(@PathVariable String accountNumber,@RequestHeader(value="X-User-Id",required=false) Long userId){return ResponseEntity.ok(service.byAccount(accountNumber,userId));}
    @GetMapping("/{referenceNumber}") public ResponseEntity<?> details(@PathVariable String referenceNumber,@RequestHeader(value="X-User-Id",required=false) Long userId){return ResponseEntity.ok(service.get(referenceNumber,userId,false));}
    @PatchMapping("/{referenceNumber}/cancel") public ResponseEntity<?> cancel(@PathVariable String referenceNumber,@RequestHeader(value="X-User-Id",required=false) Long userId,@RequestHeader(value="X-Forwarded-For",required=false) String ip){return ResponseEntity.ok(service.cancel(referenceNumber,userId,ip));}

    @PostMapping("/presented/match") public ResponseEntity<?> match(@RequestParam String accountNumber,@RequestParam String chequeNumber,@RequestParam LocalDate chequeDate,@RequestParam BigDecimal amount,@RequestParam String payeeName,@RequestHeader(value="X-Forwarded-For",required=false) String ip){return ResponseEntity.ok(service.matchPresented(accountNumber,chequeNumber,chequeDate,amount,payeeName,ip));}
}
