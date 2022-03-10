package io.vepo.distributedsystem.leaderelection.commands;

import java.nio.ByteBuffer;

public class LeaderElectedCommand extends Command {
    public static final byte COMMAND_ID = (byte) 0x03;
    private String leaderId;

    public LeaderElectedCommand() {
        super(COMMAND_ID);
    }

    public LeaderElectedCommand(String leaderId) {
        this();
        this.leaderId = leaderId;
    }

    @Override
    protected String name() {
        return "Leader Elected";
    }

    public String leaderId() {
        return leaderId;
    }

    @Override
    public void load(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        buffer.get();
        var leaderIdData = new byte[buffer.getInt()];
        buffer.get(leaderIdData, 0, leaderIdData.length);
        this.leaderId = new String(leaderIdData);
    }

    @Override
    public byte[] getBytes() {
        var leaderIdData = leaderId.getBytes();
        var buffer = ByteBuffer.allocate(1 + Integer.BYTES + leaderIdData.length);
        buffer.put(COMMAND_ID);
        buffer.putInt(leaderIdData.length);
        buffer.put(leaderIdData, 0, leaderIdData.length);
        return buffer.array();
    }

    @Override
    public String toString() {
        return String.format("Command LeaderElected[leaderId=%s]", leaderId);
    }

}
