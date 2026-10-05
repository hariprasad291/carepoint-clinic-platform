package com.example.clinic.service.patient;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
}
