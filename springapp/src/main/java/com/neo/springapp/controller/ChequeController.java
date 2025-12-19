package com.neo.springapp.controller;

import com.neo.springapp.model.Cheque;
import com.neo.springapp.service.ChequeService;
import com.neo.springapp.service.PdfService;
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

    // Request cheque drawing - User
    @PostMapping("/{id}/request")
    public ResponseEntity<Map<String, Object>> requestChequeDraw(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String requestedBy = (String) requestBody.get("requestedBy");
            
            if (requestedBy == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "RequestedBy is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Cheque requestedCheque = chequeService.requestChequeDraw(id, requestedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cheque draw request submitted successfully. Waiting for admin approval.");
            response.put("cheque", requestedCheque);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to request cheque draw: " + e.getMessage());
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
}

