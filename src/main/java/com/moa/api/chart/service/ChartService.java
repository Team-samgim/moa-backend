package com.moa.api.chart.service;

import com.moa.api.chart.dto.PivotChartRequestDTO;
import com.moa.api.chart.dto.PivotChartResponseDTO;
import com.moa.api.chart.dto.PivotHeatmapTableRequestDTO;
import com.moa.api.chart.dto.PivotHeatmapTableResponseDTO;
import com.moa.api.chart.repository.ChartRepository;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.SqlSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartRepository chartRepository;
    private final SqlSupport sqlSupport;

    public PivotChartResponseDTO getChart(PivotChartRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        PivotLayer layer = PivotLayer.from(req.getLayer());

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                req.getFilters(),
                sqlSupport
        );

        return chartRepository.getChart(ctx, req);
    }

    public PivotHeatmapTableResponseDTO getHeatmapTable(PivotHeatmapTableRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());
        PivotLayer layer = PivotLayer.from(req.getLayer());

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                req.getTime() != null ? req.getTime().getField() : null,
                tw,
                req.getFilters(),
                sqlSupport
        );

        return chartRepository.getHeatmapTable(ctx, req);
    }

    public TimeWindow resolveTimeWindow(PivotQueryRequestDTO.TimeDef time) {
        if (time == null || time.getFromEpoch() == null || time.getToEpoch() == null) {
            long nowSec = Instant.now().getEpochSecond();
            return new TimeWindow(nowSec - 3600, nowSec);
        }
        return new TimeWindow(time.getFromEpoch(), time.getToEpoch());
    }
}
