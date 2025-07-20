package com.human.service;

import com.human.config.RandomUserConfig;
import com.human.config.RandomQueryConfig;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RandomQuerySelector {
    public List<String> selectQueriesFromConfigs(RandomUserConfig config,
                                                 java.util.Map<String, RandomQueryConfig> dynamicQueryTemplates) {
        if (config == null) {
            throw new IllegalArgumentException("RandomUserConfig cannot be null");
        }
        if (dynamicQueryTemplates == null) {
            throw new IllegalArgumentException("DynamicQueryTemplates cannot be null");
        }

        if (config.hasInlineQueries()) {
            Map<String, RandomQueryConfig> userQueries = config.getQueries();

            RandomQueryConfig randomQueryConfig = selectPageFromRandomQueryConfigMap(userQueries);

            return selectQueriesFromRandomQueryConfig(randomQueryConfig);
        }

        if (config.hasQueriesFile()) {
            return dynamicQueryTemplates.values().stream()
                    .flatMap(queryConfig -> queryConfig.getQueries().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
        }

        String selectedPage = selectPageFromRandomQueryConfigs(config, dynamicQueryTemplates);

        RandomQueryConfig queryConfig = dynamicQueryTemplates.get(selectedPage);
        if (queryConfig == null || queryConfig.getQueries().isEmpty()) {
            return Collections.emptyList();
        }

        return selectQueriesFromRandomQueryConfig(queryConfig);
    }

    public static class QuerySelectionResult {
        private final List<String> queries;
        private final String pageName;

        public QuerySelectionResult(List<String> queries, String pageName) {
            this.queries = queries != null ? Collections.unmodifiableList(new ArrayList<>(queries)) : Collections.emptyList();
            this.pageName = pageName;
        }

        public List<String> getQueries() {
            return queries;
        }

        public String getPageName() {
            return pageName;
        }
    }

    public QuerySelectionResult selectQueriesWithPageName(RandomUserConfig config,
                                                          Map<String, RandomQueryConfig> dynamicQueryTemplates) {
        if (config == null) {
            throw new IllegalArgumentException("RandomUserConfig cannot be null");
        }
        if (dynamicQueryTemplates == null) {
            throw new IllegalArgumentException("DynamicQueryTemplates cannot be null");
        }

        if (config.hasInlineQueries()) {
            Map<String, RandomQueryConfig> userQueries = config.getQueries();

            List<String> pageNames = new ArrayList<>(userQueries.keySet());
            if (pageNames.isEmpty()) {
                return new QuerySelectionResult(Collections.emptyList(), "Unknown");
            }

            String selectedPageName = pageNames.get(ThreadLocalRandom.current().nextInt(pageNames.size()));
            RandomQueryConfig randomQueryConfig = userQueries.get(selectedPageName);
            List<String> selectedQueries = selectQueriesFromRandomQueryConfig(randomQueryConfig, selectedPageName);

            return new QuerySelectionResult(selectedQueries, selectedPageName);
        }

        if (config.hasQueriesFile()) {
            List<String> allQueries = dynamicQueryTemplates.values().stream()
                    .flatMap(queryConfig -> queryConfig.getQueries().stream())
                    .distinct()
                    .collect(Collectors.toList());
            return new QuerySelectionResult(allQueries, "Mixed");
        }

        String selectedPage = selectPageFromRandomQueryConfigs(config, dynamicQueryTemplates);

        RandomQueryConfig queryConfig = dynamicQueryTemplates.get(selectedPage);
        if (queryConfig == null || queryConfig.getQueries().isEmpty()) {
            return new QuerySelectionResult(Collections.emptyList(), selectedPage);
        }

        List<String> selectedQueries = selectQueriesFromRandomQueryConfig(queryConfig, selectedPage);

        return new QuerySelectionResult(selectedQueries, selectedPage);
    }

    private String selectPage(RandomUserConfig config, java.util.Map<String, List<String>> dynamicQueryTemplates) {
        List<String> availablePages = new ArrayList<>(dynamicQueryTemplates.keySet());

        if (availablePages.isEmpty()) {
            throw new IllegalStateException("No pages available in dynamic query templates");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(availablePages.size());
        return availablePages.get(randomIndex);
    }

    private String selectPageFromRandomQueryConfigs(RandomUserConfig config, Map<String, RandomQueryConfig> dynamicQueryTemplates) {
        List<String> availablePages = new ArrayList<>(dynamicQueryTemplates.keySet());

        if (availablePages.isEmpty()) {
            throw new IllegalStateException("No pages available in dynamic query templates");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(availablePages.size());
        return availablePages.get(randomIndex);
    }

    private List<String> getTemplatesForPage(String selectedPage, RandomUserConfig config,
                                             java.util.Map<String, List<String>> dynamicQueryTemplates) {
        List<String> templates = dynamicQueryTemplates.get(selectedPage);
        return templates != null ? new ArrayList<>(templates) : Collections.emptyList();
    }

    private String selectPageFromMap(Map<String, List<String>> userQueries) {
        List<String> availablePages = new ArrayList<>(userQueries.keySet());

        if (availablePages.isEmpty()) {
            throw new IllegalStateException("No pages available in user queries");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(availablePages.size());
        return availablePages.get(randomIndex);
    }

    private RandomQueryConfig selectPageFromRandomQueryConfigMap(Map<String, RandomQueryConfig> userQueries) {
        List<String> availablePages = new ArrayList<>(userQueries.keySet());

        if (availablePages.isEmpty()) {
            throw new IllegalStateException("No pages available in user queries");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(availablePages.size());
        return userQueries.get(availablePages.get(randomIndex));
    }

    public List<String> selectQueriesFromRandomQueryConfig(RandomQueryConfig queryConfig) {
        List<String> availableQueries = new ArrayList<>(queryConfig.getQueries());

        if (availableQueries.isEmpty()) {
            return Collections.emptyList();
        }

        if (queryConfig.isAllSelectEnabled()) {
            return availableQueries;
        }

        if (!queryConfig.isRandomSelectionEnabled()) {
            int count = Math.min(queryConfig.getMinSelectedQueries(), availableQueries.size());
            return availableQueries.subList(0, count);
        }

        int maxCount = Math.max(
                queryConfig.getMinSelectedQueries(),
                (availableQueries.size() * queryConfig.getMaxCountSelectedInPercent()) / 100
        );

        int selectedCount = Math.min(maxCount, availableQueries.size());

        Collections.shuffle(availableQueries);
        return availableQueries.subList(0, selectedCount);
    }

    public List<String> selectQueriesFromRandomQueryConfig(RandomQueryConfig queryConfig, String pageName) {
        List<String> availableQueries = new ArrayList<>(queryConfig.getQueries());

        if (availableQueries.isEmpty()) {
            return Collections.emptyList();
        }

        if (queryConfig.isAllSelectEnabled()) {
            return availableQueries;
        }

        if (!queryConfig.isRandomSelectionEnabled()) {
            int count = Math.min(queryConfig.getMinSelectedQueries(), availableQueries.size());
            return availableQueries.subList(0, count);
        }

        int maxCount = Math.max(
                queryConfig.getMinSelectedQueries(),
                (availableQueries.size() * queryConfig.getMaxCountSelectedInPercent()) / 100
        );

        int selectedCount = Math.min(maxCount, availableQueries.size());

        Collections.shuffle(availableQueries);
        return availableQueries.subList(0, selectedCount);
    }
}
