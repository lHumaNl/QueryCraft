package com.human.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.human.enums.TimePeriod;
import com.human.enums.UserType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StaticUserConfig extends BaseUserConfig {
    
    @JsonProperty("queries")
    private final List<String> queries;
    
    @JsonProperty("queries_file")
    private final String queriesFile;
    
    @JsonProperty("time_left_border")
    private final int timeLeftBorder;
    
    @JsonProperty("time_right_border")
    private final int timeRightBorder;

    @JsonProperty("times")
    private final List<TimePeriod> times;

    @JsonProperty("max_time_left")
    private final Integer maxTimeLeft;

    @JsonProperty("max_time_right")
    private final Integer maxTimeRight;

    @JsonProperty("is_all_select")
    private final Boolean isAllSelect;

    @JsonProperty("min_selected_queries")
    private final Integer minSelectedQueries;

    @JsonProperty("max_count_selected_in_percent")
    private final Integer maxCountSelectedInPercent;

    @JsonProperty("is_random_selection")
    private final Boolean isRandomSelection;

    @JsonCreator
    public StaticUserConfig(@JsonProperty("user_name") String userName,
                           @JsonProperty("queries") List<String> queries,
                           @JsonProperty("queries_file") String queriesFile,
                           @JsonProperty("time_left_border") int timeLeftBorder,
                           @JsonProperty("time_right_border") int timeRightBorder,
                           @JsonProperty("times") List<TimePeriod> times,
                           @JsonProperty("max_time_left") Integer maxTimeLeft,
                           @JsonProperty("max_time_right") Integer maxTimeRight,
                           @JsonProperty("filters") List<String> filters,
                           @JsonProperty("filters_file") String filtersFile,
                           @JsonProperty("filter_apply_probability") Integer filterApplyProbability,
                           @JsonProperty("using_filters") Boolean usingFilters,
                           @JsonProperty("is_all_select") Boolean isAllSelect,
                           @JsonProperty("min_selected_queries") Integer minSelectedQueries,
                           @JsonProperty("max_count_selected_in_percent") Integer maxCountSelectedInPercent,
                           @JsonProperty("is_random_selection") Boolean isRandomSelection) {
        super(userName, filters, filtersFile, filterApplyProbability, usingFilters);
        this.queries = (queries != null) ? Collections.unmodifiableList(new ArrayList<>(queries)) : Collections.emptyList();
        this.queriesFile = queriesFile;
        this.timeLeftBorder = timeLeftBorder;
        this.timeRightBorder = timeRightBorder;
        this.times = (times != null) ? Collections.unmodifiableList(new ArrayList<>(times)) : Collections.emptyList();
        this.maxTimeLeft = maxTimeLeft;
        this.maxTimeRight = maxTimeRight;
        this.isAllSelect = isAllSelect;
        this.minSelectedQueries = minSelectedQueries;
        this.maxCountSelectedInPercent = maxCountSelectedInPercent;
        this.isRandomSelection = isRandomSelection;
    }

    @Override
    public UserType getUserType() {
        return UserType.STATIC;
    }

    public List<String> getQueries() {
        return queries;
    }

    public RandomQueryConfig getQueriesAsConfigs() {
        return new RandomQueryConfig(
            queries,
            null,
            isAllSelect,
            minSelectedQueries,
            maxCountSelectedInPercent,
            isRandomSelection
        );
    }

    public boolean hasQueries() {
        return queries != null && !queries.isEmpty();
    }

    public String getQueriesFile() {
        return queriesFile;
    }

    public boolean hasQueriesFile() {
        return queriesFile != null && !queriesFile.isEmpty();
    }

    public int getTimeLeftBorder() {
        return timeLeftBorder;
    }

    public int getTimeRightBorder() {
        return timeRightBorder;
    }

    public List<TimePeriod> getTimes() {
        return times;
    }

    public Integer getMaxTimeLeft() {
        return maxTimeLeft;
    }

    public Integer getMaxTimeRight() {
        return maxTimeRight;
    }

    public Boolean getIsAllSelect() {
        return isAllSelect;
    }

    public Integer getMinSelectedQueries() {
        return minSelectedQueries;
    }

    public Integer getMaxCountSelectedInPercent() {
        return maxCountSelectedInPercent;
    }

    public Boolean getIsRandomSelection() {
        return isRandomSelection;
    }

    public boolean hasTimePeriods() {
        return times != null && !times.isEmpty();
    }

    public boolean hasFixedTimeBorders() {
        return timeLeftBorder > 0 || timeRightBorder > 0;
    }

    public boolean usesNamedTimePeriods() {
        return hasTimePeriods();
    }

    public StaticUserConfig withLoadedQueries(List<String> loadedQueries) {
        return new StaticUserConfig(
            userName,
            loadedQueries,
            queriesFile,
            timeLeftBorder,
            timeRightBorder,
            times,
            maxTimeLeft,
            maxTimeRight,
            getFilters(),
            getFiltersFile(),
            filterApplyProbability,
            usingFilters,
            isAllSelect,
            minSelectedQueries,
            maxCountSelectedInPercent,
            isRandomSelection
        );
    }
    
    public StaticUserConfig withLoadedFilters(List<String> loadedFilters, int defaultFilterProbability) {
        return new StaticUserConfig(
            userName,
            queries,
            queriesFile,
            timeLeftBorder,
            timeRightBorder,
            times,
            maxTimeLeft,
            maxTimeRight,
            loadedFilters,
            filtersFile, 
            filterApplyProbability != null ? filterApplyProbability : defaultFilterProbability,
            usingFilters,
            isAllSelect,
            minSelectedQueries,
            maxCountSelectedInPercent,
            isRandomSelection
        );
    }
}
