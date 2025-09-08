package com.pm.patientservice.service.impl;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import com.pm.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pm.patientservice.kafka.kafkaProducer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final kafkaProducer kafkaProducer;

    @Override
    public List<PatientResponseDTO> getPatients() {
        log.info("Fetching all patients");
        List<Patient> patients = patientRepository.findAll();
        log.debug("Retrieved {} patients from database", patients.size());
        return patients.stream().map(PatientMapper::toDTO).toList();
    }

    @Override
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        log.info("Attempting to create new patient with email: {}", patientRequestDTO.getEmail());

        if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            log.error("Email already exists: {}", patientRequestDTO.getEmail());
            throw new EmailAlreadyExistsException("Patient with this email id already exists : " + patientRequestDTO.getEmail());
        }

        Patient patient = PatientMapper.toModel(patientRequestDTO);
        Patient savedPatient = patientRepository.save(patient);

        log.info("Successfully created patient with ID: {}", savedPatient.getId());
        log.debug("Patient details: name={}, email={}", savedPatient.getName(), savedPatient.getEmail());

        log.info("Creating billing account for patient with ID: {}", savedPatient.getId());
        billingServiceGrpcClient.createBillingAccount(savedPatient.getId().toString(), savedPatient.getName(), savedPatient.getEmail());
        log.info("Billing account created for patient with ID: {}", savedPatient.getId());

        log.info("Creating patient event for patient with ID: {}", savedPatient.getId());
        kafkaProducer.sendEvent(savedPatient);
        log.info("Patient event created for patient with ID: {}", savedPatient.getId());

        return PatientMapper.toDTO(savedPatient);
    }

    @Override
    public PatientResponseDTO deletePatient(UUID id) {
        log.info("Attempting to delete patient with ID: {}", id);
        Patient patient = getPatientWithID(id);
        patientRepository.deleteById(id);
        log.info("Successfully deleted patient with ID: {}", id);
        log.debug("Deleted patient details: name={}, email={}", patient.getName(), patient.getEmail());
        return PatientMapper.toDTO(patient);
    }

    @Override
    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        log.info("Attempting to update patient with ID: {}", id);
        Patient patient = getPatientWithID(id);

        log.debug("Updating patient details - Old values: name={}, email={}, address={}",
                patient.getName(), patient.getEmail(), patient.getAddress());

        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setRegisteredDate(LocalDate.parse(patientRequestDTO.getRegisteredDate()));

        Patient updatedPatient = patientRepository.save(patient);
        log.info("Successfully updated patient with ID: {}", id);
        log.debug("Updated patient details: name={}, email={}, address={}",
                updatedPatient.getName(), updatedPatient.getEmail(), updatedPatient.getAddress());

        return PatientMapper.toDTO(updatedPatient);
    }

    public Patient getPatientWithID(UUID id) {
        log.debug("Fetching patient with ID: {}", id);
        try {
            Patient patient = patientRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Patient with ID " + id + " not found"));
            log.debug("Found patient: name={}, email={}", patient.getName(), patient.getEmail());
            return patient;
        } catch (NoSuchElementException e) {
            log.error("Patient not found with ID: {}", id);
            throw e;
        }
    }
}