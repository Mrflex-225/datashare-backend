package com.datashare.datashare_backend.controller;


import com.datashare.datashare_backend.dto.DownloadRequestDTO;
import com.datashare.datashare_backend.dto.DownloadResponseDTO;
import com.datashare.datashare_backend.dto.FileHistoryDTO;
import com.datashare.datashare_backend.dto.FileMetadataDTO;
import com.datashare.datashare_backend.dto.PublicFileMetadataDTO;
import com.datashare.datashare_backend.service.FileService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;


    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FileMetadataDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "password",
                    required = false
            ) String password,
            @RequestParam(
                    value = "expirationDays",
                    required = false
            ) Integer expirationDays,
            Authentication authentication
    ) throws IOException {

        FileMetadataDTO response =
                fileService.uploadFile(
                        file,
                        authentication.getName(),
                        expirationDays,
                        password
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/download/{token}")
    public ResponseEntity<DownloadResponseDTO> download(
            @PathVariable String token,
            @RequestBody(required = false) DownloadRequestDTO request
    ) {
        String password = request != null
                ? request.password()
                : null;

        String signedUrl = fileService.getDownloadUrl(token, password);

        return ResponseEntity.ok(
                new DownloadResponseDTO(signedUrl)
        );
    }
    @GetMapping("/download/{token}/metadata")
    public ResponseEntity<PublicFileMetadataDTO> getMetadata(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(
                fileService.getPublicFileMetadata(token)
        );
    }

    @GetMapping("/me")
public ResponseEntity<List<FileHistoryDTO>> getMyFiles(
        Authentication authentication
) {
    return ResponseEntity.ok(
        fileService.getMyFiles(authentication.getName())
    );
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteFile(
        @PathVariable Long id,
        Authentication authentication
) {
    fileService.deleteFile(id, authentication.getName());

    return ResponseEntity.noContent().build();
}

}