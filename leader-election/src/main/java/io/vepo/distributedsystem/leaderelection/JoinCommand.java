package io.vepo.distributedsystem.leaderelection;

public class JoinCommand extends Command {

    protected static final byte COMMAND_ID = (byte) 0x01;
    private long pid;
    private String id;

    public JoinCommand(String id, long pid) {
        super(COMMAND_ID);
        this.id = id;
        this.pid = pid;
    }

    protected JoinCommand() {
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

    @Override
    protected void load(byte[] data) {
        var idData = new byte[data.length - 1 - Long.BYTES];
        System.arraycopy(data, 1, idData, 0, data.length - 1 - Long.BYTES);
        this.id = new String(idData);
        this.pid = ((long) data[idData.length + 1 + 7] << 56)
                | ((long) data[idData.length + 1 + 6] & 0xff) << 48
                | ((long) data[idData.length + 1 + 5] & 0xff) << 40
                | ((long) data[idData.length + 1 + 4] & 0xff) << 32
                | ((long) data[idData.length + 1 + 3] & 0xff) << 24
                | ((long) data[idData.length + 1 + 2] & 0xff) << 16
                | ((long) data[idData.length + 1 + 1] & 0xff) << 8
                | ((long) data[idData.length + 1 + 0] & 0xff);
    }

    @Override
    public byte[] getBytes() {
        var idData = id.getBytes();
        var data = new byte[1 + idData.length + Long.BYTES];
        data[0] = COMMAND_ID;
        System.arraycopy(idData, 0, data, 1, idData.length);

        var lPid = pid;
        data[idData.length + 1] = (byte) lPid;
        data[idData.length + 2] = (byte) (lPid >> 8);
        data[idData.length + 3] = (byte) (lPid >> 16);
        data[idData.length + 4] = (byte) (lPid >> 24);
        data[idData.length + 5] = (byte) (lPid >> 32);
        data[idData.length + 6] = (byte) (lPid >> 40);
        data[idData.length + 7] = (byte) (lPid >> 48);
        data[idData.length + 8] = (byte) (lPid >> 56);
        return data;
    }

    @Override
    public String toString() {
        return String.format("Command Join[id=%s, pid=%d]", id, pid);
    }
}
