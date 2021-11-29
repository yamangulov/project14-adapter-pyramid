package org.satel.eip.project14.adapter.pyramid.domain.command.response.oldUnused;

import java.util.List;


public class GetGuidPartnerByFilterRequestCommandResponse {
    private List<String> uuids;
    private String commandUuid;

    public GetGuidPartnerByFilterRequestCommandResponse(List<String> uuids, String commandUuid) {
        this.uuids = uuids;
        this.commandUuid = commandUuid;
    }

    public List<String> getUuids() {
        return uuids;
    }

    public String getCommandUuid() {
        return commandUuid;
    }
}
