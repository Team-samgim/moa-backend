package com.moa.api.notification.dto;

import java.util.List;

/******************************************************************************
 CLASS NAME    : NotificationListResponseDto
 DESCRIPTION   : 알림 목록 응답 DTO (무한 스크롤 전용)
 - items: 조회된 알림 목록
 - nextCursor: 다음 페이지 요청 시 사용할 마지막 알림 ID
 - hasNext: 다음 페이지 존재 여부
 - unreadCount: 읽지 않은 알림 총 개수
 AUTHOR        : 방대혁
 ******************************************************************************/
public record NotificationListResponseDto(

        /** 현재 페이지의 알림 목록 */
        List<NotificationResponseDto> items,

        /**
         * 다음 페이지 조회 시 사용될 커서 (마지막 알림 ID)
         * null이면 더 이상 데이터 없음
         */
        Long nextCursor,

        /** 다음 페이지가 존재하는지 여부 */
        boolean hasNext,

        /** 전체 읽지 않은 알림 개수 */
        long unreadCount
) {
}
