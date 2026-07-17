package com.jtprince.coordinateoffset.command;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface OffsetCommand {

    OffsetCommandSender getCommandSender();
}
