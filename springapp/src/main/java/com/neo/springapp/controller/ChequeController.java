package com.neo.springapp.controller;

import com.neo.springapp.model.Cheque;
import com.neo.springapp.service.ChequeService;
import com.neo.springapp.service.ChequeDrawService;
import com.neo.springapp.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cheques")
public class ChequeController {

    private final ChequeService chequeService;
    private final PdfService pdfService;
    
    @Autowired
    private ChequeDrawService chequeDrawService;

    public ChequeController(ChequeService chequeService, PdfService pdfService) {
        this.chequeService = chequeService;
        this.pdfService = pdfService;
    }

    // Create cheque leaves for user
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createChequeLeaves(
            @RequestBody Map<String, Object> request) {
        try {
            String accountNumber = (String) request.get("accountNumber");
            Integer numberOfLeaves = Integer.valueOf(request.get("numberOfLeaves").toString());
            Double amount = request.get("amount") != null ? Double.valueOf(request.get("amount").toString()) : null;
            
            if (accountNumber == null || numberOfLeaves == null || numberOfLeaves <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid request data");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            List<Cheque> cheques = chequeService.createChequeLeaves(accountNumber, numberOfLeaves, amount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque leaves created successfully");
            response.put("cheques", cheques);
            response.put("count", cheques.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create cheque leaves: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Get all cheques for an account
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<Cheque>> getChequesByAccountNumber(@PathVariable String accountNumber) {
        try {
            List<Cheque> cheques = chequeService.getChequesByAccountNumber(accountNumber);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cheques by account number with pagination
    @GetMapping("/account/{accountNumber}/page")
    public ResponseEntity<Page<Cheque>> getChequesByAccountNumber(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.getChequesByAccountNumber(accountNumber, page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get active cheques for an account
    @GetMapping("/account/{accountNumber}/active")
    public ResponseEntity<List<Cheque>> getActiveCheques(@PathVariable String accountNumber) {
        try {
            List<Cheque> cheques = chequeService.getActiveChequesByAccountNumber(accountNumber);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cancelled cheques for an account
    @GetMapping("/account/{accountNumber}/cancelled")
    public ResponseEntity<List<Cheque>> getCancelledCheques(@PathVariable String accountNumber) {
        try {
            List<Cheque> cheques = chequeService.getCancelledChequesByAccountNumber(accountNumber);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cheque by ID
    @GetMapping("/{id}")
    public ResponseEntity<Cheque> getChequeById(@PathVariable Long id) {
        try {
            return chequeService.getChequeById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cheque by cheque number
    @GetMapping("/number/{chequeNumber}")
    public ResponseEntity<Cheque> getChequeByChequeNumber(@PathVariable String chequeNumber) {
        try {
            return chequeService.getChequeByChequeNumber(chequeNumber)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Cancel cheque
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelCheque(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String cancelledBy = (String) request.get("cancelledBy");
            String reason = (String) request.get("reason");
            
            if (cancelledBy == null || reason == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "CancelledBy and reason are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque cancelledCheque = chequeService.cancelCheque(id, cancelledBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque cancelled successfully");
            response.put("cheque", cancelledCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to cancel cheque: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Send OTP to registered email for cheque draw (reason in email)
    @PostMapping("/{id}/request-otp")
    public ResponseEntity<Map<String, Object>> requestDrawOtp(@PathVariable Long id) {
        return ResponseEntity.ok(chequeService.requestDrawOtp(id));
    }

    // Request cheque drawing - User. Include "otp" in body to verify before submitting.
    @PostMapping("/{id}/request")
    public ResponseEntity<Map<String, Object>> requestChequeDraw(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String requestedBy = (String) requestBody.get("requestedBy");
            String otp = requestBody.get("otp") != null ? requestBody.get("otp").toString() : null;
            if (requestedBy == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "RequestedBy is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            Cheque requestedCheque = chequeService.requestChequeDraw(id, requestedBy, otp);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque draw request submitted successfully. Waiting for admin approval.");
            response.put("cheque", requestedCheque);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Failed to request cheque draw");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Download cheque PDF
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCheque(@PathVariable Long id) {
        try {
            Cheque cheque = chequeService.getChequeById(id)
                    .orElseThrow(() -> new RuntimeException("Cheque not found"));
            
            byte[] pdfBytes = pdfService.generateChequePdf(cheque);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "cheque_" + cheque.getChequeNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // View cheque (same as download but with inline display)
    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewCheque(@PathVariable Long id) {
        try {
            Cheque cheque = chequeService.getChequeById(id)
                    .orElseThrow(() -> new RuntimeException("Cheque not found"));
            
            byte[] pdfBytes = pdfService.generateChequePdf(cheque);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "cheque_" + cheque.getChequeNumber() + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get all cheques with pagination
    @GetMapping
    public ResponseEntity<Page<Cheque>> getAllCheques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.getAllCheques(page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cheques by status
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Cheque>> getChequesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.getChequesByStatus(status, page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get cheque statistics for an account
    @GetMapping("/account/{accountNumber}/statistics")
    public ResponseEntity<Map<String, Object>> getChequeStatistics(@PathVariable String accountNumber) {
        try {
            Long activeCount = chequeService.countActiveCheques(accountNumber);
            Long cancelledCount = chequeService.countCancelledCheques(accountNumber);
            Long totalCount = activeCount + cancelledCount;
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCheques", totalCount);
            statistics.put("activeCheques", activeCount);
            statistics.put("cancelledCheques", cancelledCount);
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    // Draw cheque (withdraw amount) - Admin only
    @PostMapping("/admin/draw")
    public ResponseEntity<Map<String, Object>> drawCheque(@RequestBody Map<String, Object> request) {
        try {
            String chequeNumber = (String) request.get("chequeNumber");
            String drawnBy = (String) request.get("drawnBy");
            
            if (chequeNumber == null || drawnBy == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cheque number and drawnBy are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque drawnCheque = chequeService.drawCheque(chequeNumber, drawnBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque drawn successfully. Amount ₹" + drawnCheque.getAmount() + " debited from account.");
            response.put("cheque", drawnCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to draw cheque: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Bounce cheque - Admin only
    @PostMapping("/admin/bounce")
    public ResponseEntity<Map<String, Object>> bounceCheque(@RequestBody Map<String, Object> request) {
        try {
            String chequeNumber = (String) request.get("chequeNumber");
            String bouncedBy = (String) request.get("bouncedBy");
            String reason = (String) request.get("reason");
            
            if (chequeNumber == null || bouncedBy == null || reason == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cheque number, bouncedBy, and reason are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque bouncedCheque = chequeService.bounceCheque(chequeNumber, bouncedBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque bounced successfully");
            response.put("cheque", bouncedCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to bounce cheque: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Search cheque by cheque number - Admin only
    @GetMapping("/admin/search")
    public ResponseEntity<Page<Cheque>> searchChequeByNumber(
            @RequestParam String chequeNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.searchChequesByChequeNumber(chequeNumber, page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get all drawn cheques - Admin only
    @GetMapping("/admin/drawn")
    public ResponseEntity<Page<Cheque>> getDrawnCheques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.getDrawnCheques(page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get all bounced cheques - Admin only
    @GetMapping("/admin/bounced")
    public ResponseEntity<Page<Cheque>> getBouncedCheques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> cheques = chequeService.getBouncedCheques(page, size);
            return ResponseEntity.ok(cheques);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get pending cheque requests - Admin only
    @GetMapping("/admin/requests/pending")
    public ResponseEntity<Page<Cheque>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> requests = chequeService.getPendingRequests(page, size);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get approved cheque requests - Admin only
    @GetMapping("/admin/requests/approved")
    public ResponseEntity<Page<Cheque>> getApprovedRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Cheque> requests = chequeService.getApprovedRequests(page, size);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Approve cheque request and draw - Admin only
    @PostMapping("/admin/requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveChequeRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String approvedBy = (String) requestBody.get("approvedBy");
            
            if (approvedBy == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "ApprovedBy is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque approvedCheque = chequeService.approveChequeRequest(id, approvedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque request approved and drawn successfully. Amount ₹" + approvedCheque.getAmount() + " debited from account.");
            response.put("cheque", approvedCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to approve cheque request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Reject cheque request - Admin only
    @PostMapping("/admin/requests/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectChequeRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String rejectedBy = (String) requestBody.get("rejectedBy");
            String reason = (String) requestBody.get("reason");
            
            if (rejectedBy == null || reason == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "RejectedBy and reason are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque rejectedCheque = chequeService.rejectChequeRequest(id, rejectedBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque request rejected successfully");
            response.put("cheque", rejectedCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to reject cheque request: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Get cheque statistics (all statuses) - Admin only
    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getAllChequeStatistics() {
        try {
            Map<String, Object> statistics = chequeService.getChequeStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== CHEQUE DRAW SYSTEM ENDPOINTS ====================
    // (New feature: Salary Account Cheque Draw Requests with Admin Management)

    /**
     * User requests to draw a cheque from salary account
     * POST /api/cheques/draw/apply
     */
    @PostMapping("/draw/apply")
    public ResponseEntity<Map<String, Object>> applyCheque(
            @RequestBody Map<String, Object> request) {
        try {
            Long salaryAccountId = Long.parseLong(request.get("salaryAccountId").toString());
            String serialNumber = (String) request.get("serialNumber");
            String chequeDate = (String) request.get("chequeDate");
            Double amount = Double.parseDouble(request.get("amount").toString());
            String payeeName = (String) request.get("payeeName");
            String remarks = (String) request.get("remarks");
            
            return ResponseEntity.ok(chequeDrawService.applyChequeDrawRequest(
                    salaryAccountId, serialNumber, chequeDate, amount, payeeName, remarks
            ));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to apply cheque: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get user's cheque draw requests
     * GET /api/cheques/draw/user/{salaryAccountId}
     */
    @GetMapping("/draw/user/{salaryAccountId}")
    public ResponseEntity<Map<String, Object>> getUserChequeDrawRequests(
            @PathVariable Long salaryAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(chequeDrawService.getUserChequeDrawRequests(salaryAccountId, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get cheque draw request details
     * GET /api/cheques/draw/{id}
     */
    @GetMapping("/draw/{id}")
    public ResponseEntity<Map<String, Object>> getChequeDrawDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chequeDrawService.getChequeDrawDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * User cancels a pending cheque draw request
     * POST /api/cheques/draw/{id}/cancel
     */
    @PostMapping("/draw/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            String reason = request != null ? (String) request.get("reason") : "";
            return ResponseEntity.ok(chequeDrawService.cancelChequeDrawRequest(id, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * User edits a pending cheque draw request (payeeName and amount only)
     * PUT /api/cheques/draw/{id}/edit
     */
    @PutMapping("/draw/{id}/edit")
    public ResponseEntity<Map<String, Object>> editPendingChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String payeeName = request != null ? (String) request.get("payeeName") : null;
            Double amount = request != null && request.get("amount") != null
                    ? Double.parseDouble(request.get("amount").toString()) : null;

            return ResponseEntity.ok(chequeDrawService.editPendingChequeDrawRequest(id, payeeName, amount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get all cheque draw requests for admin (with filtering)
     * GET /api/cheques/draw/admin/all
     */
    @GetMapping("/draw/admin/all")
    public ResponseEntity<Map<String, Object>> getAdminChequeDrawRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(chequeDrawService.getAdminChequeDrawRequests(status, search, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get admin view of cheque draw request (with audit log)
     * GET /api/cheques/draw/admin/{id}
     */
    @GetMapping("/draw/admin/{id}")
    public ResponseEntity<Map<String, Object>> getAdminChequeDrawDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chequeDrawService.getAdminChequeDrawDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Verify payee account number and name match
     * POST /api/cheques/draw/admin/verify-payee
     */
    @PostMapping("/draw/admin/verify-payee")
    public ResponseEntity<Map<String, Object>> verifyPayeeAccount(
            @RequestBody Map<String, Object> request) {
        try {
            String payeeAccountNumber = (String) request.get("payeeAccountNumber");
            String payeeName = (String) request.get("payeeName");

            if (payeeAccountNumber == null || payeeAccountNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Payee account number is required")
                );
            }
            if (payeeName == null || payeeName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Payee name is required")
                );
            }

            return ResponseEntity.ok(chequeDrawService.verifyPayeeAccount(payeeAccountNumber, payeeName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Admin approves a cheque draw request with payee verification
     * POST /api/cheques/draw/admin/{id}/approve
     */
    @PostMapping("/draw/admin/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String remarks = request != null ? (String) request.get("remarks") : "";
            String payeeAccountNumber = request != null ? (String) request.get("payeeAccountNumber") : null;
            if (adminEmail == null) adminEmail = "admin@neobank.com";

            return ResponseEntity.ok(chequeDrawService.approveChequeDrawRequest(id, adminEmail, remarks, payeeAccountNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Admin rejects a cheque draw request
     * POST /api/cheques/draw/admin/{id}/reject
     */
    @PostMapping("/draw/admin/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String rejectionReason = (String) request.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Rejection reason is required")
                );
            }
            if (adminEmail == null) adminEmail = "admin@neobank.com";
            
            return ResponseEntity.ok(chequeDrawService.rejectChequeDrawRequest(id, adminEmail, rejectionReason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Admin marks cheque as picked up by user
     * POST /api/cheques/draw/admin/{id}/picked-up
     */
    @PostMapping("/draw/admin/{id}/picked-up")
    public ResponseEntity<Map<String, Object>> markChequePickedUp(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            if (adminEmail == null) adminEmail = "admin@neobank.com";
            return ResponseEntity.ok(chequeDrawService.markChequeDrawPickedUp(id, adminEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Admin marks cheque as cleared (used)
     * POST /api/cheques/draw/admin/{id}/clear
     */
    @PostMapping("/draw/admin/{id}/clear")
    public ResponseEntity<Map<String, Object>> clearCheque(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String clearedDate = request != null ? (String) request.get("clearedDate") : null;
            if (adminEmail == null) adminEmail = "admin@neobank.com";
            
            return ResponseEntity.ok(chequeDrawService.clearChequeDrawRequest(id, adminEmail, clearedDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * User marks cheque as downloaded (after printing/saving)
     * POST /api/cheques/draw/{id}/mark-downloaded
     */
    @PostMapping("/draw/{id}/mark-downloaded")
    public ResponseEntity<Map<String, Object>> markChequeDownloaded(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chequeDrawService.markChequeDownloaded(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        }
    }

    /**
     * Get cheque draw management statistics for admin dashboard
     * GET /api/cheques/draw/admin/stats
     */
    @GetMapping("/draw/admin/stats")
    public ResponseEntity<Map<String, Object>> getChequeDrawStats() {
        try {
            return ResponseEntity.ok(chequeDrawService.getChequeDrawStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get audit log for a cheque draw request
     * GET /api/cheques/draw/admin/{id}/audit-log
     */
    @GetMapping("/draw/admin/{id}/audit-log")
    public ResponseEntity<Map<String, Object>> getChequeDrawAuditLog(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(chequeDrawService.getChequeDrawAuditLog(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Search cheque draw requests by cheque number
     * GET /api/cheques/draw/admin/search
     */
    @GetMapping("/draw/admin/search")
    public ResponseEntity<Map<String, Object>> searchChequeDrawByNumber(@RequestParam String chequeNumber) {
        try {
            return ResponseEntity.ok(chequeDrawService.searchChequeDrawByNumber(chequeNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get cheque draw requests by status
     * GET /api/cheques/draw/admin/by-status
     */
    @GetMapping("/draw/admin/by-status")
    public ResponseEntity<Map<String, Object>> getChequeDrawByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(chequeDrawService.getChequeDrawByStatus(status, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Export cheque draw requests to CSV
     * GET /api/cheques/draw/admin/export
     */
    @GetMapping("/draw/admin/export")
    public ResponseEntity<?> exportChequeDrawToCSV(@RequestParam(required = false) String status) {
        try {
            byte[] csvData = chequeDrawService.exportChequeDrawToCSV(status);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=\"cheques_draw_export.csv\"")
                    .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Validate cheque serial number
     * GET /api/cheques/draw/validate-serial
     */
    @GetMapping("/draw/validate-serial")
    public ResponseEntity<Map<String, Object>> validateSerialNumber(
            @RequestParam String serialNumber,
            @RequestParam Long salaryAccountId) {
        try {
            boolean isValid = chequeDrawService.validateChequeSerialNumber(serialNumber, salaryAccountId);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get available balance for an account
     * GET /api/cheques/draw/available-balance
     */
    @GetMapping("/draw/available-balance")
    public ResponseEntity<Map<String, Object>> getAvailableBalance(@RequestParam Long salaryAccountId) {
        try {
            Double balance = chequeDrawService.getAccountAvailableBalance(salaryAccountId);
            return ResponseEntity.ok(Map.of(
                    "salaryAccountId", salaryAccountId,
                    "availableBalance", balance
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    /**
     * Get available cheque leaves for a salary account (auto-allocates 30 if none exist)
     * GET /api/cheques/draw/leaves/{salaryAccountId}
     */
    @GetMapping("/draw/leaves/{salaryAccountId}")
    public ResponseEntity<Map<String, Object>> getAvailableChequeLeaves(
            @PathVariable Long salaryAccountId) {
        try {
            return ResponseEntity.ok(chequeDrawService.getAvailableChequeLeaves(salaryAccountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }
}

