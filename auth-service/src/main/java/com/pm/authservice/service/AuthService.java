package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.SignUpRequestDto;
import com.pm.authservice.dto.SignUpResponseDto;
import com.pm.authservice.mapper.UserMapper;
import com.pm.authservice.model.User;
import com.pm.authservice.repository.UserRepository;
import com.pm.authservice.util.JwtService;
import com.pm.authservice.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private UserMapper userMapper;

    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        log.info("Attempting to sign up user with email: {}", signUpRequestDto.getEmail());

        User users = userService.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (users != null) {
            log.error("Sign up failed: User with email {} already exists", signUpRequestDto.getEmail());
      throw new IllegalArgumentException(
          "User with this email id already exists: " + signUpRequestDto.getEmail());
        }

        User user = UserMapper.toEntity(signUpRequestDto);
        user.setPassword(PasswordUtil.hashPassword(signUpRequestDto.getPassword()));

        User savedUser = userRepository.save(user);

        log.info("User with email {} signed up successfully", signUpRequestDto.getEmail());
        return UserMapper.toDTO(savedUser);
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDto) {
        log.info("Attempting login for email: {}", loginRequestDto.getEmail());

    User user =
        userRepository
            .findByEmail(loginRequestDto.getEmail())
            .orElseThrow(
                () -> {
                  log.error(
                      "Login failed: No user found with email {}", loginRequestDto.getEmail());
                  return new NoSuchElementException(
                      "User with this email id: " + loginRequestDto.getEmail() + " not exists");
                });

        boolean passwordCheck = PasswordUtil.checkPassword(loginRequestDto.getPassword(), user.getPassword());

        if (!passwordCheck) {
            log.error("Login failed: Incorrect password for email {}", loginRequestDto.getEmail());
      throw new IllegalArgumentException("The password you entered is incorrect");
        }

        log.info("Password verified for email: {}", loginRequestDto.getEmail());

        log.info("Login successful for email: {}", loginRequestDto.getEmail());
    return LoginResponseDTO.builder()
        .accessToken(jwtService.generateAccessToken(user))
        .refreshToken(jwtService.generateRefreshToken(user))
        .build();
    }
}
