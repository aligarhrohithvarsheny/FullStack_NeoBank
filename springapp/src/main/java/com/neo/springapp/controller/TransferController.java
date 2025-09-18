package com.neo.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.service.TransferService;

@RestController
@RequestMapping("/api/transfers")
@CrossOrigin(origins = "http://localhost:4200") // ✅ allow Angular frontend
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // POST: Save a new transfer
    @PostMapping
    public ResponseEntity<TransferRecord> createTransfer(@RequestBody TransferRecord transfer) {
        TransferRecord saved = transferService.saveTransfer(transfer);
        return ResponseEntity.ok(saved);
    }

    // GET: Fetch all transfers
    @GetMapping
    public ResponseEntity<List<TransferRecord>> getAllTransfers() {
        return ResponseEntity.ok(transferService.getAllTransfers());
    }

    // GET: Get transfer by ID
    @GetMapping("/{id}")
    public ResponseEntity<TransferRecord> getTransferById(@PathVariable Long id) {
        TransferRecord transfer = transferService.getTransferById(id);
        if (transfer != null) {
            return ResponseEntity.ok(transfer);
        }
        return ResponseEntity.notFound().build();
    }

    // PUT: Update transfer
    @PutMapping("/{id}")
    public ResponseEntity<TransferRecord> updateTransfer(@PathVariable Long id, @RequestBody TransferRecord transferDetails) {
        TransferRecord updatedTransfer = transferService.updateTransfer(id, transferDetails);
        if (updatedTransfer != null) {
            return ResponseEntity.ok(updatedTransfer);
        }
        return ResponseEntity.notFound().build();
    }

    // DELETE: Delete transfer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        boolean deleted = transferService.deleteTransfer(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
