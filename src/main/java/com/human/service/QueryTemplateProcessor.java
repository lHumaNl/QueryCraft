package com.human.service;

import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class QueryTemplateProcessor {

    private static final ThreadLocal<StringSubstitutor> SUBSTITUTOR =
            ThreadLocal.withInitial(StringSubstitutor::new);


    public String processTemplate(String template, TimeRange timeRange, String filter) {
        if (template == null || template.trim().isEmpty()) {
            throw new IllegalArgumentException("Template cannot be null or empty");
        }
        if (timeRange == null) {
            throw new IllegalArgumentException("TimeRange cannot be null");
        }

        try {
            Map<String, Object> values = new HashMap<>();
            values.put("time_left_border", timeRange.getLeftBorder());
            values.put("time_right_border", timeRange.getRightBorder());
            values.put("filter_block", filter != null ? "(" + filter + ")" : "");
            values.put("filter_and_block", filter != null ? "AND (" + filter + ")" : "");
            values.put("filter_or_block", filter != null ? "OR (" + filter + ")" : "");

            return SUBSTITUTOR.get().replace(template, values);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process template: " + template, e);
        }
    }
}
