package com.human.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.human.config.BaseUserConfig;

public class FilterApplier {

    public boolean shouldApplyFilter(BaseUserConfig config) {
        if (config == null || !config.isUsingFilters()) {
            return false;
        }

        int probability = config.getFilterApplyProbability();
        int randomValue = ThreadLocalRandom.current().nextInt(100);
        return randomValue <= probability;
    }

    public String selectFilter(BaseUserConfig config, List<String> availableFilters) {
        if (config == null || availableFilters == null || availableFilters.isEmpty()) {
            return null;
        }

        List<String> filtersToUse = config.hasFilters() ? config.getFilters() : availableFilters;

        if (filtersToUse.isEmpty()) {
            return null;
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(filtersToUse.size());
        return filtersToUse.get(randomIndex);
    }

    public Optional<String> selectFilter(BaseUserConfig config,
                                         java.util.Map<String, List<String>> allFilters,
                                         String defaultFiltersKey) {
        if (config == null) {
            throw new IllegalArgumentException("BaseUserConfig cannot be null");
        }
        if (allFilters == null) {
            throw new IllegalArgumentException("AllFilters cannot be null");
        }

        if (!config.isUsingFilters()) {
            return Optional.empty();
        }

        if (ThreadLocalRandom.current().nextInt(100) >= config.getFilterApplyProbability()) {
            return Optional.empty();
        }

        List<String> filtersToUse = getFiltersForUser(config, allFilters, defaultFiltersKey);
        if (filtersToUse.isEmpty()) {
            return Optional.empty();
        }

        String selectedFilter = filtersToUse.get(ThreadLocalRandom.current().nextInt(filtersToUse.size()));
        return Optional.of(selectedFilter);
    }

    private List<String> getFiltersForUser(BaseUserConfig config,
                                           java.util.Map<String, List<String>> allFilters,
                                           String defaultFiltersKey) {
        if (config.hasInlineFilters()) {
            return config.getFilters();
        }

        if (config.hasFiltersFile()) {
            String filterFile = config.getFiltersFile();
            String filterKey = filterFile.endsWith(".yaml") ?
                    filterFile.substring(0, filterFile.length() - 5) : filterFile;

            List<String> customFilters = allFilters.get(filterKey);
            if (customFilters != null && !customFilters.isEmpty()) {
                return customFilters;
            }
        }

        List<String> defaultFilters = allFilters.get(defaultFiltersKey);
        return defaultFilters != null ? defaultFilters : java.util.Collections.emptyList();
    }
}
