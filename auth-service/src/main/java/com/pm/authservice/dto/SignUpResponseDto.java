package com.pm.authservice.dto;

import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseDto {
    private UUID id;
    private String name;
    private String email;
}