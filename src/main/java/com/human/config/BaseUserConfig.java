package com.human.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.human.enums.UserType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "user_type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StaticUserConfig.class, name = "static"),
        @JsonSubTypes.Type(value = RandomUserConfig.class, name = "random")
})
public abstract class BaseUserConfig {

    @JsonProperty("user_name")
    protected final String userName;

    @JsonProperty("filters")
    protected final List<String> filters;

    @JsonProperty("filters_file")
    protected final String filtersFile;

    @JsonProperty("filter_apply_probability")
    protected final Integer filterApplyProbability;

    @JsonProperty("using_filters")
    protected final boolean usingFilters;

    protected BaseUserConfig(String userName,
                             List<String> filters,
                             String filtersFile,
                             Integer filterApplyProbability,
                             Boolean usingFilters) {
        this.userName = userName;
        this.filters = (filters != null) ? Collections.unmodifiableList(new ArrayList<>(filters)) : Collections.emptyList();
        this.filtersFile = filtersFile;
        this.filterApplyProbability = filterApplyProbability;
        this.usingFilters = Optional.ofNullable(usingFilters).orElse(true);
    }

    public abstract UserType getUserType();

    public String getUserName() {
        return userName;
    }

    public List<String> getFilters() {
        return filters;
    }

    public boolean hasFilters() {
        return filters != null && !filters.isEmpty();
    }

    public String getFiltersFile() {
        return filtersFile;
    }

    public Integer getFilterApplyProbability() {
        return filterApplyProbability;
    }

    public boolean isUsingFilters() {
        return usingFilters;
    }

    public boolean hasInlineFilters() {
        return filters != null && !filters.isEmpty();
    }

    public boolean hasFiltersFile() {
        return filtersFile != null && !filtersFile.isEmpty();
    }

    public abstract BaseUserConfig withLoadedQueries(List<String> loadedQueries);

    public abstract BaseUserConfig withLoadedFilters(List<String> loadedFilters, int defaultFilterProbability);
}
