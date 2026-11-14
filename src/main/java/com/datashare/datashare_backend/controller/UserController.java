package com.datashare.datashare_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datashare.datashare_backend.dto.LoginRequestDTO;
import com.datashare.datashare_backend.dto.RegisterDTO;
import com.datashare.datashare_backend.mapper.UserDtoMapper;
import com.datashare.datashare_backend.service.UserService;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
        return ResponseEntity.ok(jwtToken);
    }


}
