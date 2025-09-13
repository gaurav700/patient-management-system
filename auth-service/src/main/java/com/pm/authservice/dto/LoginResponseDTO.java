package com.pm.authservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
}
