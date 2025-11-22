package com.moa.api.chart.service;

import com.moa.api.chart.dto.request.DrilldownTimeSeriesRequestDTO;
import com.moa.api.chart.dto.request.PivotChartRequestDTO;
import com.moa.api.chart.dto.response.DrilldownTimeSeriesResponseDTO;
import com.moa.api.chart.dto.response.PivotChartByColumnResponseDTO;
import com.moa.api.chart.dto.response.PivotChartResponseDTO;
import com.moa.api.chart.dto.request.PivotHeatmapTableRequestDTO;
import com.moa.api.chart.dto.response.PivotHeatmapTableResponseDTO;
import com.moa.api.chart.repository.ChartRepository;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.model.PivotLayer;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.SqlSupport;
import com.moa.api.pivot.service.PivotService;
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

//    public PivotChartByColumnResponseDTO getChartByColumn(PivotChartRequestDTO req) {
//        TimeWindow tw = resolveTimeWindow(req.getTime());
//        PivotLayer layer = PivotLayer.from(req.getLayer());
//
//        PivotQueryContext ctx = new PivotQueryContext(
//                layer,
//                req.getTime() != null ? req.getTime().getField() : null,
//                tw,
//                req.getFilters(),
//                sqlSupport
//        );
//
//        return chartRepository.getChartByColumn(ctx, req);
//    }

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

    public DrilldownTimeSeriesResponseDTO getDrilldownTimeSeries(DrilldownTimeSeriesRequestDTO req) {
        PivotLayer layer = PivotLayer.from(req.getLayer());
        TimeWindow tw = resolveTimeWindow(req.getTime());
        String timeField = null;
        if (req.getTime() != null && req.getTime().getField() != null && !req.getTime().getField().isBlank()) {
            timeField = req.getTime().getField();
        } else {
            timeField = layer.getDefaultTimeField(); // ex) "ts_server_nsec"
        }

        PivotQueryContext ctx = new PivotQueryContext(
                layer,
                timeField,
                tw,
                req.getFilters(),
                sqlSupport
        );
        return chartRepository.getDrilldownTimeSeries(ctx, req);
    }
}
