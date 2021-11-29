package org.satel.eip.project14.adapter.pyramid.domain.command.container;

import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GenericCommand;

public class CommandParametersContainer<T extends GenericCommand> {
    T parameters;

    public CommandParametersContainer(T parameters) {
        this.parameters = parameters;
    }

    public T getCommandParameters() {
        return this.parameters;
    }
}
