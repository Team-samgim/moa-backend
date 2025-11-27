package com.moa.api.notification.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/******************************************************************************
 CLASS NAME    : NotificationResponseDto
 DESCRIPTION   : 단일 알림 응답 DTO
 - 알림 ID, 타입, 제목, 내용, 구성 데이터(JSON), 읽음 여부,
 생성 시각 등을 담아 API 응답으로 전달한다.
 AUTHOR        : 방대혁
 ******************************************************************************/
public record NotificationResponseDto(

        /** 알림 ID (PK) */
        Long id,

        /** 알림 종류 (예: EXPORT_DONE, PRESET_FAVORITE 등) */
        String type,

        /** 알림 제목 */
        String title,

        /** 알림 상세 내용 */
        String content,

        /**
         * 추가 설정/데이터(JSON)
         * - 특정 화면으로 이동하기 위한 ID
         * - 알림 타입에 따라 필요한 context
         * 예) { "presetId": 10, "fileName": "report.docx" }
         */
        Map<String, Object> config,

        /** 읽음 여부 */
        boolean isRead,

        /** 알림 생성 시각 */
        OffsetDateTime createdAt
) {
}
