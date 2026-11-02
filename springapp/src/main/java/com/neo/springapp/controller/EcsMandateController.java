package com.neo.springapp.controller;

import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.service.EcsMandateService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecs-mandates")
public class EcsMandateController {

    @Autowired
    private EcsMandateService service;

    private static ResponseEntity<Map<String, Object>> respond(Map<String, Object> r) {
        return Boolean.TRUE.equals(r.get("success")) ? ResponseEntity.ok(r) : ResponseEntity.badRequest().body(r);
    }

    // ----- Admin -----

    @GetMapping
    public ResponseEntity<?> all(Authentication authentication) {
        return isStaff(authentication) ? ResponseEntity.ok(service.all()) : forbidden();
    }

    @GetMapping("/eligible-loans")
    public ResponseEntity<?> eligibleLoans(Authentication authentication) {
        return isStaff(authentication) ? ResponseEntity.ok(service.eligibleLoans()) : forbidden();
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetch(@RequestParam String loanAccountNumber,
                                                     @RequestParam(required = false) String savingsAccountNumber,
                                                     @RequestParam(required = false) String dob,
                                                     Authentication authentication) {
        if (!isStaff(authentication)) return forbidden();
        return respond(service.fetchDetails(loanAccountNumber, savingsAccountNumber, dob));
    }

    @PostMapping("/link")
    public ResponseEntity<Map<String, Object>> link(@RequestBody Map<String, Object> body,
                                                     Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.link(body, actor(authentication)));
    }

    @GetMapping("/consent/{token}")
    public ResponseEntity<Map<String, Object>> consentDetails(@PathVariable String token) {
        try {
            return ResponseEntity.ok(service.consentDetails(token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(410).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/consent/{token}")
    public ResponseEntity<Map<String, Object>> confirmConsent(@PathVariable String token,
                                                               @RequestBody ConsentRequest request,
                                                               HttpServletRequest servletRequest) {
        return respond(service.confirmConsent(token, request.fullName(), request.consent(),
                servletRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> details(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.details(id));
    }

    @PutMapping("/{id}/account")
    public ResponseEntity<Map<String, Object>> changeAccount(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                              Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.changeAccount(id, str(body.get("savingsAccountNumber")), str(body.get("dob")), actor(authentication)));
    }

    @PutMapping("/{id}/date")
    public ResponseEntity<Map<String, Object>> changeDate(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                           Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        Integer day = null;
        try { day = (int) Double.parseDouble(String.valueOf(body.get("debitDay"))); } catch (Exception ignored) { }
        return respond(service.changeDate(id, day, actor(authentication)));
    }

    @PutMapping("/{id}/limit")
    public ResponseEntity<Map<String, Object>> changeLimit(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        Double limit = null;
        try { limit = Double.parseDouble(String.valueOf(body.get("amountLimit"))); } catch (Exception ignored) { }
        return respond(service.changeLimit(id, limit, actor(authentication)));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> adminPause(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                                                           Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.pause(id, "ADMIN", actor(authentication)));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Map<String, Object>> adminResume(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                                                            Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.resume(id, "ADMIN", actor(authentication)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                                                       Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.cancel(id, actor(authentication)));
    }

    @PostMapping("/{id}/reject-cancel")
    public ResponseEntity<Map<String, Object>> rejectCancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                                                             Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.rejectCancelRequest(id, body == null ? null : str(body.get("note")), actor(authentication)));
    }

    @PostMapping("/{id}/debit-now")
    public ResponseEntity<Map<String, Object>> debitNow(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                                                         Authentication authentication) {
        if (!isStaff(authentication)) return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        return respond(service.debitNow(id, actor(authentication)));
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

    private static boolean isStaff(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal
                && ("ADMIN".equals(principal.scope()) || "MANAGER".equals(principal.scope()));
    }

    private static String actor(Authentication authentication) {
        SessionPrincipal principal = authentication != null && authentication.getPrincipal() instanceof SessionPrincipal session
                ? session : null;
        return principal == null ? "STAFF" : principal.scope() + ":" + principal.userId();
    }

    private static ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
    }

    public record ConsentRequest(String fullName, boolean consent) {}
}
