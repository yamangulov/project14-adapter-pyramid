package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum RestRequestType {
    METEREVENTS("meterevents"),
    METERPOINTSBYMETERPARAMETERSBATCH("meterpointsbymeterparametersbatch");
    private final String rootDir;

    RestRequestType(String rootDir) {
        this.rootDir = rootDir;
    }
}
