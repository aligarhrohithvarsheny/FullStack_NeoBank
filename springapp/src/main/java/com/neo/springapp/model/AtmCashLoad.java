package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_cash_loads")
public class AtmCashLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atm_id", nullable = false)
    private String atmId;

    @Column(name = "loaded_by", nullable = false)
    private String loadedBy;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "notes_500")
    private Integer notes500 = 0;

    @Column(name = "notes_200")
    private Integer notes200 = 0;

    @Column(name = "notes_100")
    private Integer notes100 = 0;

    @Column(name = "notes_2000")
    private Integer notes2000 = 0;

    @Column(name = "previous_balance")
    private Double previousBalance;

    @Column(name = "new_balance")
    private Double newBalance;

    private String remarks;

    @Column(name = "loaded_at")
    private LocalDateTime loadedAt;

    @PrePersist
    protected void onCreate() {
        if (loadedAt == null) loadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }

    public String getLoadedBy() { return loadedBy; }
    public void setLoadedBy(String loadedBy) { this.loadedBy = loadedBy; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Integer getNotes500() { return notes500; }
    public void setNotes500(Integer notes500) { this.notes500 = notes500; }

    public Integer getNotes200() { return notes200; }
    public void setNotes200(Integer notes200) { this.notes200 = notes200; }

    public Integer getNotes100() { return notes100; }
    public void setNotes100(Integer notes100) { this.notes100 = notes100; }

    public Integer getNotes2000() { return notes2000; }
    public void setNotes2000(Integer notes2000) { this.notes2000 = notes2000; }

    public Double getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(Double previousBalance) { this.previousBalance = previousBalance; }

    public Double getNewBalance() { return newBalance; }
    public void setNewBalance(Double newBalance) { this.newBalance = newBalance; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getLoadedAt() { return loadedAt; }
    public void setLoadedAt(LocalDateTime loadedAt) { this.loadedAt = loadedAt; }
}
