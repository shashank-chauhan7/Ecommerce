package com.ecommerce.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class ImageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String cdnDomain;

    public ImageService(
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucketName,
            @Value("${app.cdn.domain}") String cdnDomain) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cdnDomain = cdnDomain;
    }

    public String uploadImage(MultipartFile file, UUID productId) throws IOException {
        String key = "products/" + productId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        log.info("Uploaded image for product {}: {}", productId, key);

        return "https://" + cdnDomain + "/" + key;
    }

    public void deleteImage(String imageKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(imageKey)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Deleted image: {}", imageKey);
    }
}
