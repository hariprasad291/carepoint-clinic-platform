package com.example.clinic.service.appointment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
