package com.human.config;

import java.util.List;
import java.util.Map;

public class AppConfig {

    private final Map<String, BaseUserConfig> userConfigs;
    private final Map<String, RandomQueryConfig> dynamicQueryTemplates;
    private final List<String> allFilters;
    private final String defaultFiltersKey;
    private final int baseProbabilityFilter;

    public AppConfig(Map<String, BaseUserConfig> userConfigs,
                     Map<String, RandomQueryConfig> dynamicQueryTemplates) {
        this(userConfigs, dynamicQueryTemplates, null, "DEFAULT_FILTERS", 50);
    }

    public AppConfig(Map<String, BaseUserConfig> userConfigs,
                     Map<String, RandomQueryConfig> dynamicQueryTemplates,
                     List<String> allFilters,
                     String defaultFiltersKey,
                     int baseProbabilityFilter) {
        this.userConfigs = userConfigs;
        this.dynamicQueryTemplates = dynamicQueryTemplates;
        this.allFilters = allFilters;
        this.defaultFiltersKey = defaultFiltersKey;
        this.baseProbabilityFilter = baseProbabilityFilter;
    }

    public Map<String, BaseUserConfig> getUserConfigs() {
        return userConfigs;
    }

    public Map<String, RandomQueryConfig> getDynamicQueryTemplates() {
        return dynamicQueryTemplates;
    }

    public List<String> getAllFilters() {
        return allFilters;
    }

    public String getDefaultFiltersKey() {
        return defaultFiltersKey;
    }

    public int getBaseProbabilityFilter() {
        return baseProbabilityFilter;
    }

    public BaseUserConfig getUserConfig(String userName) {
        return userConfigs.get(userName);
    }

    public boolean hasUser(String userName) {
        return userConfigs.containsKey(userName);
    }

    public RandomQueryConfig getQueriesForPage(String pageName) {
        return dynamicQueryTemplates.get(pageName);
    }

    public boolean hasPage(String pageName) {
        return dynamicQueryTemplates.containsKey(pageName);
    }

    public List<String> getQueriesListForPage(String pageName) {
        RandomQueryConfig config = dynamicQueryTemplates.get(pageName);
        return config != null ? config.getQueries() : null;
    }

    public Map<String, List<String>> getDynamicQueryTemplatesAsMap() {
        return dynamicQueryTemplates.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getQueries()
                ));
    }
}
