package com.neo.springapp.controller;

import com.neo.springapp.dto.*;
import com.neo.springapp.model.PositivePayRequest;
import com.neo.springapp.model.PositivePayStatus;
import com.neo.springapp.service.PositivePayService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/positive-pay")
public class AdminPositivePayController {
    private final PositivePayService service;
    public AdminPositivePayController(PositivePayService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required=false) PositivePayStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
        Page<PositivePayRequest> result=service.adminList(status,PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100)));
        return ResponseEntity.ok(result.map(PositivePayResponse::from));
    }
    @GetMapping("/stats") public ResponseEntity<Map<String,Object>> stats(){return ResponseEntity.ok(service.adminStats());}
    @GetMapping("/{referenceNumber}") public ResponseEntity<?> details(@PathVariable String referenceNumber){return ResponseEntity.ok(service.adminGet(referenceNumber));}
    @PatchMapping("/{referenceNumber}/approve") public ResponseEntity<?> approve(@PathVariable String referenceNumber,@RequestBody(required=false) PositivePayApprovalRequest body,@RequestHeader(value="X-Admin-Name",defaultValue="Admin") String admin,@RequestHeader(value="X-Forwarded-For",required=false) String ip){String by=body!=null&&body.performedBy()!=null?body.performedBy():admin;return ResponseEntity.ok(service.approve(referenceNumber,by,body==null?null:body.remark(),ip));}
    @PatchMapping("/{referenceNumber}/reject") public ResponseEntity<?> reject(@PathVariable String referenceNumber,@Valid @RequestBody PositivePayRejectionRequest body,@RequestHeader(value="X-Admin-Name",defaultValue="Admin") String admin,@RequestHeader(value="X-Forwarded-For",required=false) String ip){String by=body.performedBy()!=null?body.performedBy():admin;return ResponseEntity.ok(service.reject(referenceNumber,by,body.reason(),ip));}
}
