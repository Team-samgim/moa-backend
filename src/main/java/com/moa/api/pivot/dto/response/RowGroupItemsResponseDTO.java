// 작성자: 최이서
package com.moa.api.pivot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowGroupItemsResponseDTO {

    private String rowField;   // 예: "dst_mac"
    private String rowLabel;   // 예: "dst_mac (10)"
    private List<PivotQueryResponseDTO.RowGroupItem> items;

    private String nextCursor;  // 다음 페이지 cursor
    private Boolean hasMore;    // 더 있는지 여부
//    private Integer totalCount; // 전체 개수 (선택)
}
