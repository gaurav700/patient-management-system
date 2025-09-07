package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    List<PatientResponseDTO> getPatients();

    PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO);

    PatientResponseDTO deletePatient(UUID id);

    PatientResponseDTO updatePatient(UUID id, @Valid PatientRequestDTO patientRequestDTO);
}
