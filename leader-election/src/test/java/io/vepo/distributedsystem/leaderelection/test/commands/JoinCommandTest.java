package io.vepo.distributedsystem.leaderelection.test.commands;

import java.lang.reflect.InvocationTargetException;
import io.vepo.distributedsystem.leaderelection.commands.JoinCommand;
import io.vepo.distributedsystem.leaderelection.commands.Command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JoinCommandTest {
    private static final long DATA_1_PID = 123456789L;
    private static final long DATA_1_TS = 987654321L;
    private static final String DATA_1_ID = "012345";
    private static final String DATA_1_HOSTNAME = "localhost";

    private static final byte[] DATA_1_ARRAY = new byte[] {
            JoinCommand.COMMAND_ID,
            0x00, 0x00, 0x00, 0x06,
            '0', '1', '2', '3', '4', '5',
            0x00, 0x00, 0x00, 0x09,
            'l', 'o', 'c', 'a', 'l', 'h', 'o', 's', 't',
            (byte) (DATA_1_PID >> 56),
            (byte) (DATA_1_PID >> 48),
            (byte) (DATA_1_PID >> 40),
            (byte) (DATA_1_PID >> 32),
            (byte) (DATA_1_PID >> 24),
            (byte) (DATA_1_PID >> 16),
            (byte) (DATA_1_PID >> 8),
            (byte) DATA_1_PID,
            (byte) (DATA_1_TS >> 56),
            (byte) (DATA_1_TS >> 48),
            (byte) (DATA_1_TS >> 40),
            (byte) (DATA_1_TS >> 32),
            (byte) (DATA_1_TS >> 24),
            (byte) (DATA_1_TS >> 16),
            (byte) (DATA_1_TS >> 8),
            (byte) DATA_1_TS
    };

    @Test
    void testGetBytes() {
        Assertions.assertArrayEquals(DATA_1_ARRAY,
                new JoinCommand(DATA_1_ID, DATA_1_HOSTNAME, DATA_1_PID, DATA_1_TS).getBytes());
    }

    @Test
    void testLoad() {
        var cmd = new JoinCommand();
        cmd.load(DATA_1_ARRAY);
        Assertions.assertEquals(DATA_1_PID, cmd.pid());
        Assertions.assertEquals(DATA_1_ID, cmd.id());
        Assertions.assertEquals(DATA_1_TS, cmd.startup());
    }

    @Test
    void testStaticLoad() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        var maybeCmd = Command.loadCommand(DATA_1_ARRAY);
        Assertions.assertTrue(maybeCmd.isPresent());
        var cmd = maybeCmd.get();
        Assertions.assertEquals(JoinCommand.class, cmd.getClass());
        var joinCmd = (JoinCommand) cmd;
        Assertions.assertEquals(DATA_1_PID, joinCmd.pid());
        Assertions.assertEquals(DATA_1_ID, joinCmd.id());
        Assertions.assertEquals(DATA_1_TS, joinCmd.startup());
    }
}
