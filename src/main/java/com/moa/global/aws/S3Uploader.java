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

/**
 * S3 업로드/다운로드/삭제 유틸리티
 *
 * 기능:
 * - 파일 업로드 (Content-Disposition 처리 포함)
 * - 프리사인드 URL 생성 (GET)
 * - ASCII 안전 파일명 생성 (fallback)
 * - S3 객체 삭제
 *
 * AUTHOR        : 방대혁
 */
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;
    private final S3Presigner presigner;

    /**
     * 파일 업로드
     *
     * @param bucket       S3 버킷명
     * @param key          객체 키
     * @param file         업로드할 파일 Path
     * @param contentType  MIME 타입
     */
    public void upload(String bucket, String key, Path file, String contentType) {
        String original = file.getFileName().toString();
        String ascii = safeAsciiName(original); // ASCII fallback
        String encoded = URLEncoder.encode(original, StandardCharsets.UTF_8);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                // 다운로드 시 브라우저에서 파일명 깨짐 방지
                .contentDisposition(
                        "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded
                )
                .build();

        s3.putObject(req, RequestBody.fromFile(file));
    }

    /**
     * GET 프리사인드 URL 반환
     *
     * @param bucket  S3 버킷명
     * @param key     객체 키
     * @param ttl     URL 유효 시간
     * @return        https://.. 형식의 프리사인드 URL
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

        return url.toString();
    }

    /**
     * ASCII 안전 파일명 생성
     * - Unicode → ASCII 변환
     * - OS별 금지 문자 제거
     * - 비표시/비ASCII 문자 대체
     */
    private static String safeAsciiName(String name) {
        // Unicode 분해 후 결합문자 제거
        String n = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");

        // 위험 문자 제거(윈도우/맥/리눅스 공통 금지 문자)
        n = n.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 비표시/비ASCII 문자 대체
        n = n.replaceAll("[^\\x20-\\x7E]", "_").trim();

        // 빈 문자열 방지
        return n.isEmpty() ? "download.csv" : n;
    }

    /**
     * S3 객체 삭제
     */
    public void delete(String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
