package com.moa.api.pivot.model;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class TimeWindow {
    LocalDateTime from;
    LocalDateTime to;
}
