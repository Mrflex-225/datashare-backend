package com.datashare.datashare_backend.jobs;

import com.datashare.datashare_backend.service.ExpiredFileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredFileCleanupJob {

    private final ExpiredFileCleanupService cleanupService;

    @Scheduled(
            fixedDelayString = "${app.cleanup.delay-ms:3600000}",
            initialDelayString = "${app.cleanup.initial-delay-ms:60000}"
    )
    public void cleanup() {

        log.info("Starting expired files cleanup");

        int deleted = cleanupService.cleanupExpiredFiles();

        log.info("Expired files deleted: {}", deleted);
    }
}
