package com.datashare.datashare_backend.dto;

import java.time.LocalDateTime;

public record FileHistoryDTO(
        Long id,
        String originalName,
        Long size,
        LocalDateTime createdAt,
        LocalDateTime expirationDate,
        String downloadToken,
        boolean password
) {}
