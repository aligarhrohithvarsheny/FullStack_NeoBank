package com.neo.springapp.controller;

import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.service.EcsMandateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecs-mandates")
public class EcsMandateController {

    @Autowired
    private EcsMandateService service;

    private static String admin(Map<String, Object> body) {
        Object a = body == null ? null : body.get("admin");
        return a == null ? "Admin" : a.toString();
    }

    private static ResponseEntity<Map<String, Object>> respond(Map<String, Object> r) {
        return Boolean.TRUE.equals(r.get("success")) ? ResponseEntity.ok(r) : ResponseEntity.badRequest().body(r);
    }

    // ----- Admin -----

    @GetMapping
    public List<EcsMandate> all() {
        return service.all();
    }

    @GetMapping("/eligible-loans")
    public List<Map<String, Object>> eligibleLoans() {
        return service.eligibleLoans();
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetch(@RequestParam String loanAccountNumber,
                                                     @RequestParam(required = false) String savingsAccountNumber,
                                                     @RequestParam(required = false) String dob) {
        return respond(service.fetchDetails(loanAccountNumber, savingsAccountNumber, dob));
    }

    @PostMapping("/link")
    public ResponseEntity<Map<String, Object>> link(@RequestBody Map<String, Object> body) {
        return respond(service.link(body, admin(body)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> details(@PathVariable Long id) {
        return respond(service.details(id));
    }

    @PutMapping("/{id}/account")
    public ResponseEntity<Map<String, Object>> changeAccount(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return respond(service.changeAccount(id, str(body.get("savingsAccountNumber")), str(body.get("dob")), admin(body)));
    }

    @PutMapping("/{id}/date")
    public ResponseEntity<Map<String, Object>> changeDate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer day = null;
        try { day = (int) Double.parseDouble(String.valueOf(body.get("debitDay"))); } catch (Exception ignored) { }
        return respond(service.changeDate(id, day, admin(body)));
    }

    @PutMapping("/{id}/limit")
    public ResponseEntity<Map<String, Object>> changeLimit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double limit = null;
        try { limit = Double.parseDouble(String.valueOf(body.get("amountLimit"))); } catch (Exception ignored) { }
        return respond(service.changeLimit(id, limit, admin(body)));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> adminPause(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.pause(id, "ADMIN", admin(body)));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Map<String, Object>> adminResume(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.resume(id, "ADMIN", admin(body)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.cancel(id, admin(body)));
    }

    @PostMapping("/{id}/reject-cancel")
    public ResponseEntity<Map<String, Object>> rejectCancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.rejectCancelRequest(id, body == null ? null : str(body.get("note")), admin(body)));
    }

    @PostMapping("/{id}/debit-now")
    public ResponseEntity<Map<String, Object>> debitNow(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.debitNow(id, admin(body)));
    }

    // ----- User -----

    @GetMapping("/account/{accountNumber}")
    public List<EcsMandate> forAccount(@PathVariable String accountNumber) {
        return service.forAccount(accountNumber);
    }

    @GetMapping("/account/{accountNumber}/{id}")
    public ResponseEntity<Map<String, Object>> userDetails(@PathVariable String accountNumber, @PathVariable Long id) {
        EcsMandate m = service.get(id);
        if (m == null || !service.isOwner(m, accountNumber)) return ResponseEntity.notFound().build();
        return respond(service.details(id));
    }

    @PostMapping("/account/{accountNumber}/{id}/request-cancel")
    public ResponseEntity<Map<String, Object>> requestCancel(@PathVariable String accountNumber, @PathVariable Long id,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        return respond(service.requestCancel(id, accountNumber, body == null ? null : str(body.get("reason"))));
    }

    @PostMapping("/account/{accountNumber}/{id}/pause")
    public ResponseEntity<Map<String, Object>> userPause(@PathVariable String accountNumber, @PathVariable Long id) {
        EcsMandate m = service.get(id);
        if (m == null || !service.isOwner(m, accountNumber)) return ResponseEntity.notFound().build();
        return respond(service.pause(id, "USER", accountNumber));
    }

    @PostMapping("/account/{accountNumber}/{id}/resume")
    public ResponseEntity<Map<String, Object>> userResume(@PathVariable String accountNumber, @PathVariable Long id) {
        EcsMandate m = service.get(id);
        if (m == null || !service.isOwner(m, accountNumber)) return ResponseEntity.notFound().build();
        return respond(service.resume(id, "USER", accountNumber));
    }

    // ----- Public (landing page, no login) -----

    @PostMapping("/public/lookup")
    public ResponseEntity<Map<String, Object>> publicLookup(@RequestBody Map<String, Object> body) {
        return respond(service.publicLookup(str(body.get("loanAccountNumber")), str(body.get("savingsAccountNumber")), str(body.get("dob"))));
    }

    @PostMapping("/public/cancel-request")
    public ResponseEntity<Map<String, Object>> publicCancel(@RequestBody Map<String, Object> body) {
        return respond(service.publicRequestCancel(str(body.get("loanAccountNumber")), str(body.get("savingsAccountNumber")),
                str(body.get("dob")), str(body.get("reason"))));
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().trim();
    }
}
