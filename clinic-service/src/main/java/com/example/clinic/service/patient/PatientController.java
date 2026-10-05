package com.example.clinic.service.patient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic/patients")
public class PatientController {
    private final PatientRepository patientRepository;

    PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping
    public List<PatientResponse> listPatients() {
        return patientRepository.findAll().stream().map(PatientResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(@Valid @RequestBody PatientRequest request) {
        Patient patient = patientRepository.save(
                new Patient(request.fullName().trim(), request.email().trim().toLowerCase(), request.phone()));
        return PatientResponse.from(patient);
    }

    public record PatientRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 32) String phone) {
    }

    public record PatientResponse(UUID id, String fullName, String email, String phone, Instant createdAt) {
        static PatientResponse from(Patient patient) {
            return new PatientResponse(
                    patient.getId(), patient.getFullName(), patient.getEmail(), patient.getPhone(), patient.getCreatedAt());
        }
    }
}
