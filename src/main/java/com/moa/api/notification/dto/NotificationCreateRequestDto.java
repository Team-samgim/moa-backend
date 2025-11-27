package com.moa.api.notification.dto;

import java.util.Map;

/******************************************************************************
 CLASS NAME    : NotificationCreateRequestDto
 DESCRIPTION   : 알림 생성 요청 DTO
 - 클라이언트 → 서버로 전달되는 알림 생성 요청 데이터
 - type: 알림 종류(업무 구분)
 - title: 알림 제목
 - content: 알림 본문 메시지
 - config: 알림 상세 정보(JSON)
 AUTHOR        : 방대혁
 ******************************************************************************/
public record NotificationCreateRequestDto(

        /** 알림 타입 (예: EXPORT_DONE, PRESET_SHARE 등) */
        String type,

        /** 알림 제목 */
        String title,

        /** 알림 내용 (본문 메시지) */
        String content,

        /**
         * 알림에 필요한 세부 정보(JSON)
         * 예: {"presetId": 123, "fileName": "report.pdf"}
         */
        Map<String, Object> config
) {
}
