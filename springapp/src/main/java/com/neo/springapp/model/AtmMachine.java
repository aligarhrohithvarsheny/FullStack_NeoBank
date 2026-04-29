package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_machines")
public class AtmMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atm_id", unique = true, nullable = false)
    private String atmId;

    @Column(name = "atm_name", nullable = false)
    private String atmName;

    @Column(nullable = false)
    private String location;

    private String city;
    private String state;
    private String pincode;

    @Column(length = 30)
    private String status = "ACTIVE";

    @Column(name = "cash_available")
    private Double cashAvailable = 0.0;

    @Column(name = "max_capacity")
    private Double maxCapacity = 5000000.0;

    @Column(name = "min_threshold")
    private Double minThreshold = 50000.0;

    @Column(name = "max_withdrawal_limit")
    private Double maxWithdrawalLimit = 25000.0;

    @Column(name = "daily_limit")
    private Double dailyLimit = 100000.0;

    @Column(name = "notes_500_count")
    private Integer notes500Count = 0;

    @Column(name = "notes_200_count")
    private Integer notes200Count = 0;

    @Column(name = "notes_100_count")
    private Integer notes100Count = 0;

    @Column(name = "notes_2000_count")
    private Integer notes2000Count = 0;

    @Column(name = "last_cash_loaded")
    private LocalDateTime lastCashLoaded;

    @Column(name = "last_serviced")
    private LocalDateTime lastServiced;

    @Column(name = "installed_date")
    private LocalDateTime installedDate;

    @Column(name = "managed_by")
    private String managedBy;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (installedDate == null) installedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }

    public String getAtmName() { return atmName; }
    public void setAtmName(String atmName) { this.atmName = atmName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getCashAvailable() { return cashAvailable; }
    public void setCashAvailable(Double cashAvailable) { this.cashAvailable = cashAvailable; }

    public Double getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Double maxCapacity) { this.maxCapacity = maxCapacity; }

    public Double getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Double minThreshold) { this.minThreshold = minThreshold; }

    public Double getMaxWithdrawalLimit() { return maxWithdrawalLimit; }
    public void setMaxWithdrawalLimit(Double maxWithdrawalLimit) { this.maxWithdrawalLimit = maxWithdrawalLimit; }

    public Double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }

    public Integer getNotes500Count() { return notes500Count; }
    public void setNotes500Count(Integer notes500Count) { this.notes500Count = notes500Count; }

    public Integer getNotes200Count() { return notes200Count; }
    public void setNotes200Count(Integer notes200Count) { this.notes200Count = notes200Count; }

    public Integer getNotes100Count() { return notes100Count; }
    public void setNotes100Count(Integer notes100Count) { this.notes100Count = notes100Count; }

    public Integer getNotes2000Count() { return notes2000Count; }
    public void setNotes2000Count(Integer notes2000Count) { this.notes2000Count = notes2000Count; }

    public LocalDateTime getLastCashLoaded() { return lastCashLoaded; }
    public void setLastCashLoaded(LocalDateTime lastCashLoaded) { this.lastCashLoaded = lastCashLoaded; }

    public LocalDateTime getLastServiced() { return lastServiced; }
    public void setLastServiced(LocalDateTime lastServiced) { this.lastServiced = lastServiced; }

    public LocalDateTime getInstalledDate() { return installedDate; }
    public void setInstalledDate(LocalDateTime installedDate) { this.installedDate = installedDate; }

    public String getManagedBy() { return managedBy; }
    public void setManagedBy(String managedBy) { this.managedBy = managedBy; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
