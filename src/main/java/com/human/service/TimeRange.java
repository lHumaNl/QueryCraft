package com.human.service;

public class TimeRange {
    private final long leftBorder;
    private final long rightBorder;

    public TimeRange(long leftBorder, long rightBorder) {
        if (leftBorder > rightBorder) {
            throw new IllegalArgumentException("Left border cannot be greater than right border");
        }
        this.leftBorder = leftBorder;
        this.rightBorder = rightBorder;
    }

    public long getLeftBorder() {
        return leftBorder;
    }

    public long getRightBorder() {
        return rightBorder;
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "leftBorder=" + leftBorder +
                ", rightBorder=" + rightBorder +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRange timeRange = (TimeRange) o;
        return leftBorder == timeRange.leftBorder && rightBorder == timeRange.rightBorder;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(leftBorder) * 31 + Long.hashCode(rightBorder);
    }
}
