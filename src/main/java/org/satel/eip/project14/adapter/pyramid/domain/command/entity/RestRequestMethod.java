package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RestRequestMethod {

    @JsonProperty("GET")
    GET("GET"),

    @JsonProperty("POST")
    POST("POST"),

    @JsonProperty("PUT")
    PUT("PUT"),

    @JsonProperty("PATCH")
    PATCH("PATCH"),

    @JsonProperty("DELETE")
    DELETE("DELETE"),

    @JsonProperty("TRACE")
    TRACE("TRACE");

    private final String method;

    RestRequestMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

}