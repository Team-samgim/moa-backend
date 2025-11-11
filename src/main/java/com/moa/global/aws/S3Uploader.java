package com.moa.global.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;
    private final S3Presigner presigner;

    public void upload(String bucket, String key, Path file, String contentType) {
        String original = file.getFileName().toString();
        String ascii = safeAsciiName(original); // ✅ 아래 helper 사용
        String encoded = URLEncoder.encode(original, StandardCharsets.UTF_8);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentDisposition(
                        "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded
                )
                .build();

        s3.putObject(req, RequestBody.fromFile(file));
    }

    /**
     * GET 프리사인드 URL (브라우저에서 직접 열기)
     */
    public String presign(String bucket, String key, Duration ttl) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getReq)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
        URL url = presigned.url();
        return url.toString(); // https://... 형식
    }

    /** ASCII 안전 파일명 생성 (헤더용 fall-back) */
    private static String safeAsciiName(String name) {
        // Unicode 분해 후 결합문자 제거
        String n = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        // 위험 문자 제거(윈도우/맥/리눅스 공통 금지 문자)
        n = n.replaceAll("[\\\\/:*?\"<>|]", "_");
        // 비표시/비ASCII 문자 대체
        n = n.replaceAll("[^\\x20-\\x7E]", "_").trim();
        // 빈 결과 방지
        return n.isEmpty() ? "download.csv" : n;
    }

    /** 단일 객체 삭제 */
    public void delete(String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}