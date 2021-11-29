package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ProtocolProperties {

    HTTP_REST("HTTP", "REST"),
    HTTP_SOAP("HTTP", "SOAP"),
    HTTPS_REST("HTTPS", "REST"),
    HTTPS_SOAP("HTTPS", "SOAP"),
    AMQP_RABBITMQ("AMQP", "RABBITMQ"),
    DEFAULT("HTTP", "REST");


    private final String protocol;
    private final String connectionType;

    ProtocolProperties(String protocol, String connectionType) {
        this.protocol = protocol;
        this.connectionType = connectionType;
    }

    public String getProtocol() {
        return protocol;
    }


    public String getConnectionType() {
        return connectionType;
    }

    @JsonCreator
    public static ProtocolProperties forValues(@JsonProperty("protocol") String protocol,
                                               @JsonProperty("connectionType") String connectionType) {
        return Arrays.stream(ProtocolProperties.values())
                .filter(type -> type.protocol.equalsIgnoreCase(protocol) && type.connectionType.equalsIgnoreCase(connectionType))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No constant with protocol: " + protocol + " and connectionType " + connectionType + " found"));
    }

}
