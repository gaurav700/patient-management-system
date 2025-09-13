package com.pm.authservice.controller;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.SignUpRequestDto;
import com.pm.authservice.dto.SignUpResponseDto;
import com.pm.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signup user",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class))})
    })
    @PostMapping("/signup")
    public SignUpResponseDto signUp(@RequestBody SignUpRequestDto signUpRequestDto){
        return authService.signUp(signUpRequestDto);
    }


    @Operation(summary = "Login the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login user",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class))})
    })
    @PostMapping("/login")
    public LoginResponseDTO signUp(@RequestBody LoginRequestDTO loginRequestDto){
        return authService.login(loginRequestDto);
    }

}
