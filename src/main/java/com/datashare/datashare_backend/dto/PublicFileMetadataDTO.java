package com.datashare.datashare_backend.dto;

import java.time.LocalDateTime;

public record PublicFileMetadataDTO(
        String originalName,
        String contentType,
        Long size,
        LocalDateTime expirationDate,
        boolean password
) {}
