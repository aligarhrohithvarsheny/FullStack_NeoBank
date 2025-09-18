package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // Create new account
    @PostMapping("/create")
    public Account createAccount(@RequestBody Account account) {
        return accountService.saveAccount(account);
    }

    // Get account by PAN
    @GetMapping("/pan/{pan}")
    public Account getAccountByPan(@PathVariable String pan) {
        return accountService.getAccountByPan(pan);
    }

    // Get account by Account Number
    @GetMapping("/number/{accountNumber}")
    public Account getAccountByNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }
}
