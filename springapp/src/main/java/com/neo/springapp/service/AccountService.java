package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    public Account getAccountByPan(String pan) {
        return accountRepository.findByPan(pan);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account updateBalance(String accountNumber, Double amount) {
        Account account = getAccountByNumber(accountNumber);
        if (account != null) {
            account.setBalance(account.getBalance() + amount);
            return accountRepository.save(account);
        }
        return null;
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }
}
