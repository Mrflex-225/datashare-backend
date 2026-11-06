package com.datashare.datashare_backend.service;

import com.datashare.datashare_backend.dto.FileHistoryDTO;
import com.datashare.datashare_backend.dto.FileMetadataDTO;
import com.datashare.datashare_backend.dto.PublicFileMetadataDTO;
import com.datashare.datashare_backend.entities.File;
import com.datashare.datashare_backend.entities.User;
import com.datashare.datashare_backend.repository.FileRepository;
import com.datashare.datashare_backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;
    private final PasswordEncoder passwordEncoder;


    private static final long MAX_SIZE =
            1024L * 1024 * 1024;

    private static final Set<String> UNALLOWED_EXTENSIONS =
            Set.of(
                    "exe", "bat", "cmd", "com",
                    "msi", "msp", "scr", "pif",
                    "ps1", "psm1", "vbs", "vbe",
                    "js", "jse", "wsf", "wsh",
                    "hta", "jar", "reg",
                    "sh", "bash", "run"
            );

    public FileMetadataDTO uploadFile(
            MultipartFile multipartFile,
            String email,
            Integer expirationDays,
            String password

    ) throws IOException {

        // 1. Vérifier que le fichier existe
        if (multipartFile.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le fichier est vide"
            );
        }

        // 2. Vérifier sa taille
        if (multipartFile.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Taille maximale : 1 Go"
            );
        }

        // 3. Vérifier l'expiration
        int days = expirationDays == null
                ? 7 : expirationDays;

        if (days < 1 || days > 7) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expiration : entre 1 et 7 jours"
            );
        }

        // 4. Vérifier son nom et son extension
        String originalName =
                multipartFile.getOriginalFilename();

        if (originalName == null
                || originalName.isBlank()
                || originalName.contains("/")
                || originalName.contains("\\")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nom de fichier invalide"
            );
        }

        String normalizedName = originalName.toLowerCase(Locale.ROOT);

        int dotIndex = normalizedName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == normalizedName.length() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Extension de fichier manquante"
            );
        }


        String extension = normalizedName.substring(dotIndex + 1)
            .toLowerCase(Locale.ROOT);

        if (UNALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ce type de fichier est interdit"
            );
        }

        // 5. Identifier le propriétaire
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Utilisateur introuvable"
                        )
                );

        // 6. Vérifier et hasher le mot de passe facultatif
        String passwordHash = null;

        if (password != null && !password.isEmpty()) {

            if (password.length() < 6) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Le mot de passe doit contenir au moins 6 caractères"
                );
            }

            passwordHash = passwordEncoder.encode(password);
        }

        // 7. Créer des identifiants uniques
        String storagePath =
                "uploads/" + UUID.randomUUID();

        String token = UUID.randomUUID().toString();

        // 8. Calculer la date d'expiration
        LocalDateTime expiration =
                LocalDateTime.now(ZoneOffset.UTC)
                        .plusDays(days);

        String mimeType =
                multipartFile.getContentType() == null
                        ? "application/octet-stream"
                        : multipartFile.getContentType();

        // 9. Construire l'entité
        File file = File.builder()
                .originalName(originalName)
                .storagePath(storagePath)
                .mimeType(mimeType)
                .size(multipartFile.getSize())
                .downloadToken(token)
                .expirationDate(expiration)
                .password(passwordHash)
                .user(user)
                .build();

        // 10. Envoyer le contenu vers AWS S3
        s3StorageService.upload(
                multipartFile,
                storagePath
        );

        // 11. Enregistrer les métadonnées
        try {
            fileRepository.saveAndFlush(file);
        } catch (RuntimeException e) {
            try {
                s3StorageService.deleteFile(storagePath);
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }

        return toDTO(file);
    }

    private FileMetadataDTO toDTO(File file) {

        return new FileMetadataDTO(
                file.getId(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSize(),
                file.getDownloadToken(),
                file.getExpirationDate()
        );
    }

    public String getDownloadUrl(String token, String password) {

        File file = fileRepository
                .findByDownloadToken(token)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Fichier introuvable"
                        )
                );

        if (!file.getExpirationDate()
                .isAfter(LocalDateTime.now(ZoneOffset.UTC))) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Le lien a expiré"
            );
        }

        if (file.getPassword() != null) {

            if (password == null ||
                    !passwordEncoder.matches(
                            password,
                            file.getPassword()
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Mot de passe manquant ou incorrect"
                );
            }
        }

        return s3StorageService.generateDownloadUrl(
                file.getStoragePath()
        );
    }

   
    private File findValidFile(String token) {

        File file = fileRepository.findByDownloadToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lien de partage invalide"
                ));

        if (!file.getExpirationDate().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Ce lien de partage a expiré"
            );
        }

        return file;
    }

    public PublicFileMetadataDTO getPublicFileMetadata(String token) {
        File file = findValidFile(token);

        return new PublicFileMetadataDTO(
                file.getOriginalName(),
                file.getMimeType(),
                file.getSize(),
                file.getExpirationDate(),
                file.getPassword() != null
        );
    }

    public List<FileHistoryDTO> getMyFiles(String email) {

    return fileRepository.findByUser_Email(email)
        .stream()
        .map(file -> new FileHistoryDTO(
            file.getId(),
            file.getOriginalName(),
            file.getSize(),
            file.getCreatedAt(),
            file.getExpirationDate(),
            file.getDownloadToken(),
            file.getPassword() != null
        ))
        .toList();
}

@Transactional
public void deleteFile(Long id, String email) {

    File file = fileRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Fichier introuvable"
        ));

    if (!file.getUser().getEmail().equals(email)) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Fichier introuvable"
        );
    }

    if (!file.getExpirationDate().isAfter(LocalDateTime.now())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Ce fichier est déjà expiré"
        );
    }

    try {
        s3StorageService.deleteFile(file.getStoragePath());

        fileRepository.delete(file);

    } catch (S3Exception e) {

        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Impossible de supprimer le fichier sur AWS S3",
            e
        );

    } catch (SdkClientException e) {

        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Le service de stockage est temporairement indisponible",
            e
        );
    }
}

}
