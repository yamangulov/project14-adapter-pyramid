package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum RestRequestType {
    METERPARAMETERSWITHSTATUS("meterparameterswithstatus/"),
    METEREVENTS("meterevents/"),
    OBJECT("object/"),
    METER("meter/"),
    METERPOINTPARAMETERSWITHSTATUS("meterpointparameterswithstatus/");
    private final String type;

    RestRequestType(String type) {
        this.type = type;
    }
}
