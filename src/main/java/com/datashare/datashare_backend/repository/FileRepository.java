package com.datashare.datashare_backend.repository;

import com.datashare.datashare_backend.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository
        extends JpaRepository<File, Long> {

    Optional<File> findByDownloadToken(
            String token
    );

    List<File> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<File> findByExpirationDateBefore(
            LocalDateTime date
    );

    List<File> findByUser_Email(String email);

    List<File> findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
            LocalDateTime date
    );
}
