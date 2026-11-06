package com.datashare.datashare_backend.controller;

import com.datashare.datashare_backend.dto.FileHistoryDTO;
import com.datashare.datashare_backend.dto.FileMetadataDTO;
import com.datashare.datashare_backend.dto.PublicFileMetadataDTO;
import com.datashare.datashare_backend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class FileControllerTest {

    private static final String EMAIL = "designanguy@gmail.com";
    private static final String TOKEN = "download-token";

    @Mock
    private FileService fileService;
    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;

    private final Authentication authentication =
            new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Vérifie que l'upload renvoie 201
     * avec les métadonnées du fichier.
     */
    @Test
    public void test_upload() throws Exception {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());
        FileMetadataDTO response = new FileMetadataDTO(
                1L, "report.pdf", "application/pdf", 7L, TOKEN, LocalDateTime.now().plusDays(3));
        when(fileService.uploadFile(any(MultipartFile.class), eq(EMAIL), eq(3), eq("secret123")))
                .thenReturn(response);

        // WHEN
        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .param("password", "secret123")
                        .param("expirationDays", "3")
                        .principal(authentication))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.downloadToken").value(TOKEN));
    }

    @Test
    public void test_upload_without_optional_params() throws Exception {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());
        when(fileService.uploadFile(any(MultipartFile.class), eq(EMAIL), isNull(), isNull()))
                .thenReturn(new FileMetadataDTO(1L, "report.pdf", "application/pdf", 7L, TOKEN, null));

        // WHEN
        mockMvc.perform(multipart("/api/files").file(file).principal(authentication))
                .andExpect(status().isCreated());
    }

    @Test
    public void test_upload_invalid_file_returns_bad_request() throws Exception {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "content".getBytes());
        when(fileService.uploadFile(any(MultipartFile.class), eq(EMAIL), isNull(), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce type de fichier est interdit"));

        // WHEN
        mockMvc.perform(multipart("/api/files").file(file).principal(authentication))
                .andExpect(status().isBadRequest());
    }

    /**
     * Vérifie que le téléchargement transmet
     * le mot de passe et renvoie l'URL signée.
     */
    @Test
    public void test_download_with_password() throws Exception {
        // GIVEN
        when(fileService.getDownloadUrl(TOKEN, "secret123")).thenReturn("https://s3/signed");

        // WHEN
        mockMvc.perform(post("/api/files/download/{token}", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("https://s3/signed"));
    }

    @Test
    public void test_download_without_body() throws Exception {
        // GIVEN
        when(fileService.getDownloadUrl(TOKEN, null)).thenReturn("https://s3/signed");

        // WHEN
        mockMvc.perform(post("/api/files/download/{token}", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value("https://s3/signed"));
    }

    @Test
    public void test_download_wrong_password_returns_forbidden() throws Exception {
        // GIVEN
        when(fileService.getDownloadUrl(TOKEN, "wrong"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Mot de passe manquant ou incorrect"));

        // WHEN
        mockMvc.perform(post("/api/files/download/{token}", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void test_get_metadata() throws Exception {
        // GIVEN
        when(fileService.getPublicFileMetadata(TOKEN)).thenReturn(new PublicFileMetadataDTO(
                "report.pdf", "application/pdf", 7L, LocalDateTime.now().plusDays(2), true));

        // WHEN
        mockMvc.perform(get("/api/files/download/{token}/metadata", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.password").value(true));
    }

    @Test
    public void test_get_metadata_expired_returns_gone() throws Exception {
        // GIVEN
        when(fileService.getPublicFileMetadata(TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.GONE, "Ce lien de partage a expiré"));

        // WHEN
        mockMvc.perform(get("/api/files/download/{token}/metadata", TOKEN))
                .andExpect(status().isGone());
    }

    @Test
    public void test_get_my_files() throws Exception {
        // GIVEN
        when(fileService.getMyFiles(EMAIL)).thenReturn(List.of(
                new FileHistoryDTO(1L, "a.pdf", 10L, LocalDateTime.now(), LocalDateTime.now().plusDays(1), "t1", false),
                new FileHistoryDTO(2L, "b.pdf", 20L, LocalDateTime.now(), LocalDateTime.now().plusDays(2), "t2", true)
        ));

        // WHEN
        mockMvc.perform(get("/api/files/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalName").value("a.pdf"))
                .andExpect(jsonPath("$[1].password").value(true));
    }

    @Test
    public void test_delete_file() throws Exception {
        // WHEN
        mockMvc.perform(delete("/api/files/{id}", 1L).principal(authentication))
                .andExpect(status().isNoContent());

        // THEN
        verify(fileService).deleteFile(1L, EMAIL);
    }
}
