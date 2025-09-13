package com.pm.authservice.mapper;

import com.pm.authservice.dto.SignUpRequestDto;
import com.pm.authservice.dto.SignUpResponseDto;
import com.pm.authservice.model.User;

public class UserMapper {
    public static SignUpResponseDto toDTO(User user){
        return SignUpResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static User toEntity(SignUpRequestDto signUpRequestDto){
        return User.builder()
                .name(signUpRequestDto.getName())
                .password(signUpRequestDto.getPassword())
                .email(signUpRequestDto.getEmail())
                .build();
    }
}
