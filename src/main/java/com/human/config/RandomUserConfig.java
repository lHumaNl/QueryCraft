package com.human.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.human.enums.TimePeriod;
import com.human.enums.UserType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RandomUserConfig extends BaseUserConfig {

    @JsonProperty("queries")
    private final Map<String, RandomQueryConfig> queries;

    @JsonProperty("times")
    private final List<TimePeriod> times;

    @JsonProperty("max_time_left")
    private final Long maxTimeLeft;

    @JsonProperty("max_time_right")
    private final Long maxTimeRight;

    @JsonProperty("time_left_border")
    private final Integer timeLeftBorder;

    @JsonProperty("time_right_border")
    private final Integer timeRightBorder;

    @JsonProperty("queries_file")
    private final String queriesFile;

    @JsonCreator
    public RandomUserConfig(@JsonProperty("user_name") String userName,
                            @JsonProperty("queries") Map<String, RandomQueryConfig> queries,
                            @JsonProperty("times") List<TimePeriod> times,
                            @JsonProperty("max_time_left") Long maxTimeLeft,
                            @JsonProperty("max_time_right") Long maxTimeRight,
                            @JsonProperty("time_left_border") Integer timeLeftBorder,
                            @JsonProperty("time_right_border") Integer timeRightBorder,
                            @JsonProperty("queries_file") String queriesFile,
                            @JsonProperty("filters") List<String> filters,
                            @JsonProperty("filters_file") String filtersFile,
                            @JsonProperty("filter_apply_probability") Integer filterApplyProbability,
                            @JsonProperty("using_filters") Boolean usingFilters) {
        super(userName, filters, filtersFile, filterApplyProbability, usingFilters);
        this.queries = queries != null ? Collections.unmodifiableMap(queries) : Collections.emptyMap();
        this.times = times != null ? Collections.unmodifiableList(times) : Collections.emptyList();
        this.maxTimeLeft = maxTimeLeft;
        this.maxTimeRight = maxTimeRight;
        this.timeLeftBorder = timeLeftBorder;
        this.timeRightBorder = timeRightBorder;
        this.queriesFile = queriesFile;
    }

    @Override
    public UserType getUserType() {
        return UserType.RANDOM;
    }

    public Map<String, RandomQueryConfig> getQueries() {
        return queries;
    }

    public List<TimePeriod> getTimes() {
        return times;
    }

    public Long getMaxTimeLeft() {
        return maxTimeLeft;
    }

    public Long getMaxTimeRight() {
        return maxTimeRight;
    }

    public Integer getTimeLeftBorder() {
        return timeLeftBorder;
    }

    public Integer getTimeRightBorder() {
        return timeRightBorder;
    }

    public String getQueriesFile() {
        return queriesFile;
    }

    public boolean hasFixedTimeBorders() {
        return timeLeftBorder != null && timeRightBorder != null;
    }

    public boolean hasQueriesFile() {
        return queriesFile != null && !queriesFile.isEmpty();
    }

    public boolean hasInlineQueries() {
        return queries != null && !queries.isEmpty();
    }

    public boolean usesDynamicTemplates() {
        return !hasInlineQueries() && !hasQueriesFile();
    }

    public List<String> getQueriesAsList() {
        return queries.values().stream()
                .flatMap(config -> config.getQueries().stream())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getQueriesForPage(String pageName) {
        RandomQueryConfig config = queries.get(pageName);
        return config != null ? config.getQueries() : Collections.emptyList();
    }

    public RandomQueryConfig getQueryConfigForPage(String pageName) {
        return queries.get(pageName);
    }

    public RandomUserConfig withLoadedQueries(Map<String, RandomQueryConfig> loadedQueries) {
        return new RandomUserConfig(
                userName,
                loadedQueries,
                times,
                maxTimeLeft,
                maxTimeRight,
                timeLeftBorder,
                timeRightBorder,
                queriesFile,
                getFilters(),
                getFiltersFile(),
                filterApplyProbability,
                usingFilters
        );
    }

    public RandomUserConfig withLoadedQueries(List<String> loadedQueries) {
        return this;
    }

    public RandomUserConfig withLoadedFilters(List<String> loadedFilters, int defaultFilterProbability) {
        return new RandomUserConfig(
                userName,
                queries,
                times,
                maxTimeLeft,
                maxTimeRight,
                timeLeftBorder,
                timeRightBorder,
                queriesFile,
                loadedFilters,
                filtersFile,
                filterApplyProbability != null ? filterApplyProbability : defaultFilterProbability,
                usingFilters
        );
    }
}
