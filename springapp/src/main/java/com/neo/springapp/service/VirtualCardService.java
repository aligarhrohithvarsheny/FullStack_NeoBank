package com.neo.springapp.service;

import com.neo.springapp.model.VirtualCard;
import com.neo.springapp.repository.VirtualCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class VirtualCardService {

    @Autowired
    private VirtualCardRepository virtualCardRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public VirtualCard createVirtualCard(String accountNumber, String cardholderName) {
        VirtualCard card = new VirtualCard();
        card.setAccountNumber(accountNumber);
        card.setCardholderName(cardholderName);
        card.setCardNumber(generateCardNumber());
        card.setCvv(generateCVV());
        card.setExpiryDate(LocalDate.now().plusYears(3));
        return virtualCardRepository.save(card);
    }

    public List<VirtualCard> getCardsByAccountNumber(String accountNumber) {
        return virtualCardRepository.findByAccountNumber(accountNumber);
    }

    public Optional<VirtualCard> getCardById(Long id) {
        return virtualCardRepository.findById(id);
    }

    public Optional<VirtualCard> getCardByNumber(String cardNumber) {
        return virtualCardRepository.findByCardNumber(cardNumber);
    }

    public VirtualCard freezeCard(Long id) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            card.setStatus("FROZEN");
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public VirtualCard unfreezeCard(Long id) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            card.setStatus("ACTIVE");
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public VirtualCard cancelCard(Long id) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            card.setStatus("CANCELLED");
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public VirtualCard updateLimits(Long id, Double dailyLimit, Double monthlyLimit) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            if (dailyLimit != null) card.setDailyLimit(dailyLimit);
            if (monthlyLimit != null) card.setMonthlyLimit(monthlyLimit);
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public VirtualCard toggleOnlinePayments(Long id, boolean enabled) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            card.setOnlinePaymentsEnabled(enabled);
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public VirtualCard toggleInternationalPayments(Long id, boolean enabled) {
        Optional<VirtualCard> opt = virtualCardRepository.findById(id);
        if (opt.isPresent()) {
            VirtualCard card = opt.get();
            card.setInternationalPaymentsEnabled(enabled);
            return virtualCardRepository.save(card);
        }
        return null;
    }

    public List<VirtualCard> getAll() {
        return virtualCardRepository.findAll();
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("4");
        for (int i = 1; i < 16; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        String candidate = sb.toString();
        while (virtualCardRepository.existsByCardNumber(candidate)) {
            sb = new StringBuilder("4");
            for (int i = 1; i < 16; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            candidate = sb.toString();
        }
        return candidate;
    }

    private String generateCVV() {
        return String.format("%03d", secureRandom.nextInt(1000));
    }
}
