package com.neo.springapp.service;

import com.neo.springapp.model.SubscriptionPayment;
import com.neo.springapp.repository.SubscriptionPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class SubscriptionPaymentService {

    @Autowired
    private SubscriptionPaymentRepository subscriptionPaymentRepository;

    public SubscriptionPayment createSubscription(SubscriptionPayment subscription) {
        subscription.setNextBillingDate(subscription.getStartDate());
        subscription.setStatus("ACTIVE");
        return subscriptionPaymentRepository.save(subscription);
    }

    public List<SubscriptionPayment> getByEmployeeId(String employeeId) {
        return subscriptionPaymentRepository.findByEmployeeId(employeeId);
    }

    public List<SubscriptionPayment> getBySalaryAccountNumber(String accountNumber) {
        return subscriptionPaymentRepository.findBySalaryAccountNumber(accountNumber);
    }

    public Optional<SubscriptionPayment> getById(Long id) {
        return subscriptionPaymentRepository.findById(id);
    }

    public SubscriptionPayment pauseSubscription(Long id) {
        Optional<SubscriptionPayment> opt = subscriptionPaymentRepository.findById(id);
        if (opt.isPresent()) {
            SubscriptionPayment sub = opt.get();
            sub.setStatus("PAUSED");
            return subscriptionPaymentRepository.save(sub);
        }
        return null;
    }

    public SubscriptionPayment resumeSubscription(Long id) {
        Optional<SubscriptionPayment> opt = subscriptionPaymentRepository.findById(id);
        if (opt.isPresent()) {
            SubscriptionPayment sub = opt.get();
            sub.setStatus("ACTIVE");
            return subscriptionPaymentRepository.save(sub);
        }
        return null;
    }

    public SubscriptionPayment cancelSubscription(Long id) {
        Optional<SubscriptionPayment> opt = subscriptionPaymentRepository.findById(id);
        if (opt.isPresent()) {
            SubscriptionPayment sub = opt.get();
            sub.setStatus("CANCELLED");
            sub.setEndDate(LocalDate.now());
            return subscriptionPaymentRepository.save(sub);
        }
        return null;
    }

    public SubscriptionPayment update(SubscriptionPayment subscription) {
        if (subscriptionPaymentRepository.existsById(subscription.getId())) {
            return subscriptionPaymentRepository.save(subscription);
        }
        return null;
    }

    public boolean deleteSubscription(Long id) {
        if (subscriptionPaymentRepository.existsById(id)) {
            subscriptionPaymentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<SubscriptionPayment> getDueBillings() {
        return subscriptionPaymentRepository.findByStatusAndNextBillingDateLessThanEqual("ACTIVE", LocalDate.now());
    }

    public List<SubscriptionPayment> getAll() {
        return subscriptionPaymentRepository.findAll();
    }

    public List<SubscriptionPayment> getActiveByAccount(String accountNumber) {
        return subscriptionPaymentRepository.findBySalaryAccountNumberAndStatus(accountNumber, "ACTIVE");
    }
}
