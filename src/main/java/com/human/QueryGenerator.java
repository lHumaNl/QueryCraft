package com.human;

import com.human.config.*;
import com.human.enums.TimePeriod;
import com.human.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class QueryGenerator {

    private final AppConfig appConfig;
    private final TimeRangeCalculator timeRangeCalculator;
    private final FilterApplier filterApplier;
    private final QueryTemplateProcessor templateProcessor;
    private final RandomQuerySelector querySelector;
    private final Random random = new Random();

    public QueryGenerator(AppConfig appConfig) {
        if (appConfig == null) {
            throw new IllegalArgumentException("ImprovedAppConfig cannot be null");
        }
        this.appConfig = appConfig;
        this.timeRangeCalculator = new TimeRangeCalculator();
        this.filterApplier = new FilterApplier();
        this.templateProcessor = new QueryTemplateProcessor();
        this.querySelector = new RandomQuerySelector();
    }

    public QueryResult generateQueries(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }

        try {
            BaseUserConfig userConfig = Optional.ofNullable(appConfig.getUserConfigs().get(userName))
                    .orElseThrow(() -> new IllegalArgumentException("User config not found: " + userName));

            if (userConfig instanceof StaticUserConfig) {
                return processStaticUser((StaticUserConfig) userConfig);
            } else if (userConfig instanceof RandomUserConfig) {
                return processRandomUser((RandomUserConfig) userConfig);
            } else {
                throw new IllegalStateException("Unknown UserType: " + userConfig.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate BQL queries for user: " + userName, e);
        }
    }

    private QueryResult processStaticUser(StaticUserConfig config) {
        try {
            String timePeriodName;
            TimeRange timeRange;

            if (config.usesNamedTimePeriods()) {
                List<TimePeriod> availablePeriods = config.getTimes();
                if (!availablePeriods.isEmpty()) {
                    TimePeriod selectedPeriod = availablePeriods.get(random.nextInt(availablePeriods.size()));

                    long currentTime = System.currentTimeMillis() / 1000;
                    timeRange = timeRangeCalculator.calculateTimeRange(selectedPeriod, config, currentTime);

                    if (selectedPeriod.equals(TimePeriod.CUSTOM)) {
                        timePeriodName = "Custom";
                    } else {
                        timePeriodName = String.format("Last %s", formatTimeInterval(currentTime - timeRange.getLeftBorder()));
                    }
                } else {
                    throw new IllegalStateException("No time periods available for user: " + config.getUserName());
                }
            } else {
                timeRange = timeRangeCalculator.calculateTimeRange(config, System.currentTimeMillis() / 1000);

                long timeRightBorderSeconds = config.getTimeRightBorder();
                long timeLeftBorderSeconds = config.getTimeLeftBorder();

                if (timeRightBorderSeconds > 0) {
                    String leftBorder = formatTimeInterval(timeLeftBorderSeconds);
                    String rightBorder = formatTimeInterval(timeRightBorderSeconds);
                    timePeriodName = String.format("From now %s - Last %s", rightBorder, leftBorder);
                } else {
                    timePeriodName = String.format("Last %s", formatTimeInterval(timeLeftBorderSeconds));
                }
            }

            String appliedFilter = null;
            if (filterApplier.shouldApplyFilter(config)) {
                appliedFilter = filterApplier.selectFilter(config, appConfig.getAllFilters());
            }

            String userName = config.getUserName();
            if (appliedFilter != null) {
                userName = userName + " with filter";
            }

            List<String> selectedQueries = selectQueriesFromStaticConfig(config);

            List<String> processedQueries = new ArrayList<>();
            for (String queryTemplate : selectedQueries) {
                String processedQuery = templateProcessor.processTemplate(queryTemplate, timeRange, appliedFilter);
                processedQueries.add(processedQuery);
            }

            return new QueryResult(
                    processedQueries,
                    config.getUserType().toString(),
                    userName,
                    timePeriodName,
                    appliedFilter,
                    timeRange.getLeftBorder(),
                    timeRange.getRightBorder()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process static user: " + config.getUserName(), e);
        }
    }

    private List<String> selectQueriesFromStaticConfig(StaticUserConfig config) {
        return querySelector.selectQueriesFromRandomQueryConfig(config.getQueriesAsConfigs());
    }

    private QueryResult processRandomUser(RandomUserConfig config) {
        try {
            RandomQuerySelector.QuerySelectionResult selectionResult = querySelector.selectQueriesWithPageName(config, appConfig.getDynamicQueryTemplates());
            List<String> selectedTemplates = selectionResult.getQueries();
            String selectedPageName = selectionResult.getPageName();
            String userName = config.getUserName();

            if (selectedPageName != null) {
                userName = userName + " " + selectedPageName;
            }

            if (selectedTemplates.isEmpty()) {
                System.err.println("Warning: No queries selected for random user: " + config.getUserName());
                return null;
            }

            String timePeriodName;
            TimeRange timeRange;

            List<TimePeriod> availablePeriods = config.getTimes();
            if (availablePeriods != null && !availablePeriods.isEmpty()) {
                TimePeriod selectedPeriod = availablePeriods.get(random.nextInt(availablePeriods.size()));

                long currentTime = System.currentTimeMillis() / 1000;

                timeRange = timeRangeCalculator.calculateTimeRange(selectedPeriod, config, currentTime);
                if (selectedPeriod.equals(TimePeriod.CUSTOM)) {
                    timePeriodName = "Custom";
                } else {
                    timePeriodName = String.format("Last %s", formatTimeInterval(currentTime - timeRange.getLeftBorder()));
                }
            } else if (config.hasFixedTimeBorders()) {
                timeRange = timeRangeCalculator.calculateTimeRange(config, System.currentTimeMillis() / 1000);

                long timeRightBorderSeconds = config.getTimeRightBorder() != null ? config.getTimeRightBorder() : 0;
                long timeLeftBorderSeconds = config.getTimeLeftBorder() != null ? config.getTimeLeftBorder() : 3600;

                if (timeRightBorderSeconds > 0) {
                    String leftBorder = formatTimeInterval(timeLeftBorderSeconds);
                    String rightBorder = formatTimeInterval(timeRightBorderSeconds);
                    timePeriodName = String.format("From now %s - Last %s", rightBorder, leftBorder);
                } else {
                    timePeriodName = String.format("Last %s", formatTimeInterval(timeLeftBorderSeconds));
                }
            } else {
                throw new IllegalStateException("No time configuration available for user: " + config.getUserName());
            }

            String appliedFilter = null;
            if (filterApplier.shouldApplyFilter(config)) {
                appliedFilter = filterApplier.selectFilter(config, appConfig.getAllFilters());
            }

            if (appliedFilter != null) {
                userName = userName + " with filter";
            }

            List<String> processedQueries = new ArrayList<>();
            for (String queryTemplate : selectedTemplates) {
                String processedQuery = templateProcessor.processTemplate(queryTemplate, timeRange, appliedFilter);
                processedQueries.add(processedQuery);
            }

            return new QueryResult(
                    processedQueries,
                    config.getUserType().toString(),
                    userName,
                    timePeriodName,
                    appliedFilter,
                    timeRange.getLeftBorder(),
                    timeRange.getRightBorder()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process random user: " + config.getUserName(), e);
        }
    }

    private String formatTimeInterval(long seconds) {
        if (seconds == 0) {
            return "0S";
        }

        long hours = seconds / 3600;
        long remainingSeconds = seconds % 3600;
        long minutes = remainingSeconds / 60;
        long finalSeconds = remainingSeconds % 60;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append("h");
        }
        if (minutes > 0) {
            result.append(minutes).append("m");
        }
        if (finalSeconds > 0) {
            result.append(finalSeconds).append("s");
        }

        return result.toString();
    }
}
