package io.vepo.distributedsystem.leaderelection.commands;

import java.nio.ByteBuffer;

public class JoinCommand extends Command {

    public static final byte COMMAND_ID = (byte) 0x01;
    private long pid;
    private String id;
    private long startup;
    private String hostname;

    public JoinCommand(String id, String hostname, long pid, long startup) {
        super(COMMAND_ID);
        this.id = id;
        this.hostname = hostname;
        this.pid = pid;
        this.startup = startup;
    }

    public JoinCommand() {
        super(COMMAND_ID);
    }

    @Override
    protected String name() {
        return "Join";
    }

    public String id() {
        return id;
    }

    public long pid() {
        return pid;
    }

    public String hostname() {
        return hostname;
    }

    public long startup() {
        return startup;
    }

    @Override
    public void load(byte[] data) {
        var buffer = ByteBuffer.wrap(data);
        buffer.get();
        var idData = new byte[buffer.getInt()];
        buffer.get(idData);
        this.id = new String(idData);

        var hostnameData = new byte[buffer.getInt()];
        buffer.get(hostnameData);
        this.hostname = new String(hostnameData);

        this.pid = buffer.getLong();
        this.startup = buffer.getLong();
    }

    @Override
    public byte[] getBytes() {
        var idData = id.getBytes();
        var hostnameData = hostname.getBytes();
        var buffer = ByteBuffer.allocate(1 + idData.length +
                hostnameData.length + Integer.BYTES + Integer.BYTES + Long.BYTES + Long.BYTES);

        buffer.put(COMMAND_ID);

        buffer.putInt(idData.length);
        buffer.put(idData, 0, idData.length);

        buffer.putInt(hostnameData.length);
        buffer.put(hostnameData, 0, hostnameData.length);

        buffer.putLong(pid);

        buffer.putLong(startup);
        return buffer.array();
    }

    @Override
    public String toString() {
        return String.format("Command Join[id=%s, hostname=%s, pid=%d, startup=%d]", id, hostname, pid, startup);
    }
}
