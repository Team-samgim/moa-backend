// 작성자: 최이서
package com.moa.api.chart.repository;

import com.moa.api.chart.entity.ChartThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartThumbnailRepository extends JpaRepository<ChartThumbnail, Long> {
}
