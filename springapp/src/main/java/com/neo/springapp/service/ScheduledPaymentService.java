package com.neo.springapp.service;

import com.neo.springapp.model.ScheduledPayment;
import com.neo.springapp.repository.ScheduledPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class ScheduledPaymentService {

    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;

    public ScheduledPayment createScheduledPayment(ScheduledPayment payment) {
        payment.setNextPaymentDate(payment.getStartDate());
        return scheduledPaymentRepository.save(payment);
    }

    public List<ScheduledPayment> getByAccountNumber(String accountNumber) {
        return scheduledPaymentRepository.findByAccountNumber(accountNumber);
    }

    public List<ScheduledPayment> getActiveByAccountNumber(String accountNumber) {
        return scheduledPaymentRepository.findByAccountNumberAndStatus(accountNumber, "ACTIVE");
    }

    public Optional<ScheduledPayment> getById(Long id) {
        return scheduledPaymentRepository.findById(id);
    }

    public ScheduledPayment updateStatus(Long id, String status) {
        Optional<ScheduledPayment> opt = scheduledPaymentRepository.findById(id);
        if (opt.isPresent()) {
            ScheduledPayment sp = opt.get();
            sp.setStatus(status);
            return scheduledPaymentRepository.save(sp);
        }
        return null;
    }

    public ScheduledPayment pausePayment(Long id) {
        return updateStatus(id, "PAUSED");
    }

    public ScheduledPayment resumePayment(Long id) {
        return updateStatus(id, "ACTIVE");
    }

    public ScheduledPayment cancelPayment(Long id) {
        return updateStatus(id, "CANCELLED");
    }

    public ScheduledPayment update(ScheduledPayment payment) {
        if (scheduledPaymentRepository.existsById(payment.getId())) {
            return scheduledPaymentRepository.save(payment);
        }
        return null;
    }

    public boolean deletePayment(Long id) {
        if (scheduledPaymentRepository.existsById(id)) {
            scheduledPaymentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ScheduledPayment> getDuePayments() {
        return scheduledPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", LocalDate.now());
    }

    public List<ScheduledPayment> getAll() {
        return scheduledPaymentRepository.findAll();
    }

    public LocalDate calculateNextPaymentDate(LocalDate current, String frequency) {
        switch (frequency) {
            case "DAILY": return current.plusDays(1);
            case "WEEKLY": return current.plusWeeks(1);
            case "MONTHLY": return current.plusMonths(1);
            case "QUARTERLY": return current.plusMonths(3);
            case "YEARLY": return current.plusYears(1);
            default: return current.plusMonths(1);
        }
    }
}
