package com.example.clinic.service.patient;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Patient() {
    }

    Patient(String fullName, String email, String phone) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
