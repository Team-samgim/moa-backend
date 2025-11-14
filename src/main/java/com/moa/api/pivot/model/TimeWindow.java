package com.moa.api.pivot.model;

import lombok.Value;

@Value
public class TimeWindow {
    double fromEpoch;  // UNIX epoch (seconds) 시작
    double toEpoch;    // UNIX epoch (seconds) 끝
}
