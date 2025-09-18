package com.neo.springapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "kyc_requests")
public class KycRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String panNumber;
    private String name;
    private String status; // "Pending", "Approved", "Rejected"

    public KycRequest() {}

    public KycRequest(String panNumber, String name, String status) {
        this.panNumber = panNumber;
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
