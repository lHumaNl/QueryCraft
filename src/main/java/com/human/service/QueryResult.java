package com.human.service;

import java.util.Collections;
import java.util.List;

public class QueryResult {
    private final List<String> queries;
    private final String userType;
    private final String userName;
    private final String timePeriodName;
    private final String appliedFilter;
    private final long timeLeftBorder;
    private final long timeRightBorder;

    public QueryResult(List<String> queries, String userType, String userName, String timePeriodName, String appliedFilter, long timeLeftBorder, long timeRightBorder) {
        this.queries = Collections.unmodifiableList(queries);
        this.userType = userType;
        this.userName = userName;
        this.timePeriodName = timePeriodName;
        this.appliedFilter = appliedFilter;
        this.timeLeftBorder = timeLeftBorder;
        this.timeRightBorder = timeRightBorder;
    }

    public List<String> getQueries() {
        return queries;
    }

    public String getUserType() {
        return userType;
    }

    public String getUserName() {
        return userName;
    }

    public String getTimePeriodName() {
        return timePeriodName;
    }

    public String getAppliedFilter() {
        return appliedFilter;
    }

    public long getTimeLeftBorder() {
        return timeLeftBorder;
    }

    public long getTimeRightBorder() {
        return timeRightBorder;
    }

    @Override
    public String toString() {
        return "BqlResult{queryName='" + userType + "'}";
    }

    public String getFormattedTimeRange() {
        return String.format("%d-%d", timeLeftBorder, timeRightBorder);
    }

    public boolean hasFilter() {
        return appliedFilter != null && !appliedFilter.trim().isEmpty();
    }

    public String getQueryType() {
        if (userType == null) {
            return "unknown";
        }
        return userType.toLowerCase().contains("dashboard") ? "dashboard" : "query";
    }

    public String getFirstQuery() {
        return queries.isEmpty() ? "" : queries.get(0);
    }

    public String getJoinedQueries() {
        return String.join("; ", queries);
    }

    public long getDurationMs() {
        return timeRightBorder - timeLeftBorder;
    }

    public long getDurationSeconds() {
        return getDurationMs() / 1000;
    }
}
