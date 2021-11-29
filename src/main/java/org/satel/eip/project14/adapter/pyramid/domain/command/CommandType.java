package org.satel.eip.project14.adapter.pyramid.domain.command;

public enum CommandType {
    GET_PYRAMID_METERS_REQUEST("GetPyramidMetersRequestCommand");

    private final String name;

    CommandType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static CommandType getCommandTypeByString(String name) {
        for (CommandType e : CommandType.values()) {
            if (e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }
}
