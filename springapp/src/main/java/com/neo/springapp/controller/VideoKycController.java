package com.neo.springapp.controller;

import com.neo.springapp.model.VideoKycSession;
import com.neo.springapp.model.VideoKycAuditLog;
import com.neo.springapp.model.VideoKycSlot;
import com.neo.springapp.service.VideoKycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/video-kyc")
@CrossOrigin(origins = "*")
public class VideoKycController {

    @Autowired
    private VideoKycService videoKycService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );

    // ======================== User Registration ========================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> payload) {
        try {
            VideoKycSession session = new VideoKycSession();
            session.setFullName((String) payload.get("fullName"));
            session.setMobileNumber((String) payload.get("mobileNumber"));
            session.setEmail((String) payload.get("email"));
            session.setAddressCity((String) payload.get("addressCity"));
            session.setAddressState((String) payload.get("addressState"));
            session.setAccountType((String) payload.getOrDefault("accountType", "Savings"));
            session.setAadharNumber((String) payload.get("aadharNumber"));
            session.setPanNumber((String) payload.get("panNumber"));

            VideoKycSession created = videoKycService.createSession(session);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful. Please upload documents.");
            response.put("sessionId", created.getId());
            response.put("customerId", created.getCustomerId());
            response.put("temporaryAccountNumber", created.getTemporaryAccountNumber());
            response.put("roomId", created.getRoomId());
            response.put("kycStatus", created.getKycStatus());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ======================== Document Upload ========================

    @PostMapping("/upload-documents/{sessionId}")
    public ResponseEntity<?> uploadDocuments(
            @PathVariable Long sessionId,
            @RequestParam("aadharDocument") MultipartFile aadharDoc,
            @RequestParam("panDocument") MultipartFile panDoc) {
        try {
            // Validate file sizes
            if (aadharDoc.getSize() > MAX_FILE_SIZE || panDoc.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "File size exceeds 5MB limit"
                ));
            }

            // Validate file types
            if (!ALLOWED_TYPES.contains(aadharDoc.getContentType()) ||
                !ALLOWED_TYPES.contains(panDoc.getContentType())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Only JPEG, PNG and PDF files are allowed"
                ));
            }

            VideoKycSession updated = videoKycService.uploadDocuments(
                    sessionId,
                    aadharDoc.getBytes(), aadharDoc.getOriginalFilename(), aadharDoc.getContentType(),
                    panDoc.getBytes(), panDoc.getOriginalFilename(), panDoc.getContentType()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("sessionId", updated.getId());
            response.put("kycStatus", updated.getKycStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ======================== Video KYC Session ========================

    @PostMapping("/start-video/{sessionId}")
    public ResponseEntity<?> startVideoKyc(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.startVideoKyc(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomId", session.getRoomId());
            response.put("otpCode", session.getOtpCode());
            response.put("kycStatus", session.getKycStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-otp/{sessionId}")
    public ResponseEntity<?> verifyOtp(@PathVariable Long sessionId, @RequestBody Map<String, String> payload) {
        try {
            String otp = payload.get("otp");
            VideoKycSession session = videoKycService.verifyOtp(sessionId, otp);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP verified successfully",
                    "otpVerified", session.getOtpVerified()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/snapshot/face/{sessionId}")
    public ResponseEntity<?> saveFaceSnapshot(
            @PathVariable Long sessionId,
            @RequestParam("snapshot") MultipartFile file) {
        try {
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File too large"));
            }
            videoKycService.saveFaceSnapshot(sessionId, file.getBytes(), file.getContentType());
            return ResponseEntity.ok(Map.of("success", true, "message", "Face snapshot saved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/snapshot/id/{sessionId}")
    public ResponseEntity<?> saveIdSnapshot(
            @PathVariable Long sessionId,
            @RequestParam("snapshot") MultipartFile file) {
        try {
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File too large"));
            }
            videoKycService.saveIdSnapshot(sessionId, file.getBytes(), file.getContentType());
            return ResponseEntity.ok(Map.of("success", true, "message", "ID snapshot saved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/liveness-check/{sessionId}")
    public ResponseEntity<?> livenessCheck(@PathVariable Long sessionId, @RequestBody Map<String, Object> payload) {
        try {
            boolean passed = (Boolean) payload.get("passed");
            String checkType = (String) payload.get("checkType");
            videoKycService.completeLivenessCheck(sessionId, passed, checkType);
            return ResponseEntity.ok(Map.of("success", true, "passed", passed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/end-session/{sessionId}")
    public ResponseEntity<?> endSession(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.endVideoSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "kycStatus", session.getKycStatus(),
                    "message", "Video KYC session ended"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Status Check ========================

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<?> getStatus(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("customerId", session.getCustomerId());
            response.put("fullName", session.getFullName());
            response.put("kycStatus", session.getKycStatus());
            response.put("temporaryAccountNumber", session.getTemporaryAccountNumber());
            response.put("finalAccountNumber", session.getFinalAccountNumber());
            response.put("ifscCode", session.getIfscCode());
            response.put("roomId", session.getRoomId());
            response.put("rejectionReason", session.getRejectionReason());
            response.put("kycAttemptCount", session.getKycAttemptCount());
            response.put("maxAttempts", session.getMaxAttempts());
            response.put("createdAt", session.getCreatedAt());
            response.put("approvedAt", session.getApprovedAt());
            response.put("accountType", session.getAccountType());
            response.put("verificationNumber", session.getVerificationNumber());
            response.put("slotDate", session.getSlotDate() != null ? session.getSlotDate().toString() : null);
            response.put("slotTime", session.getSlotTime() != null ? session.getSlotTime().toString() : null);
            response.put("slotEndTime", session.getSlotEndTime() != null ? session.getSlotEndTime().toString() : null);
            response.put("bookedSlotId", session.getBookedSlotId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/status/mobile/{mobileNumber}")
    public ResponseEntity<?> getStatusByMobile(@PathVariable String mobileNumber) {
        try {
            VideoKycSession session = videoKycService.checkStatusByMobile(mobileNumber);
            if (session == null) {
                return ResponseEntity.ok(Map.of("found", false));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("found", true);
            response.put("sessionId", session.getId());
            response.put("kycStatus", session.getKycStatus());
            response.put("customerId", session.getCustomerId());
            response.put("temporaryAccountNumber", session.getTemporaryAccountNumber());
            response.put("finalAccountNumber", session.getFinalAccountNumber());
            response.put("rejectionReason", session.getRejectionReason());
            response.put("kycAttemptCount", session.getKycAttemptCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Admin Endpoints ========================

    @GetMapping("/admin/queue")
    public ResponseEntity<?> getKycQueue() {
        try {
            List<VideoKycSession> queue = videoKycService.getKycQueue();
            List<Map<String, Object>> result = new ArrayList<>();
            for (VideoKycSession s : queue) {
                result.add(buildSessionSummary(s));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<VideoKycSession> sessions = videoKycService.getAllSessions(page, size);
            return ResponseEntity.ok(sessions.map(this::buildSessionSummary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/filter")
    public ResponseEntity<?> filterByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<VideoKycSession> sessions = videoKycService.getSessionsByStatus(status, page, size);
            return ResponseEntity.ok(sessions.map(this::buildSessionSummary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/search")
    public ResponseEntity<?> searchSessions(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<VideoKycSession> sessions = videoKycService.searchSessions(query, page, size);
            return ResponseEntity.ok(sessions.map(this::buildSessionSummary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(videoKycService.getStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/join/{sessionId}")
    public ResponseEntity<?> adminJoinSession(
            @PathVariable Long sessionId,
            @RequestParam Long adminId,
            @RequestParam String adminName) {
        try {
            VideoKycSession session = videoKycService.adminJoinSession(sessionId, adminId, adminName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomId", session.getRoomId());
            response.put("kycStatus", session.getKycStatus());
            response.put("session", buildSessionDetail(session));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/approve/{sessionId}")
    public ResponseEntity<?> approveKyc(
            @PathVariable Long sessionId,
            @RequestParam Long adminId,
            @RequestParam String adminName) {
        try {
            VideoKycSession session = videoKycService.approveKyc(sessionId, adminId, adminName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "KYC approved successfully");
            response.put("finalAccountNumber", session.getFinalAccountNumber());
            response.put("ifscCode", session.getIfscCode());
            response.put("kycStatus", session.getKycStatus());
            response.put("accountType", session.getAccountType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/reject/{sessionId}")
    public ResponseEntity<?> rejectKyc(
            @PathVariable Long sessionId,
            @RequestParam Long adminId,
            @RequestParam String adminName,
            @RequestParam String reason) {
        try {
            VideoKycSession session = videoKycService.rejectKyc(sessionId, adminId, adminName, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "KYC rejected",
                    "kycStatus", session.getKycStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/reopen/{sessionId}")
    public ResponseEntity<?> reopenSession(
            @PathVariable Long sessionId,
            @RequestParam Long adminId,
            @RequestParam String adminName) {
        try {
            VideoKycSession session = videoKycService.reopenSession(sessionId, adminId, adminName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Session re-opened for re-attempt",
                    "kycStatus", session.getKycStatus(),
                    "roomId", session.getRoomId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/force-reverify/{sessionId}")
    public ResponseEntity<?> forceReVerification(
            @PathVariable Long sessionId,
            @RequestParam Long adminId,
            @RequestParam String adminName) {
        try {
            VideoKycSession session = videoKycService.forceReVerification(sessionId, adminId, adminName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Force re-verification initiated",
                    "kycStatus", session.getKycStatus(),
                    "roomId", session.getRoomId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/session-detail/{sessionId}")
    public ResponseEntity<?> getSessionDetail(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            return ResponseEntity.ok(buildSessionDetail(session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Document Download ========================

    @GetMapping("/document/aadhar/{sessionId}")
    public ResponseEntity<?> downloadAadhar(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            if (session.getAadharDocument() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + session.getAadharDocumentName() + "\"")
                    .contentType(MediaType.parseMediaType(session.getAadharDocumentType()))
                    .body(session.getAadharDocument());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/document/pan/{sessionId}")
    public ResponseEntity<?> downloadPan(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            if (session.getPanDocument() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + session.getPanDocumentName() + "\"")
                    .contentType(MediaType.parseMediaType(session.getPanDocumentType()))
                    .body(session.getPanDocument());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/snapshot/face/{sessionId}")
    public ResponseEntity<?> getFaceSnapshot(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            if (session.getFaceSnapshot() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(session.getFaceSnapshotType()))
                    .body(session.getFaceSnapshot());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/snapshot/id-proof/{sessionId}")
    public ResponseEntity<?> getIdSnapshot(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.getSession(sessionId);
            if (session.getIdSnapshot() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(session.getIdSnapshotType()))
                    .body(session.getIdSnapshot());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Audit Logs ========================

    @GetMapping("/admin/audit-logs/{sessionId}")
    public ResponseEntity<?> getAuditLogs(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(videoKycService.getAuditLogs(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/audit-logs")
    public ResponseEntity<?> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            return ResponseEntity.ok(videoKycService.getAllAuditLogs(page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Slot Booking (User) ========================

    @GetMapping("/slots/available")
    public ResponseEntity<?> getAvailableSlots() {
        try {
            List<VideoKycSlot> slots = videoKycService.getAvailableSlots();
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/book-slot/{sessionId}")
    public ResponseEntity<?> bookSlot(@PathVariable Long sessionId, @RequestBody Map<String, Object> payload) {
        try {
            Long slotId = Long.valueOf(payload.get("slotId").toString());
            VideoKycSession session = videoKycService.bookSlot(sessionId, slotId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Slot booked successfully");
            response.put("bookedSlotId", session.getBookedSlotId());
            response.put("slotDate", session.getSlotDate() != null ? session.getSlotDate().toString() : null);
            response.put("slotTime", session.getSlotTime() != null ? session.getSlotTime().toString() : null);
            response.put("slotEndTime", session.getSlotEndTime() != null ? session.getSlotEndTime().toString() : null);
            response.put("kycStatus", session.getKycStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/cancel-booking/{sessionId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long sessionId) {
        try {
            VideoKycSession session = videoKycService.cancelBooking(sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Booking cancelled", "kycStatus", session.getKycStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/can-join/{sessionId}")
    public ResponseEntity<?> canJoinVideoKyc(@PathVariable Long sessionId) {
        try {
            boolean canJoin = videoKycService.canJoinVideoKyc(sessionId);
            return ResponseEntity.ok(Map.of("canJoin", canJoin));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Verification Number ========================

    @GetMapping("/verify/{verificationNumber}")
    public ResponseEntity<?> verifyByNumber(@PathVariable String verificationNumber) {
        try {
            VideoKycSession session = videoKycService.verifyByNumber(verificationNumber);
            if (session == null) {
                return ResponseEntity.ok(Map.of("found", false, "message", "No session found with this verification number"));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("found", true);
            response.put("verificationNumber", session.getVerificationNumber());
            response.put("fullName", session.getFullName());
            response.put("kycStatus", session.getKycStatus());
            response.put("customerId", session.getCustomerId());
            response.put("accountType", session.getAccountType());
            response.put("finalAccountNumber", session.getFinalAccountNumber());
            response.put("approvedAt", session.getApprovedAt());
            response.put("createdAt", session.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Admin Slot Management ========================

    @PostMapping("/admin/slots/create")
    public ResponseEntity<?> createSlot(@RequestBody Map<String, Object> payload) {
        try {
            LocalDate date = LocalDate.parse((String) payload.get("date"));
            LocalTime time = LocalTime.parse((String) payload.get("time"));
            LocalTime endTime = LocalTime.parse((String) payload.get("endTime"));
            int maxBookings = payload.get("maxBookings") != null ? Integer.parseInt(payload.get("maxBookings").toString()) : 5;
            String createdBy = (String) payload.getOrDefault("createdBy", "Admin");

            VideoKycSlot slot = videoKycService.createSlot(date, time, endTime, maxBookings, createdBy);
            return ResponseEntity.ok(Map.of("success", true, "message", "Slot created", "slotId", slot.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/slots/generate-defaults")
    public ResponseEntity<?> generateDefaultSlots() {
        try {
            videoKycService.generateDefaultSlots();
            List<VideoKycSlot> slots = videoKycService.getAllActiveSlots();
            return ResponseEntity.ok(Map.of("success", true, "message", "Default slots generated for next 7 days", "totalSlots", slots.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/slots/all")
    public ResponseEntity<?> getAllSlots() {
        try {
            return ResponseEntity.ok(videoKycService.getAllActiveSlots());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/slots/{slotId}")
    public ResponseEntity<?> cancelSlot(@PathVariable Long slotId) {
        try {
            videoKycService.cancelSlot(slotId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Slot cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/admin/slots/{slotId}/reschedule")
    public ResponseEntity<?> rescheduleSlot(@PathVariable Long slotId, @RequestBody Map<String, Object> payload) {
        try {
            LocalDate newDate = LocalDate.parse((String) payload.get("date"));
            LocalTime newTime = LocalTime.parse((String) payload.get("time"));
            LocalTime newEndTime = LocalTime.parse((String) payload.get("endTime"));
            VideoKycSlot slot = videoKycService.rescheduleSlot(slotId, newDate, newTime, newEndTime);
            return ResponseEntity.ok(Map.of("success", true, "message", "Slot rescheduled", "slot", slot));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ======================== Helpers ========================

    private Map<String, Object> buildSessionSummary(VideoKycSession s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("customerId", s.getCustomerId());
        map.put("fullName", s.getFullName());
        map.put("mobileNumber", s.getMobileNumber());
        map.put("email", s.getEmail());
        map.put("accountType", s.getAccountType());
        map.put("kycStatus", s.getKycStatus());
        map.put("temporaryAccountNumber", s.getTemporaryAccountNumber());
        map.put("finalAccountNumber", s.getFinalAccountNumber());
        map.put("roomId", s.getRoomId());
        map.put("kycAttemptCount", s.getKycAttemptCount());
        map.put("sessionActive", s.getSessionActive());
        map.put("createdAt", s.getCreatedAt());
        map.put("rejectionReason", s.getRejectionReason());
        map.put("assignedAdminName", s.getAssignedAdminName());
        map.put("verificationNumber", s.getVerificationNumber());
        map.put("slotDate", s.getSlotDate());
        map.put("slotTime", s.getSlotTime());
        map.put("slotEndTime", s.getSlotEndTime());
        map.put("bookedSlotId", s.getBookedSlotId());
        return map;
    }

    private Map<String, Object> buildSessionDetail(VideoKycSession s) {
        Map<String, Object> map = buildSessionSummary(s);
        map.put("addressCity", s.getAddressCity());
        map.put("addressState", s.getAddressState());
        map.put("aadharNumber", s.getAadharNumber());
        map.put("panNumber", s.getPanNumber());
        map.put("ifscCode", s.getIfscCode());
        map.put("otpVerified", s.getOtpVerified());
        map.put("livenessCheckPassed", s.getLivenessCheckPassed());
        map.put("livenessCheckType", s.getLivenessCheckType());
        map.put("maxAttempts", s.getMaxAttempts());
        map.put("sessionStartedAt", s.getSessionStartedAt());
        map.put("sessionEndedAt", s.getSessionEndedAt());
        map.put("sessionDurationSeconds", s.getSessionDurationSeconds());
        map.put("approvedAt", s.getApprovedAt());
        map.put("hasAadharDocument", s.getAadharDocument() != null);
        map.put("aadharDocumentName", s.getAadharDocumentName());
        map.put("hasPanDocument", s.getPanDocument() != null);
        map.put("panDocumentName", s.getPanDocumentName());
        map.put("hasFaceSnapshot", s.getFaceSnapshot() != null);
        map.put("hasIdSnapshot", s.getIdSnapshot() != null);
        map.put("verificationNumber", s.getVerificationNumber());
        return map;
    }
}
