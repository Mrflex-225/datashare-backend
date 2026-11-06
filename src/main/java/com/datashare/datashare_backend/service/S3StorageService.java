package com.datashare.datashare_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public void upload(
            MultipartFile file,
            String storagePath
    ) throws IOException {

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storagePath)
                        .contentType(
                                file.getContentType() == null
                                        ? "application/octet-stream"
                                        : file.getContentType()
                        )
                        .build();

        try (var input = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            input,
                            file.getSize()
                    )
            );
        }
    }

    public String generateDownloadUrl(
            String storagePath
    ) {

        GetObjectRequest getRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storagePath)
                        .responseContentDisposition("attachment")
                        .build();

        GetObjectPresignRequest request =
                GetObjectPresignRequest.builder()
                        .signatureDuration(
                                Duration.ofMinutes(5)
                        )
                        .getObjectRequest(getRequest)
                        .build();

        return s3Presigner
                .presignGetObject(request)
                .url()
                .toString();
    }

   
    
public void deleteFile(String storagePath) {

    DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(storagePath)
            .build();

    s3Client.deleteObject(request);
}

}