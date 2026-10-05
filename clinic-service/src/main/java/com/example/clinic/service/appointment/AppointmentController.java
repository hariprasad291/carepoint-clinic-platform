package com.example.clinic.service.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.clinic.service.patient.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic/appointments")
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    AppointmentController(AppointmentRepository appointmentRepository, PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping
    public List<AppointmentResponse> listAppointments() {
        return appointmentRepository.findAll().stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentRequest request) {
        if (!patientRepository.existsById(request.patientId())) {
            throw new EntityNotFoundException("Patient not found: " + request.patientId());
        }
        Appointment appointment = appointmentRepository.save(
                new Appointment(request.patientId(), request.reason().trim(), request.scheduledAt()));
        return AppointmentResponse.from(appointment);
    }

    public record AppointmentRequest(
            @NotNull UUID patientId,
            @NotBlank @Size(max = 240) String reason,
            @NotNull @Future Instant scheduledAt) {
    }

    public record AppointmentResponse(
            UUID id, UUID patientId, String reason, Instant scheduledAt, String status, Instant createdAt) {
        static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(
                    appointment.getId(),
                    appointment.getPatientId(),
                    appointment.getReason(),
                    appointment.getScheduledAt(),
                    appointment.getStatus(),
                    appointment.getCreatedAt());
        }
    }
}
