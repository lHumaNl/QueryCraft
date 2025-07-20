package com.human.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UserType {
    @JsonProperty("static")
    STATIC,
    @JsonProperty("random")
    RANDOM
}
