package io.vepo.distributedsystem.leaderelection.commands;

import java.nio.ByteBuffer;

public class ApplyForLeaderCommand extends Command {
    protected static final byte COMMAND_ID = (byte) 0x02;

    private String id;

    public ApplyForLeaderCommand(String id) {
        this();
        this.id = id;
    }

    public ApplyForLeaderCommand() {
        super(COMMAND_ID);
    }

    @Override
    protected String name() {
        return "Apply for Leader Command";
    }

    public String id() {
        return id;
    }

    @Override
    public void load(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        buffer.get();
        var idData = new byte[buffer.getInt()];
        buffer.get(idData, 0, idData.length);
        this.id = new String(idData);
    }

    @Override
    public byte[] getBytes() {
        var idData = id.getBytes();
        var buffer = ByteBuffer.allocate(1 + Integer.BYTES + idData.length);
        buffer.put(COMMAND_ID);
        buffer.putInt(idData.length);
        buffer.put(idData, 0, idData.length);
        return buffer.array();
    }

    @Override
    public String toString() {
        return String.format("Command ApplyForLeader[id=%s]", id);
    }
}
