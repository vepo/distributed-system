package io.vepo.distributedsystem.leaderelection;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class Command {
    private static Map<Byte, Class<? extends Command>> COMMANDS = new HashMap<>();
    static {
        COMMANDS.put(JoinCommand.COMMAND_ID, JoinCommand.class);
    }
    private byte commandId;

    protected Command(byte commandId) {
        this.commandId = commandId;
    }

    protected abstract String name();

    public byte commandId() {
        return commandId;
    }

    protected abstract void load(byte[] data);

    public static Optional<Command> loadCommand(byte[] data) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        if (data.length > 0) {
            var cmdClass = COMMANDS.get(data[0]);
            if (Objects.nonNull(cmdClass)) {
                var cmd = cmdClass.getDeclaredConstructor().newInstance();
                cmd.load(data);
                return Optional.of(cmd);
            }
        }
        return Optional.empty();
    }

    public abstract byte[] getBytes();

}
