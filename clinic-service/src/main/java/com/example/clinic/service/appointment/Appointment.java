package com.example.clinic.service.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(nullable = false, length = 240)
    private String reason;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Appointment() {
    }

    Appointment(UUID patientId, String reason, Instant scheduledAt) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.reason = reason;
        this.scheduledAt = scheduledAt;
        this.status = "SCHEDULED";
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
