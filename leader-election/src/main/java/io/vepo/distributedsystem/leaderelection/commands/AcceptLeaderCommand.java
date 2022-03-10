package io.vepo.distributedsystem.leaderelection.commands;

import java.nio.ByteBuffer;

public class AcceptLeaderCommand extends Command {
    protected static final byte COMMAND_ID = (byte) 0x03;
    private String leaderId;
    private String brokerId;

    public AcceptLeaderCommand(String brokerId, String leaderId) {
        this();
        this.leaderId = leaderId;
        this.brokerId = brokerId;
    }

    public AcceptLeaderCommand() {
        super(COMMAND_ID);
    }

    public String leaderId() {
        return leaderId;
    }

    public String brokerId() {
        return brokerId;
    }

    @Override
    protected String name() {
        return "Leader Accepted";
    }

    @Override
    public void load(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        buffer.get();

        var brokerIdData = new byte[buffer.getInt()];
        buffer.get(brokerIdData, 0, brokerIdData.length);
        this.brokerId = new String(brokerIdData);

        var leaderIdData = new byte[buffer.getInt()];
        buffer.get(leaderIdData, 0, leaderIdData.length);
        this.leaderId = new String(leaderIdData);

    }

    @Override
    public byte[] getBytes() {
        var brokerIdData = brokerId.getBytes();
        var leaderIdData = leaderId.getBytes();

        var buffer = ByteBuffer.allocate(1 + Integer.BYTES + leaderIdData.length + Integer.BYTES + brokerIdData.length);
        buffer.put(COMMAND_ID);

        buffer.putInt(brokerIdData.length);
        buffer.put(brokerIdData, 0, brokerIdData.length);

        buffer.putInt(leaderIdData.length);
        buffer.put(leaderIdData, 0, leaderIdData.length);

        return buffer.array();
    }

    @Override
    public String toString() {
        return String.format("Command AcceptLeader[brokerId=%s, leaderId=%s]", brokerId, leaderId);
    }

}
