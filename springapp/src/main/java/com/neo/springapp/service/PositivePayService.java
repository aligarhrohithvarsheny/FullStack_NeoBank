package com.neo.springapp.service;

import com.neo.springapp.dto.*;
import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PositivePayService {
    private final PositivePayRequestRepository requestRepository;
    private final PositivePayAuditLogRepository auditRepository;
    private final ChequeRepository chequeRepository;
    private final ChequeRequestRepository salaryChequeRepository;
    private final BusinessChequeRequestRepository businessChequeRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;

    @Value("${positivepay.minimum.amount:10000}")
    private BigDecimal minimumAmount;

    public PositivePayService(PositivePayRequestRepository requestRepository,
                              PositivePayAuditLogRepository auditRepository,
                              ChequeRepository chequeRepository,
                              ChequeRequestRepository salaryChequeRepository,
                              BusinessChequeRequestRepository businessChequeRepository,
                              SalaryAccountRepository salaryAccountRepository,
                              CurrentAccountRepository currentAccountRepository,
                              AccountRepository accountRepository,
                              UserRepository userRepository,
                              AccountService accountService) {
        this.requestRepository = requestRepository; this.auditRepository = auditRepository;
        this.chequeRepository = chequeRepository; this.salaryChequeRepository = salaryChequeRepository;
        this.businessChequeRepository = businessChequeRepository; this.salaryAccountRepository = salaryAccountRepository;
        this.currentAccountRepository = currentAccountRepository; this.accountRepository = accountRepository;
        this.userRepository = userRepository; this.accountService = accountService;
    }

    public BigDecimal getMinimumAmount() { return minimumAmount; }

    public List<Map<String, Object>> eligibleCheques(String accountNumber, Long claimedUserId) {
        Owner owner = resolveOwner(accountNumber);
        if (owner == null) throw new IllegalArgumentException("Account not found");
        if (claimedUserId == null || owner.userId == null || !claimedUserId.equals(owner.userId)) throw new SecurityException("Account access denied");
        List<Map<String, Object>> result = new ArrayList<>();
        if (owner.type.equals("SAVINGS")) {
            for (Cheque cheque : chequeRepository.findByAccountNumberAndStatus(accountNumber, "ACTIVE")) result.add(chequeMap(cheque.getId(), "CHEQUES", cheque.getChequeNumber(), accountNumber, owner.type, owner.name, cheque.getAmount(), cheque.getStatus(), null, cheque.getAmount() != null && BigDecimal.valueOf(cheque.getAmount()).compareTo(minimumAmount) >= 0));
        } else if (owner.type.equals("SALARY")) {
            SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary != null) for (ChequeRequest cheque : salaryChequeRepository.findBySalaryAccountIdOrderByCreatedAtDesc(salary.getId(), org.springframework.data.domain.PageRequest.of(0, 200)).getContent()) if ("APPROVED".equalsIgnoreCase(cheque.getStatus()) || "PENDING".equalsIgnoreCase(cheque.getStatus())) result.add(chequeMap(cheque.getId(), "SALARY_CHEQUE_REQUESTS", cheque.getChequeNumber(), accountNumber, owner.type, owner.name, cheque.getAmount() == null ? null : cheque.getAmount().doubleValue(), cheque.getStatus(), cheque.getSerialNumber(), cheque.getAmount() != null && cheque.getAmount().compareTo(minimumAmount) >= 0));
        } else {
            CurrentAccount current = currentAccountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new IllegalArgumentException("Current account not found"));
            for (BusinessChequeRequest cheque : businessChequeRepository.findByCurrentAccountIdOrderByCreatedAtDesc(current.getId(), org.springframework.data.domain.PageRequest.of(0, 200)).getContent()) if ("APPROVED".equalsIgnoreCase(cheque.getStatus()) || "PENDING".equalsIgnoreCase(cheque.getStatus())) result.add(chequeMap(cheque.getId(), "BUSINESS_CHEQUE_REQUESTS", cheque.getChequeNumber(), accountNumber, owner.type, owner.name, cheque.getAmount() == null ? null : cheque.getAmount().doubleValue(), cheque.getStatus(), cheque.getSerialNumber(), cheque.getAmount() != null && cheque.getAmount().compareTo(minimumAmount) >= 0));
        }
        for (Map<String, Object> item : result) {
            String number = String.valueOf(item.get("chequeNumber"));
            item.put("positivePayStatus", requestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(accountNumber, number, List.of(PositivePayStatus.PENDING_ADMIN_APPROVAL, PositivePayStatus.APPROVED)).map(r -> r.getStatus().name()).orElse("NOT_REGISTERED"));
        }
        return result;
    }

    @Transactional
    public PositivePayResponse create(PositivePayCreateRequest input, Long claimedUserId, String ip) {
        Owner owner = resolveOwner(input.accountNumber());
        if (owner == null) throw new IllegalArgumentException("Account not found");
        if (claimedUserId != null && owner.userId != null && !claimedUserId.equals(owner.userId)) throw new SecurityException("Account does not belong to the logged-in customer");
        ResolvedCheque cheque = resolveCheque(owner, input.chequeNumber());
        if (cheque == null || !cheque.available) throw new IllegalArgumentException("Cheque does not belong to this account or is not available");
        if (input.amount().compareTo(minimumAmount) < 0) throw new IllegalArgumentException("Positive Pay registration is not required for this cheque amount. Minimum: ₹" + minimumAmount);
        if (input.chequeDate() == null || input.chequeDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("Cheque date cannot be in the future");
        if (requestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(input.accountNumber(), input.chequeNumber(), List.of(PositivePayStatus.PENDING_ADMIN_APPROVAL, PositivePayStatus.APPROVED)).isPresent()) throw new IllegalArgumentException("Positive Pay registration already exists for this cheque");
        PositivePayRequest request = new PositivePayRequest();
        request.setReferenceNumber("PP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%04d", (System.currentTimeMillis() % 10000)));
        request.setUserId(owner.userId); request.setAccountId(owner.accountId); request.setAccountNumber(input.accountNumber()); request.setAccountType(owner.type); request.setAccountHolderName(owner.name); request.setCustomerEmail(owner.email); request.setCustomerPhone(owner.phone);
        request.setChequeSource(cheque.source); request.setChequeId(cheque.id); request.setChequeNumber(cheque.number); request.setChequeStatus(cheque.status); request.setChequeBookNumber(cheque.bookNumber); request.setChequeDate(input.chequeDate()); request.setAmount(input.amount()); request.setPayeeName(input.payeeName()); request.setPayeeAccountNumber(input.payeeAccountNumber()); request.setPayeeBankName(input.payeeBankName()); request.setPayeeIfsc(input.payeeIfsc()); request.setRemarks(input.remarks()); request.setStatus(PositivePayStatus.PENDING_ADMIN_APPROVAL);
        PositivePayRequest saved = requestRepository.save(request); audit(saved, "CUSTOMER_SUBMITTED", owner.name, "CUSTOMER", ip, input.remarks()); return PositivePayResponse.from(saved);
    }

    public List<PositivePayResponse> byAccount(String accountNumber, Long claimedUserId) { Owner owner = resolveOwner(accountNumber); if (owner == null || (claimedUserId != null && owner.userId != null && !claimedUserId.equals(owner.userId))) throw new SecurityException("Account access denied"); return requestRepository.findByAccountNumberOrderBySubmittedAtDesc(accountNumber).stream().map(PositivePayResponse::from).toList(); }
    public PositivePayResponse get(String reference, Long claimedUserId, boolean admin) { PositivePayRequest r = requestRepository.findByReferenceNumber(reference).orElseThrow(() -> new NoSuchElementException("Positive Pay request not found")); if (!admin && claimedUserId != null && r.getUserId() != null && !claimedUserId.equals(r.getUserId())) throw new SecurityException("Request access denied"); if (!admin) audit(r, "CUSTOMER_VIEWED", String.valueOf(claimedUserId), "CUSTOMER", null, null); return PositivePayResponse.from(r); }

    @Transactional public PositivePayResponse cancel(String reference, Long userId, String ip) { PositivePayRequest r = requestRepository.findByReferenceNumber(reference).orElseThrow(); if (userId != null && r.getUserId() != null && !userId.equals(r.getUserId())) throw new SecurityException("Request access denied"); if (r.getStatus() != PositivePayStatus.PENDING_ADMIN_APPROVAL) throw new IllegalArgumentException("Only pending requests can be cancelled"); r.setStatus(PositivePayStatus.CANCELLED); r.setCancelledAt(LocalDateTime.now()); PositivePayRequest saved = requestRepository.save(r); audit(saved, "CUSTOMER_CANCELLED", String.valueOf(userId), "CUSTOMER", ip, null); return PositivePayResponse.from(saved); }
    public Page<PositivePayRequest> adminList(PositivePayStatus status, Pageable pageable) { return status == null ? requestRepository.findAll(pageable) : requestRepository.findByStatus(status, pageable); }
    public Map<String, Object> adminStats() { LocalDateTime start = LocalDate.now().atStartOfDay(); LocalDateTime end = start.plusDays(1); Map<String,Object> m = new LinkedHashMap<>(); m.put("pending", requestRepository.countByStatus(PositivePayStatus.PENDING_ADMIN_APPROVAL)); m.put("approved", requestRepository.countByStatus(PositivePayStatus.APPROVED)); m.put("rejected", requestRepository.countByStatus(PositivePayStatus.REJECTED)); m.put("cancelled", requestRepository.countByStatus(PositivePayStatus.CANCELLED)); m.put("today", requestRepository.countBySubmittedAtBetween(start,end)); m.put("minimumAmount", minimumAmount); m.put("totalPositivePayAmount", requestRepository.findAll().stream().filter(r -> r.getAmount()!=null).map(PositivePayRequest::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)); return m; }
    public PositivePayAdminResponse adminGet(String reference) { PositivePayRequest r = requestRepository.findByReferenceNumber(reference).orElseThrow(); Owner o = resolveOwner(r.getAccountNumber()); return PositivePayAdminResponse.from(r, o == null ? "UNKNOWN" : o.status); }
    @Transactional public PositivePayAdminResponse approve(String reference, String by, String remark, String ip) { PositivePayRequest r = pending(reference); ResolvedCheque c = resolveCheque(resolveOwner(r.getAccountNumber()), r.getChequeNumber()); if (c == null || !c.available) throw new IllegalArgumentException("Cheque is no longer available"); r.setStatus(PositivePayStatus.APPROVED); r.setApprovedAt(LocalDateTime.now()); r.setApprovedBy(by); r.setRemarks(remark); PositivePayRequest saved=requestRepository.save(r); linkReceiverToDrawRequest(saved); audit(saved,"ADMIN_APPROVED",by,"ADMIN",ip,remark); return adminGet(saved.getReferenceNumber()); }

    private void linkReceiverToDrawRequest(PositivePayRequest request) {
        if (request.getChequeId() == null || request.getPayeeAccountNumber() == null || request.getPayeeAccountNumber().isBlank()) return;
        if ("SALARY_CHEQUE_REQUESTS".equals(request.getChequeSource())) {
            salaryChequeRepository.findById(request.getChequeId()).ifPresent(draw -> {
                draw.setPayeeAccountNumber(request.getPayeeAccountNumber());
                draw.setPayeeAccountVerified(true);
                draw.setPayeeAccountType(request.getPayeeBankName());
                salaryChequeRepository.save(draw);
            });
        } else if ("BUSINESS_CHEQUE_REQUESTS".equals(request.getChequeSource())) {
            businessChequeRepository.findById(request.getChequeId()).ifPresent(draw -> {
                draw.setPayeeAccountNumber(request.getPayeeAccountNumber());
                draw.setPayeeAccountVerified(true);
                draw.setPayeeAccountType(request.getPayeeBankName());
                businessChequeRepository.save(draw);
            });
        }
    }
    @Transactional public PositivePayAdminResponse reject(String reference, String by, String reason, String ip) { PositivePayRequest r = pending(reference); r.setStatus(PositivePayStatus.REJECTED); r.setRejectedAt(LocalDateTime.now()); r.setRejectedBy(by); r.setRejectionReason(reason); PositivePayRequest saved=requestRepository.save(r); audit(saved,"ADMIN_REJECTED",by,"ADMIN",ip,reason); return adminGet(saved.getReferenceNumber()); }

    @Transactional public Map<String,Object> matchPresented(String accountNumber, String chequeNumber, LocalDate date, BigDecimal amount, String payeeName, String ip) { PositivePayRequest r=requestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(accountNumber,chequeNumber,List.of(PositivePayStatus.APPROVED)).orElseThrow(() -> new IllegalArgumentException("No approved Positive Pay registration found")); boolean matched=r.getChequeDate().equals(date) && r.getAmount().compareTo(amount)==0 && r.getPayeeName().equalsIgnoreCase(payeeName); r.setStatus(matched?PositivePayStatus.MATCHED:PositivePayStatus.MISMATCH); requestRepository.save(r); audit(r,matched?"CHEQUE_DETAILS_MATCHED":"CHEQUE_DETAILS_MISMATCHED","SYSTEM","SYSTEM",ip,matched?null:"Presented details did not match registration"); return Map.of("matched",matched,"status",r.getStatus().name(),"referenceNumber",r.getReferenceNumber()); }

    private PositivePayRequest pending(String reference) { PositivePayRequest r=requestRepository.findByReferenceNumber(reference).orElseThrow(); if(r.getStatus()!=PositivePayStatus.PENDING_ADMIN_APPROVAL) throw new IllegalArgumentException("Only pending requests can be processed"); return r; }
    private void audit(PositivePayRequest r,String action,String by,String role,String ip,String remarks){ PositivePayAuditLog l=new PositivePayAuditLog(); l.setReferenceNumber(r.getReferenceNumber()); l.setAction(action); l.setPerformedBy(by); l.setPerformedByRole(role); l.setIpAddress(ip); l.setRemarks(remarks); auditRepository.save(l); }
    private Map<String,Object> chequeMap(Long id,String source,String number,String account,String type,String holder,Double amount,String status,String book,boolean eligible){ Map<String,Object> m=new LinkedHashMap<>(); m.put("chequeId",id);m.put("chequeSource",source);m.put("chequeNumber",number);m.put("accountNumber",account);m.put("accountType",type);m.put("accountHolderName",holder);m.put("amount",amount);m.put("chequeStatus",status);m.put("chequeBookNumber",book);m.put("eligible",eligible);m.put("minimumAmount",minimumAmount);return m; }
    private ResolvedCheque resolveCheque(Owner owner,String number){ if(owner==null)return null; if(owner.type.equals("SAVINGS")){return chequeRepository.findByChequeNumber(number).filter(c->owner.number.equalsIgnoreCase(c.getAccountNumber())).map(c->new ResolvedCheque(c.getId(),"CHEQUES",c.getChequeNumber(),c.getStatus(),"ACTIVE".equalsIgnoreCase(c.getStatus()),null)).orElse(null);} if(owner.type.equals("SALARY")){SalaryAccount sa=salaryAccountRepository.findByAccountNumber(owner.number); if(sa==null)return null; return salaryChequeRepository.findAllByChequeNumber(number).stream().filter(c->Objects.equals(c.getSalaryAccountId(),sa.getId())).findFirst().map(c->new ResolvedCheque(c.getId(),"SALARY_CHEQUE_REQUESTS",c.getChequeNumber(),c.getStatus(),"APPROVED".equalsIgnoreCase(c.getStatus()) || "PENDING".equalsIgnoreCase(c.getStatus()),c.getSerialNumber())).orElse(null);} CurrentAccount ca=currentAccountRepository.findByAccountNumber(owner.number).orElse(null); if(ca==null)return null; return businessChequeRepository.findAllByChequeNumber(number).stream().filter(c->Objects.equals(c.getCurrentAccountId(),ca.getId())).findFirst().map(c->new ResolvedCheque(c.getId(),"BUSINESS_CHEQUE_REQUESTS",c.getChequeNumber(),c.getStatus(),"APPROVED".equalsIgnoreCase(c.getStatus()) || "PENDING".equalsIgnoreCase(c.getStatus()),c.getSerialNumber())).orElse(null); }
    private Owner resolveOwner(String number){ if(number==null)return null; User user=userRepository.findByAccountNumber(number).orElse(null); Account a=accountRepository.findByAccountNumber(number); if(a!=null)return new Owner(number,"SAVINGS",a.getName(),a.getStatus(),a.getId(),user==null?a.getId():user.getId(),user==null?null:user.getEmail(),null); SalaryAccount sa=salaryAccountRepository.findByAccountNumber(number); if(sa!=null)return new Owner(number,"SALARY",sa.getEmployeeName(),sa.getStatus(),sa.getId(),user==null?sa.getId():user.getId(),sa.getEmail(),sa.getMobileNumber()); CurrentAccount ca=currentAccountRepository.findByAccountNumber(number).orElse(null); if(ca!=null)return new Owner(number,"CURRENT",ca.getOwnerName(),ca.getStatus(),ca.getId(),user==null?ca.getId():user.getId(),ca.getEmail(),ca.getMobile()); return null; }
    private record Owner(String number,String type,String name,String status,Long accountId,Long userId,String email,String phone){}
    private record ResolvedCheque(Long id,String source,String number,String status,boolean available,String bookNumber){}
}
