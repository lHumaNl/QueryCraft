package com.human.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomQueryConfig {

    @JsonProperty("queries")
    private final List<String> queries;

    @JsonProperty("queries_file")
    private final String queriesFile;

    @JsonProperty("is_all_select")
    private final Boolean isAllSelect;

    @JsonProperty("min_selected_queries")
    private final Integer minSelectedQueries;

    @JsonProperty("max_count_selected_in_percent")
    private final Integer maxCountSelectedInPercent;

    @JsonProperty("is_random_selection")
    private final Boolean isRandomSelection;

    @JsonCreator
    public RandomQueryConfig(@JsonProperty("queries") List<String> queries,
                             @JsonProperty("queries_file") String queriesFile,
                             @JsonProperty("is_all_select") Boolean isAllSelect,
                             @JsonProperty("min_selected_queries") Integer minSelectedQueries,
                             @JsonProperty("max_count_selected_in_percent") Integer maxCountSelectedInPercent,
                             @JsonProperty("is_random_selection") Boolean isRandomSelection) {
        this.queries = (queries != null) ? Collections.unmodifiableList(new ArrayList<>(queries)) : Collections.emptyList();
        this.queriesFile = queriesFile;
        this.isAllSelect = (isAllSelect != null) ? isAllSelect : true;
        this.minSelectedQueries = (minSelectedQueries != null) ? minSelectedQueries : 2;
        this.maxCountSelectedInPercent = (maxCountSelectedInPercent != null) ? maxCountSelectedInPercent : 50;
        this.isRandomSelection = (isRandomSelection != null) ? isRandomSelection : true;
    }

    public RandomQueryConfig(String singleQuery) {
        this.queries = Collections.singletonList(singleQuery);
        this.queriesFile = null;
        this.isAllSelect = true; // Default: select all queries
        this.minSelectedQueries = 2; // Default minimum
        this.maxCountSelectedInPercent = 50; // Default percentage
        this.isRandomSelection = true; // Default: random selection
    }

    public List<String> getQueries() {
        return queries;
    }

    public String getQueriesFile() {
        return queriesFile;
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

    public boolean hasInlineQueries() {
        return queries != null && !queries.isEmpty();
    }

    public boolean hasQueriesFile() {
        return queriesFile != null && !queriesFile.isEmpty();
    }

    public boolean isAllSelectEnabled() {
        return isAllSelect != null && isAllSelect;
    }

    public boolean isRandomSelectionEnabled() {
        return isRandomSelection != null && isRandomSelection;
    }

    public RandomQueryConfig withLoadedQueries(List<String> loadedQueries) {
        return new RandomQueryConfig(
                loadedQueries,
                queriesFile,
                isAllSelect,
                minSelectedQueries,
                maxCountSelectedInPercent,
                isRandomSelection
        );
    }

    @Override
    public String toString() {
        return "RandomQueryConfig{" +
                "queries=" + (queries != null ? queries.size() + " items" : "null") +
                ", queriesFile='" + queriesFile + '\'' +
                ", isAllSelect=" + isAllSelect +
                ", minSelectedQueries=" + minSelectedQueries +
                ", maxCountSelectedInPercent=" + maxCountSelectedInPercent +
                ", isRandomSelection=" + isRandomSelection +
                '}';
    }
}
