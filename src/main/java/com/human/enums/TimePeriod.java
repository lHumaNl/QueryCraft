package com.human.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TimePeriod {
    @JsonProperty("Last1h")
    LAST_1H,
    @JsonProperty("Last8h")
    LAST_8H,
    @JsonProperty("Last24h")
    LAST_24H,
    @JsonProperty("Last3d")
    LAST_3D,
    @JsonProperty("Last7d")
    LAST_7D,
    @JsonProperty("Yesterday")
    YESTERDAY,
    @JsonProperty("BeforeYesterday")
    BEFORE_YESTERDAY,
    @JsonProperty("SameDayPrevWeek")
    SAME_DAY_PREV_WEEK,
    @JsonProperty("LastWeek")
    LAST_WEEK,
    @JsonProperty("Custom")
    CUSTOM
}
