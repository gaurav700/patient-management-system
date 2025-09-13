package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

import java.util.Arrays;

@Slf4j
@Service
public class KafkaConsumer {

    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void listen(byte[] message) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(message);
            // perform any action with the patient event

            log.info("Received patient event: [ PatientId = {}, PatientName={}, PatientEmail={} ]",
                    patientEvent.getPatientId(),
                    patientEvent.getName(),
                    patientEvent.getEmail());

        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing message: {}", e.getMessage());
        }
    }

}
