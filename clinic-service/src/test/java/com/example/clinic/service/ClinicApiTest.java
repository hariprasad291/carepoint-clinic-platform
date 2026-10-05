package com.example.clinic.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.clinic.service.appointment.AppointmentRepository;
import com.example.clinic.service.patient.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ClinicApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void clearDatabase() {
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    void patientCanBeCreatedAndListed() throws Exception {
        MvcResult createPatient = mockMvc.perform(post("/api/clinic/patients")
                        .with(clinicUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Alex Morgan","email":"alex@example.com","phone":"555-0100"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Alex Morgan"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andReturn();

        UUID patientId = UUID.fromString(objectMapper.readTree(createPatient.getResponse().getContentAsString())
                .get("id").asText());
        mockMvc.perform(post("/api/clinic/appointments")
                        .with(clinicUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":"%s","reason":"Annual checkup","scheduledAt":"%s"}
                                """.formatted(patientId, Instant.now().plusSeconds(86400))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        mockMvc.perform(get("/api/clinic/patients").with(clinicUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Alex Morgan"));
        mockMvc.perform(get("/api/clinic/appointments").with(clinicUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Annual checkup"));
    }

    @Test
    void appointmentRequiresAnExistingPatient() throws Exception {
        mockMvc.perform(post("/api/clinic/appointments")
                        .with(clinicUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":"%s","reason":"Annual checkup","scheduledAt":"%s"}
                                """.formatted(UUID.randomUUID(), Instant.now().plusSeconds(86400))))
                .andExpect(status().isNotFound());
    }

    @Test
    void clinicEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/clinic/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientCreationRequiresWriteScope() throws Exception {
        mockMvc.perform(post("/api/clinic/patients")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_clinic.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Alex Morgan","email":"alex@example.com","phone":"555-0100"}
                                """))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor clinicUser() {
        return jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_clinic.read"),
                new SimpleGrantedAuthority("SCOPE_clinic.write"));
    }
}
