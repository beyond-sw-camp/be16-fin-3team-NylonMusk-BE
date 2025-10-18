package com.beyond.MKX.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Manager {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 메뉴 이미지를 업로드하고 URL을 반환합니다.
     * key 형식: menus/{UUID}-{파일명}
     */
    public String upload(MultipartFile file, String prefix) {
        String key = String.format("%s/%s-%s", prefix, UUID.randomUUID(), file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();

        } catch (IOException e) {
            throw new IllegalArgumentException("메뉴 이미지 업로드 실패", e);
        }
    }

    /**
     * S3에서 해당 URL에 해당하는 객체를 삭제합니다.
     */
    public void delete(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("S3 파일 삭제 실패", e);
        }
    }

    /**
     * 같은 버킷 내에서 객체를 복사하고 대상 URL을 반환합니다.
     * destPrefix 예: disclosures/approved/{type}/{yyyy}/{MM}/{stockId}
     * 원본 파일명은 유지됩니다.
     */
    public String copy(String sourceFileUrl, String destPrefix) {
        String sourceKey = extractKeyFromUrl(sourceFileUrl);
        String filename = sourceKey.substring(sourceKey.lastIndexOf('/') + 1);
        String destKey = String.format("%s/%s", destPrefix, filename);
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(destKey)
                .build();
        s3Client.copyObject(copyReq);
        return s3Client.utilities().getUrl(b -> b.bucket(bucket).key(destKey)).toExternalForm();
    }

    /**
     * S3 URL에서 키(key)를 추출하는 유틸리티 메서드
     */
    private String extractKeyFromUrl(String fileUrl) {
        int index = fileUrl.indexOf(".amazonaws.com/");
        if (index == -1) {
            throw new IllegalArgumentException("잘못된 S3 URL 형식입니다.");
        }

        String encodedKey = fileUrl.substring(index + ".amazonaws.com/".length());

        // URL 디코딩 적용 (ex: %ED%95%9C%EA%B8%80.png → 한글.png)
        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }
}
