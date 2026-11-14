
package com.datashare.datashare_backend.service;

import com.datashare.datashare_backend.entities.File;
import com.datashare.datashare_backend.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredFileCleanupServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileService fileService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ExpiredFileCleanupService cleanupService;

    private void setFixedTime() {
        when(clock.instant())
                .thenReturn(Instant.parse("2026-11-14T12:00:00Z"));

        when(clock.getZone())
                .thenReturn(ZoneOffset.UTC);
    }

    @Test
    void shouldDeleteExpiredFiles() {
        setFixedTime();

        File expiredFile = File.builder()
                .id(1L)
                .build();

        LocalDateTime expectedDate =
                LocalDateTime.of(2026, 11, 14, 12, 0);

        when(fileRepository
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
                        expectedDate))
                .thenReturn(List.of(expiredFile));

        int deleted = cleanupService.cleanupExpiredFiles();

        assertEquals(1, deleted);
        verify(fileService).deleteExpiredFile(expiredFile);
    }

    @Test
    void shouldDoNothingWhenNoFileIsExpired() {
        setFixedTime();

        when(fileRepository
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        int deleted = cleanupService.cleanupExpiredFiles();

        assertEquals(0, deleted);
        verifyNoInteractions(fileService);
    }

    @Test
    void shouldContinueWhenOneDeletionFails() {
        setFixedTime();

        File firstFile = File.builder().id(1L).build();
        File secondFile = File.builder().id(2L).build();

        when(fileRepository
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
                        any(LocalDateTime.class)))
                .thenReturn(List.of(firstFile, secondFile));

        doThrow(new RuntimeException("S3 unavailable"))
                .when(fileService)
                .deleteExpiredFile(firstFile);

        int deleted = cleanupService.cleanupExpiredFiles();

        assertEquals(1, deleted);

        verify(fileService).deleteExpiredFile(firstFile);
        verify(fileService).deleteExpiredFile(secondFile);
    }

    @Test
    void shouldUseUtcTimeFromClock() {
        setFixedTime();

        when(fileRepository
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        cleanupService.cleanupExpiredFiles();

        verify(fileRepository)
                .findTop100ByExpirationDateLessThanEqualOrderByExpirationDateAsc(
                        LocalDateTime.of(2026, 11, 14, 12, 0));
    }
}
