package org.satel.eip.project14.adapter.pyramid.domain.command.response;

import java.util.List;


public class CommandResponse {
    private List<String> guids;
    private String commandUuid;
    private String errorMsg;

    public CommandResponse(String commandUuid, List<String> guids, String errorMsg) {
        this.guids = guids;
        this.commandUuid = commandUuid;
        this.errorMsg = errorMsg;
    }

    public List<String> getGuids() {
        return guids;
    }

    public String getCommandUuid() {
        return commandUuid;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
