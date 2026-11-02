package com.neo.springapp.controller;

import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.EcsConsentRecord;
import com.neo.springapp.service.EcsMandateService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
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

    @GetMapping("/{id}/consent-record")
    public ResponseEntity<?> downloadConsentRecord(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) {
            return ResponseEntity.status(403).body(Map.of("message", "Staff sign-in required."));
        }
        EcsConsentRecord record = service.consentRecord(id);
        if (record == null) {
            return ResponseEntity.status(404).body(Map.of("message", "No recorded customer consent is available."));
        }

        String html = consentRecordHtml(record);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("ecs-consent-record-" + id + ".html").build());
        headers.setCacheControl(CacheControl.noStore().cachePrivate());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(html.getBytes(StandardCharsets.UTF_8));
    }

    private static String consentRecordHtml(EcsConsentRecord record) {
        String statement = record.getConsentStatement() == null || record.getConsentStatement().isBlank()
                ? "<p>The exact consent wording was not retained for this legacy authorization. This record contains only the confirmation metadata available in the database.</p>"
                : "<blockquote>" + escapeHtml(record.getConsentStatement()) + "</blockquote>";
        String acceptedAt = record.getAcceptedAt() == null ? "Not available"
                : record.getAcceptedAt().format(DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss"));
        String remoteAddress = record.getRemoteAddress() == null ? "Not available" : record.getRemoteAddress();
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>NeoBank ECS Consent Record</title><style>"
                + "body{font:15px Arial,sans-serif;color:#172033;background:#f3f6fb;padding:28px}"
                + "main{max-width:760px;margin:auto;background:white;border:2px solid #0876a8;padding:28px}"
                + "h1{color:#075985;border-bottom:2px solid #0786b5;padding-bottom:12px}"
                + "h2{font-size:17px;color:#075985;margin-top:24px}table{width:100%;border-collapse:collapse}"
                + "th,td{text-align:left;padding:10px;border:1px solid #cbd5e1;overflow-wrap:anywhere}"
                + "th{width:36%;background:#eff8fc}blockquote{margin:12px 0;padding:14px;background:#f7f9fc;border-left:4px solid #0786b5;line-height:1.55}"
                + ".note{margin-top:24px;padding-top:12px;border-top:1px solid #cbd5e1;color:#526170;font-size:12px}"
                + "@media print{body{padding:0;background:white}main{border-radius:0}}</style></head><body><main>"
                + "<h1>NeoBank</h1><h2>Electronic ECS consent record</h2>"
                + "<table><tbody>"
                + recordRow("Record type", "Customer authorization audit record")
                + recordRow("Mandate ID", record.getMandateId())
                + recordRow("Product account", record.getProductAccountNumber())
                + recordRow("Product", record.getProductType())
                + recordRow("Customer", record.getCustomerName())
                + recordRow("Linked debit account", record.getSavingsAccountNumber())
                + recordRow("Monthly contribution", "INR " + record.getMonthlyAmount())
                + recordRow("Maximum monthly debit", "INR " + record.getAmountLimit())
                + recordRow("Debit day", record.getDebitDay())
                + recordRow("Consent version", record.getConsentVersion())
                + recordRow("Confirmed by", record.getConfirmedName())
                + recordRow("Recorded at (server local time)", acceptedAt)
                + recordRow("Remote address", remoteAddress)
                + "</tbody></table><h2>Customer-confirmed statement</h2>" + statement
                + "<p class=\"note\">This document is generated from the consent record retained by NeoBank. "
                + "It is an ECS authorization audit record, not a copy of a signed loan agreement.</p>"
                + "</main></body></html>";
    }

    private static String recordRow(String label, Object value) {
        return "<tr><th>" + escapeHtml(label) + "</th><td>"
                + escapeHtml(value == null ? "Not available" : value) + "</td></tr>";
    }

    private static String escapeHtml(Object value) {
        return String.valueOf(value)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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
