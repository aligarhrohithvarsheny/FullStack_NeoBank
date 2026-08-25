package com.neo.springapp.controller;

import com.neo.springapp.model.TermDepositPlan;
import com.neo.springapp.repository.TermDepositPlanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/term-deposit-plans")
public class TermDepositPlanController {
    private final TermDepositPlanRepository repository;

    public TermDepositPlanController(TermDepositPlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TermDepositPlan> activePlans() {
        return repository.findByActiveTrueOrderByDaysAsc();
    }

    @GetMapping("/all")
    public List<TermDepositPlan> allPlans() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<TermDepositPlan> create(@RequestBody TermDepositPlan plan) {
        if (plan.getSchemeName() == null || plan.getSchemeName().isBlank() || plan.getDays() == null || plan.getDays() <= 0
                || plan.getInterestRate() == null || plan.getInterestRate() < 0) {
            return ResponseEntity.badRequest().build();
        }
        plan.setId(null);
        plan.setActive(true);
        return ResponseEntity.ok(repository.save(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TermDepositPlan> update(@PathVariable Long id, @RequestBody TermDepositPlan updates) {
        return repository.findById(id).map(plan -> {
            plan.setSchemeName(updates.getSchemeName());
            plan.setDays(updates.getDays());
            plan.setYears(updates.getYears());
            plan.setMinimumAmount(updates.getMinimumAmount());
            plan.setMaximumAmount(updates.getMaximumAmount());
            plan.setInterestRate(updates.getInterestRate());
            plan.setPayoutDate(updates.getPayoutDate());
            plan.setInterestAmount(updates.getInterestAmount());
            plan.setActive(updates.getActive() == null || updates.getActive());
            return ResponseEntity.ok(repository.save(plan));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        return repository.findById(id).map(plan -> {
            plan.setActive(false);
            repository.save(plan);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
