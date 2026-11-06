package com.datashare.datashare_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank
    private String email;
    @NotBlank
    private String password;

}
