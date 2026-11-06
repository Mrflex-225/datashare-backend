package com.datashare.datashare_backend.dto;


import java.time.LocalDateTime;

public record FileMetadataDTO(
        Long id,
        String originalName,
        String mimeType,
        Long size,
        String downloadToken,
        LocalDateTime expirationDate
) {}
