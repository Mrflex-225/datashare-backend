package com.datashare.datashare_backend.service;

import com.datashare.datashare_backend.entities.File;
import com.datashare.datashare_backend.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiredFileCleanupService {

    private final FileRepository fileRepository;
    private final FileService fileService;
    private final Clock clock;

    public int cleanupExpiredFiles() {

        LocalDateTime now = LocalDateTime.now(clock);

        List<File> expiredFiles = fileRepository
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(now);

        int deletedCount = 0;

        for (File file : expiredFiles) {
            try {
                fileService.deleteExpiredFile(file);
                deletedCount++;

            } catch (Exception e) {
                log.error(
                        "Unable to clean up expired file: id={}",
                        file.getId(),
                        e
                );
            }
        }

        log.info(
                "Expired files cleanup: found={}, deleted={}, failed={}",
                expiredFiles.size(),
                deletedCount,
                expiredFiles.size() - deletedCount
        );

        return deletedCount;
    }
}
