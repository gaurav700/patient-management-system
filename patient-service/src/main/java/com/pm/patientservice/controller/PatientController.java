package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient management APIs")
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "Get all patients")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found all patients",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatientResponseDTO.class))})
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<PatientResponseDTO> getAllPatients() {
        return patientService.getPatients();
    }

    @Operation(summary = "Create a new patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatientResponseDTO.class))})
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponseDTO createPatient(@Valid @RequestBody PatientRequestDTO patientRequestDTO) {
        return patientService.createPatient(patientRequestDTO);
    }

    @Operation(summary = "Delete a patient by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient deleted successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatientResponseDTO.class))})
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PatientResponseDTO deletePatient(@PathVariable UUID id) {
        return patientService.deletePatient(id);
    }

    @Operation(summary = "Update a patient by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient updated successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatientResponseDTO.class))})
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PatientResponseDTO updatePatient(@PathVariable UUID id, @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
        return patientService.updatePatient(id, patientRequestDTO);
    }

}