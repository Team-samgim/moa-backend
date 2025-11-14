package com.moa.api.pivot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotFieldsResponseDTO {

    private String layer;          // "HTTP_PAGE"
    private List<FieldMeta> fields;

    @Data
    @Builder
    public static class FieldMeta {
        private String name;           // "vlan_id"
        private String label;          // "vlan_id" or "VLAN ID"
        private String dataType;       // "string" | "number" | "ip" | ...
//        private boolean aggregatable;  // 집계 가능 여부
//        private List<String> allowedZones; // ["row","column","value"]: 사용 가능한 영역
//        private String defaultAgg;     // "sum" | "avg" | null
    }
}
