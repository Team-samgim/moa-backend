// 작성자: 최이서
package com.moa.api.pivot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PivotFieldMeta {
    private String name;      // field_key
    private String dataType;  // data_type
    private String label;     // label_ko
}