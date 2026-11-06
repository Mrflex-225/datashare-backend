package com.datashare.datashare_backend.service;

import com.datashare.datashare_backend.dto.FileHistoryDTO;
import com.datashare.datashare_backend.dto.FileMetadataDTO;
import com.datashare.datashare_backend.dto.PublicFileMetadataDTO;
import com.datashare.datashare_backend.entities.File;
import com.datashare.datashare_backend.entities.User;
import com.datashare.datashare_backend.repository.FileRepository;
import com.datashare.datashare_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    private static final String EMAIL = "designanguy@gmail.com";
    private static final String TOKEN = "download-token";
    private static final String STORAGE_PATH = "uploads/abc";
    private static final String SIGNED_URL = "https://s3.example.com/signed";

    @Mock
    private FileRepository fileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private FileService fileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail(EMAIL);
        user.setPassword("password");
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "content".getBytes()
        );
    }

    private File storedFile(LocalDateTime expiration, String passwordHash) {
        return File.builder()
                .id(1L)
                .originalName("report.pdf")
                .storagePath(STORAGE_PATH)
                .mimeType("application/pdf")
                .size(7L)
                .downloadToken(TOKEN)
                .expirationDate(expiration)
                .password(passwordHash)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .user(user)
                .build();
    }

    private static LocalDateTime inDays(int days) {
        return LocalDateTime.now(ZoneOffset.UTC).plusDays(days);
    }

    private static void assertStatus(HttpStatus expected, ResponseStatusException e) {
        assertEquals(expected, e.getStatusCode());
    }

    // ---------- uploadFile ----------

    /**
     * Vérifie qu'un fichier valide est envoyé sur S3
     * puis enregistré en base avec 7 jours par défaut.
     */
    @Test
    public void test_upload_file_with_default_values() throws Exception {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // WHEN
        FileMetadataDTO result = fileService.uploadFile(validFile(), EMAIL, null, null);

        // THEN
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).saveAndFlush(fileCaptor.capture());
        File saved = fileCaptor.getValue();

        assertEquals("report.pdf", saved.getOriginalName());
        assertEquals("application/pdf", saved.getMimeType());
        assertEquals(7L, saved.getSize());
        assertNull(saved.getPassword());
        assertSame(user, saved.getUser());
        assertTrue(saved.getStoragePath().startsWith("uploads/"));
        assertThat(saved.getExpirationDate())
                .isBetween(inDays(7).minusMinutes(1), inDays(7).plusMinutes(1));

        verify(s3StorageService).upload(any(MultipartFile.class), eq(saved.getStoragePath()));
        verify(passwordEncoder, never()).encode(anyString());

        assertEquals("report.pdf", result.originalName());
        assertEquals(saved.getDownloadToken(), result.downloadToken());
        assertEquals(saved.getExpirationDate(), result.expirationDate());
    }

    /**
     * Vérifie que le mot de passe est hashé
     * et que l'expiration choisie est respectée.
     */
    @Test
    public void test_upload_file_with_password_and_expiration() throws Exception {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        // WHEN
        fileService.uploadFile(validFile(), EMAIL, 3, "secret123");

        // THEN
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileRepository).saveAndFlush(fileCaptor.capture());
        assertEquals("hashed", fileCaptor.getValue().getPassword());
        assertThat(fileCaptor.getValue().getExpirationDate())
                .isBetween(inDays(3).minusMinutes(1), inDays(3).plusMinutes(1));
    }


    @Test
    public void test_upload_empty_file_throws_bad_request() {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(file, EMAIL, null, null));
        assertStatus(HttpStatus.BAD_REQUEST, e);
    }

    @Test
    public void test_upload_too_large_file_throws_bad_request() {
        // GIVEN
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L * 1024 * 1024 + 1);

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(file, EMAIL, null, null));
        assertStatus(HttpStatus.BAD_REQUEST, e);
        assertEquals("Taille maximale : 1 Go", e.getReason());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, -1})
    public void test_upload_invalid_expiration_throws_bad_request(int days) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(validFile(), EMAIL, days, null));
        assertStatus(HttpStatus.BAD_REQUEST, e);
        assertEquals("Expiration : entre 1 et 7 jours", e.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "dir/file.txt", "dir\\file.txt"})
    public void test_upload_invalid_name_throws_bad_request(String name) {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile("file", name, "text/plain", "x".getBytes());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(file, EMAIL, null, null));
        assertEquals("Nom de fichier invalide", e.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"README", "archive."})
    public void test_upload_missing_extension_throws_bad_request(String name) {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile("file", name, "text/plain", "x".getBytes());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(file, EMAIL, null, null));
        assertEquals("Extension de fichier manquante", e.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"virus.exe", "script.SH", "macro.js"})
    public void test_upload_forbidden_extension_throws_bad_request(String name) {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile("file", name, "text/plain", "x".getBytes());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(file, EMAIL, null, null));
        assertEquals("Ce type de fichier est interdit", e.getReason());
    }

    @Test
    public void test_upload_unknown_user_throws_unauthorized() {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(validFile(), EMAIL, null, null));
        assertStatus(HttpStatus.UNAUTHORIZED, e);
    }

    @Test
    public void test_upload_short_password_throws_bad_request() {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.uploadFile(validFile(), EMAIL, null, "123"));
        assertStatus(HttpStatus.BAD_REQUEST, e);
        verify(s3StorageService, never()).deleteFile(anyString());
    }

    /**
     * Vérifie que l'erreur de nettoyage S3
     * est attachée comme exception supprimée.
     */
    @Test
    public void test_upload_db_and_cleanup_failure_adds_suppressed() throws Exception {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(fileRepository.saveAndFlush(any(File.class))).thenThrow(new RuntimeException("db down"));
        RuntimeException cleanupError = new RuntimeException("s3 down");
        doThrow(cleanupError).when(s3StorageService).deleteFile(anyString());

        // WHEN
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fileService.uploadFile(validFile(), EMAIL, null, null));

        // THEN
        assertEquals("db down", thrown.getMessage());
        assertSame(cleanupError, thrown.getSuppressed()[0]);
    }

    // ---------- getDownloadUrl ----------

    @Test
    public void test_get_download_url_without_password() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(2), null)));
        when(s3StorageService.generateDownloadUrl(STORAGE_PATH)).thenReturn(SIGNED_URL);

        // THEN
        assertEquals(SIGNED_URL, fileService.getDownloadUrl(TOKEN, null));
    }

    @Test
    public void test_get_download_url_with_correct_password() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(2), "hashed")));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(s3StorageService.generateDownloadUrl(STORAGE_PATH)).thenReturn(SIGNED_URL);

        // THEN
        assertEquals(SIGNED_URL, fileService.getDownloadUrl(TOKEN, "secret123"));
    }

    @Test
    public void test_get_download_url_unknown_token_throws_not_found() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getDownloadUrl(TOKEN, null));
        assertStatus(HttpStatus.NOT_FOUND, e);
    }

    @Test
    public void test_get_download_url_expired_throws_gone() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(-1), null)));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getDownloadUrl(TOKEN, null));
        assertStatus(HttpStatus.GONE, e);
    }

    @Test
    public void test_get_download_url_missing_password_throws_forbidden() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(2), "hashed")));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getDownloadUrl(TOKEN, null));
        assertStatus(HttpStatus.FORBIDDEN, e);
    }

    @Test
    public void test_get_download_url_wrong_password_throws_forbidden() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(2), "hashed")));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getDownloadUrl(TOKEN, "wrong"));
        assertStatus(HttpStatus.FORBIDDEN, e);
        verify(s3StorageService, never()).generateDownloadUrl(anyString());
    }

    // ---------- getPublicFileMetadata ----------

    @Test
    public void test_get_public_metadata() {
        // GIVEN
        File file = storedFile(inDays(2), "hashed");
        when(fileRepository.findByDownloadToken(TOKEN)).thenReturn(Optional.of(file));

        // WHEN
        PublicFileMetadataDTO result = fileService.getPublicFileMetadata(TOKEN);

        // THEN
        assertEquals("report.pdf", result.originalName());
        assertEquals("application/pdf", result.contentType());
        assertEquals(7L, result.size());
        assertEquals(file.getExpirationDate(), result.expirationDate());
        assertTrue(result.password());
    }

    @Test
    public void test_get_public_metadata_unknown_token_throws_not_found() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getPublicFileMetadata(TOKEN));
        assertStatus(HttpStatus.NOT_FOUND, e);
    }

    @Test
    public void test_get_public_metadata_expired_throws_gone() {
        // GIVEN
        when(fileRepository.findByDownloadToken(TOKEN))
                .thenReturn(Optional.of(storedFile(inDays(-1), null)));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.getPublicFileMetadata(TOKEN));
        assertStatus(HttpStatus.GONE, e);
    }

    // ---------- getMyFiles ----------

    @Test
    public void test_get_my_files() {
        // GIVEN
        File withPassword = storedFile(inDays(2), "hashed");
        File withoutPassword = storedFile(inDays(3), null);
        withoutPassword.setId(2L);
        when(fileRepository.findByUser_Email(EMAIL)).thenReturn(List.of(withPassword, withoutPassword));

        // WHEN
        List<FileHistoryDTO> result = fileService.getMyFiles(EMAIL);

        // THEN
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(TOKEN, result.get(0).downloadToken());
        assertTrue(result.get(0).password());
        assertEquals(2L, result.get(1).id());
        assertFalse(result.get(1).password());
    }

    // ---------- deleteFile ----------

    @Test
    public void test_delete_file() {
        // GIVEN
        File file = storedFile(inDays(2), null);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        // WHEN
        fileService.deleteFile(1L, EMAIL);

        // THEN
        verify(s3StorageService).deleteFile(STORAGE_PATH);
        verify(fileRepository).delete(file);
    }

    @Test
    public void test_delete_unknown_file_throws_not_found() {
        // GIVEN
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.deleteFile(1L, EMAIL));
        assertStatus(HttpStatus.NOT_FOUND, e);
    }

    @Test
    public void test_delete_file_of_other_user_throws_not_found() {
        // GIVEN
        when(fileRepository.findById(1L)).thenReturn(Optional.of(storedFile(inDays(2), null)));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.deleteFile(1L, "other@gmail.com"));
        assertStatus(HttpStatus.NOT_FOUND, e);
        verify(s3StorageService, never()).deleteFile(anyString());
    }

    @Test
    public void test_delete_expired_file_throws_conflict() {
        // GIVEN
        when(fileRepository.findById(1L)).thenReturn(Optional.of(storedFile(inDays(-1), null)));

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.deleteFile(1L, EMAIL));
        assertStatus(HttpStatus.CONFLICT, e);
    }

    @Test
    public void test_delete_file_s3_error_throws_bad_gateway() {
        // GIVEN
        File file = storedFile(inDays(2), null);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));
        doThrow(S3Exception.builder().message("denied").build())
                .when(s3StorageService).deleteFile(STORAGE_PATH);

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.deleteFile(1L, EMAIL));
        assertStatus(HttpStatus.BAD_GATEWAY, e);
        verify(fileRepository, never()).delete(any(File.class));
    }

    @Test
    public void test_delete_file_s3_unavailable_throws_service_unavailable() {
        // GIVEN
        File file = storedFile(inDays(2), null);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));
        doThrow(SdkClientException.create("timeout"))
                .when(s3StorageService).deleteFile(STORAGE_PATH);

        // THEN
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> fileService.deleteFile(1L, EMAIL));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, e);
        verify(fileRepository, never()).delete(any(File.class));
    }
}
