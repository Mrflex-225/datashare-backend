package com.datashare.datashare_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3StorageServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String STORAGE_PATH = "uploads/abc";

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;
    @InjectMocks
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3StorageService, "bucketName", BUCKET);
    }

    /**
     * Vérifie que le fichier est envoyé
     * dans le bon bucket avec le bon type MIME.
     */
    @Test
    public void test_upload() throws Exception {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        // WHEN
        s3StorageService.upload(file, STORAGE_PATH);

        // THEN
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(STORAGE_PATH, captor.getValue().key());
        assertEquals("application/pdf", captor.getValue().contentType());
    }

    @Test
    public void test_upload_without_content_type() throws Exception {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.bin", null, "content".getBytes());

        // WHEN
        s3StorageService.upload(file, STORAGE_PATH);

        // THEN
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("application/octet-stream", captor.getValue().contentType());
    }

    /**
     * Vérifie qu'une URL signée valable 5 minutes
     * est générée pour le bon objet.
     */
    @Test
    public void test_generate_download_url() throws Exception {
        // GIVEN
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.example.com/signed").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        // WHEN
        String url = s3StorageService.generateDownloadUrl(STORAGE_PATH);

        // THEN
        assertEquals("https://s3.example.com/signed", url);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertEquals(Duration.ofMinutes(5), captor.getValue().signatureDuration());
        assertEquals(BUCKET, captor.getValue().getObjectRequest().bucket());
        assertEquals(STORAGE_PATH, captor.getValue().getObjectRequest().key());
        assertEquals("attachment", captor.getValue().getObjectRequest().responseContentDisposition());
    }

    @Test
    public void test_delete_file() {
        // WHEN
        s3StorageService.deleteFile(STORAGE_PATH);

        // THEN
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(STORAGE_PATH, captor.getValue().key());
    }
}
