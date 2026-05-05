package com.neo.springapp.controller;

import com.neo.springapp.model.Fasttag;
import com.neo.springapp.service.FasttagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fasttags")
@SuppressWarnings("null")
public class FasttagController {

    @Autowired
    private FasttagService fasttagService;

    @GetMapping
    public List<Fasttag> listAll() {
        return fasttagService.listAll();
    }

    @PostMapping("/{id}/assign-account")
    public ResponseEntity<?> assignAccount(@PathVariable Long id, @RequestBody Map<String,String> body) {
        String accountId = body.get("accountId");
        if (accountId == null || accountId.isEmpty()) return ResponseEntity.badRequest().body("accountId required");
        try {
            Fasttag updated = fasttagService.assignAccount(id, accountId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            Map<String,Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/{id}/assigned-account")
    public ResponseEntity<?> getAssignedAccount(@PathVariable Long id) {
        try {
            var info = fasttagService.getAssignedAccountInfo(id);
            if (info == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(info);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/recharge/admin")
    public ResponseEntity<?> rechargeAdmin(@RequestBody Map<String, Object> body) {
        String fasttagNumber = (String) body.get("fasttagNumber");
        Double amount = body.get("amount") == null ? 0.0 : Double.valueOf(body.get("amount").toString());
        String accountId = body.get("accountId") == null ? null : String.valueOf(body.get("accountId"));
        String initiatedById = body.get("initiatedById") == null ? null : String.valueOf(body.get("initiatedById"));
        try {
            Fasttag updated = fasttagService.rechargeByTagWithDebitAccount(fasttagNumber, amount, initiatedById, accountId);
            if (updated == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "FASTag not found"));
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public List<Fasttag> listForUser(@PathVariable String userId) {
        return fasttagService.listForUser(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Fasttag f = fasttagService.getById(id);
        if (f == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(f);
    }

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody Fasttag app) {
        try {
            Fasttag saved = fasttagService.apply(app);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException ex) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        try {
            Fasttag updated = fasttagService.approve(id);
            if (updated == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@RequestBody Map<String, Object> body) {
        String fasttagNumber = (String) body.get("fasttagNumber");
        Double amount = body.get("amount") == null ? 0.0 : Double.valueOf(body.get("amount").toString());
        // initiatedBy: USER or ADMIN
        String initiatedBy = body.get("initiatedBy") == null ? "USER" : (String) body.get("initiatedBy");
        String initiatedById = body.get("initiatedById") == null ? null : (String) body.get("initiatedById");
        try {
            Fasttag updated = fasttagService.rechargeByTag(fasttagNumber, amount, initiatedBy, initiatedById);
            if (updated == null) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("message", "FASTag not found");
                return ResponseEntity.badRequest().body(resp);
            }
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> transactions(@PathVariable Long id) {
        Fasttag f = fasttagService.getById(id);
        if (f == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fasttagService.transactionsForTag(id));
    }

    @PostMapping("/close/{id}")
    public ResponseEntity<?> close(@PathVariable Long id) {
        Fasttag updated = fasttagService.closeFasttag(id);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/sticker")
    public ResponseEntity<?> downloadSticker(@PathVariable Long id) {
        Fasttag f = fasttagService.getById(id);
        if (f == null) return ResponseEntity.notFound().build();
        if (!"Approved".equalsIgnoreCase(f.getStatus())) return ResponseEntity.badRequest().body("Sticker is available only for approved FASTag.");
        f = fasttagService.ensureStickerForApprovedTag(f);
        String path = f.getStickerPath();
        if (path == null || path.isEmpty()) return ResponseEntity.badRequest().body("Sticker not available");
        try {
            File file = new File(path);
            if (!file.exists()) return ResponseEntity.notFound().build();
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to read sticker");
        }
    }

    @PostMapping("/{id}/upload-rc")
    public ResponseEntity<?> uploadRcFiles(@PathVariable Long id,
                                           @RequestParam(value = "rcFront", required = false) MultipartFile rcFront,
                                           @RequestParam(value = "rcBack", required = false) MultipartFile rcBack) {
        Fasttag f = fasttagService.getById(id);
        if (f == null) return ResponseEntity.notFound().build();
        try {
            String uploadDir = "uploads/fasttag-rc";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            if (rcFront != null && !rcFront.isEmpty()) {
                String originalName = rcFront.getOriginalFilename();
                String ext = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
                String fileName = "rc_front_" + id + "_" + System.currentTimeMillis() + ext;
                Path target = Paths.get(uploadDir, fileName);
                Files.copy(rcFront.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                f.setRcFrontPath(target.toString());
            }
            if (rcBack != null && !rcBack.isEmpty()) {
                String originalName = rcBack.getOriginalFilename();
                String ext = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
                String fileName = "rc_back_" + id + "_" + System.currentTimeMillis() + ext;
                Path target = Paths.get(uploadDir, fileName);
                Files.copy(rcBack.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                f.setRcBackPath(target.toString());
            }
            Fasttag saved = fasttagService.save(f);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to upload RC files: " + e.getMessage()));
        }
    }

    @GetMapping("/rc-image/{filename}")
    public ResponseEntity<?> getRcImage(@PathVariable String filename) {
        try {
            // Sanitize filename to prevent path traversal
            String sanitized = Paths.get(filename).getFileName().toString();
            Path filePath = Paths.get("uploads/fasttag-rc", sanitized);
            if (!Files.exists(filePath)) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to load image");
        }
    }

    @PostMapping("/recharge/user")
    public ResponseEntity<?> rechargeByUser(@RequestBody Map<String, Object> body) {
        String vehicleNumber = body.get("vehicleNumber") == null ? null : String.valueOf(body.get("vehicleNumber"));
        String fasttagNumber = body.get("fasttagNumber") == null ? null : String.valueOf(body.get("fasttagNumber"));
        Double amount = body.get("amount") == null ? 0.0 : Double.valueOf(body.get("amount").toString());
        String debitAccountNumber = body.get("debitAccountNumber") == null ? null : String.valueOf(body.get("debitAccountNumber"));
        String userId = body.get("userId") == null ? null : String.valueOf(body.get("userId"));

        if (amount < 500 || amount > 10000) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Recharge amount must be between ₹500 and ₹10000"));
        }

        try {
            Fasttag updated;
            if (fasttagNumber != null && !fasttagNumber.isEmpty()) {
                updated = fasttagService.rechargeByTag(fasttagNumber, amount, "USER", userId);
            } else if (vehicleNumber != null && !vehicleNumber.isEmpty()) {
                updated = fasttagService.rechargeByVehicleNumber(vehicleNumber, amount, userId);
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vehicle number or FASTag number is required"));
            }
            if (updated == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "FASTag not found for given details"));
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
