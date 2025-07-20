package com.human.service;

import com.human.config.BaseUserConfig;
import com.human.config.StaticUserConfig;
import com.human.config.RandomUserConfig;
import com.human.enums.TimePeriod;

import java.util.concurrent.ThreadLocalRandom;

public class TimeRangeCalculator {

    public TimeRange calculateTimeRange(StaticUserConfig config, long executionTime) {
        if (config == null) {
            throw new IllegalArgumentException("StaticUserConfig cannot be null");
        }

        long leftBorder = executionTime - config.getTimeLeftBorder();
        long rightBorder = executionTime - config.getTimeRightBorder();

        return new TimeRange(leftBorder, rightBorder);
    }

    public TimeRange calculateTimeRange(RandomUserConfig config, long executionTime) {
        if (config == null) {
            throw new IllegalArgumentException("RandomUserConfig cannot be null");
        }

        String selectedTimePeriod = selectTimePeriod(config);
        return calculateTimeRangeForPeriod(selectedTimePeriod, config, executionTime);
    }

    private String selectTimePeriod(RandomUserConfig config) {
        if (config.getTimes() == null || config.getTimes().isEmpty()) {
            return "Last1h";
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(config.getTimes().size());
        Object timePeriod = config.getTimes().get(randomIndex);
        return timePeriod.toString();
    }

    private TimeRange calculateTimeRangeForPeriod(String timePeriod, RandomUserConfig config, long executionTime) {
        long leftBorder;
        long rightBorder = executionTime;

        switch (timePeriod) {
            case "Last8h":
            case "LAST_8H":
                leftBorder = executionTime - 28800;
                break;
            case "Last24h":
            case "LAST_24H":
                leftBorder = executionTime - 86400;
                break;
            case "Custom":
            case "CUSTOM":
                long maxTimeLeft = config.getMaxTimeLeft() != null ? config.getMaxTimeLeft() : 3600L;
                long maxTimeRight = config.getMaxTimeRight() != null ? config.getMaxTimeRight() : 0L;

                leftBorder = executionTime - ThreadLocalRandom.current().nextLong(1, maxTimeLeft + 1);
                rightBorder = executionTime - ThreadLocalRandom.current().nextLong(0, maxTimeRight + 1);
                break;
            case "Last1h":
            case "LAST_1H":
            default:
                leftBorder = executionTime - 3600;
                break;
        }

        return new TimeRange(leftBorder, rightBorder);
    }

    public TimeRange calculateTimeRange(TimePeriod timePeriod, BaseUserConfig config, long executionTime) {
        if (timePeriod == null) {
            throw new IllegalArgumentException("TimePeriod cannot be null");
        }

        Long maxTimeLeft = null;
        Long maxTimeRight = null;

        if (config instanceof StaticUserConfig) {
            StaticUserConfig staticConfig = (StaticUserConfig) config;
            maxTimeLeft = staticConfig.getMaxTimeLeft() != null ? staticConfig.getMaxTimeLeft().longValue() : null;
            maxTimeRight = staticConfig.getMaxTimeRight() != null ? staticConfig.getMaxTimeRight().longValue() : null;
        } else if (config instanceof RandomUserConfig) {
            RandomUserConfig randomConfig = (RandomUserConfig) config;
            maxTimeLeft = randomConfig.getMaxTimeLeft();
            maxTimeRight = randomConfig.getMaxTimeRight();
        }

        return calculateTimeRangeForPeriod(timePeriod.name(), maxTimeLeft, maxTimeRight, executionTime);
    }


    private TimeRange calculateTimeRangeForPeriod(String timePeriod, Long maxTimeLeft, Long maxTimeRight, long executionTime) {
        long leftBorder;
        long rightBorder = executionTime;

        switch (timePeriod) {
            case "Last8h":
            case "LAST_8H":
                leftBorder = executionTime - 28800;
                break;
            case "Last24h":
            case "LAST_24H":
                leftBorder = executionTime - 86400;
                break;
            case "Custom":
            case "CUSTOM":
                if (maxTimeLeft != null && maxTimeRight != null) {
                    leftBorder = executionTime - ThreadLocalRandom.current().nextLong(1, maxTimeLeft + 1);
                    rightBorder = executionTime - ThreadLocalRandom.current().nextLong(0, maxTimeRight + 1);
                } else {
                    leftBorder = executionTime - 3600;
                }
                break;
            case "Last1h":
            case "LAST_1H":
            default:
                leftBorder = executionTime - 3600;
                break;
        }

        return new TimeRange(leftBorder, rightBorder);
    }
}
