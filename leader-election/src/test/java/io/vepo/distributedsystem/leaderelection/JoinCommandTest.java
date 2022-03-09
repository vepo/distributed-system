package io.vepo.distributedsystem.leaderelection;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JoinCommandTest {
    private static long DATA_1_PID = 123456789L;
    private static String DATA_1_ID = "012345";
    private static byte[] DATA_1_ARRAY = new byte[] {
            JoinCommand.COMMAND_ID,
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            (byte) DATA_1_PID,
            (byte) (DATA_1_PID >> 8),
            (byte) (DATA_1_PID >> 16),
            (byte) (DATA_1_PID >> 24),
            (byte) (DATA_1_PID >> 32),
            (byte) (DATA_1_PID >> 40),
            (byte) (DATA_1_PID >> 48),
            (byte) (DATA_1_PID >> 56) };

    @Test
    void testGetBytes() {
        Assertions.assertArrayEquals(DATA_1_ARRAY, new JoinCommand(DATA_1_ID, DATA_1_PID).getBytes());
    }

    @Test
    void testLoad() {
        var cmd = new JoinCommand();
        cmd.load(DATA_1_ARRAY);
        Assertions.assertEquals(DATA_1_PID, cmd.pid());
        Assertions.assertEquals(DATA_1_ID, cmd.id());
    }

    @Test
    void testStaticLoad() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        var maybeCmd = Command.loadCommand(DATA_1_ARRAY);
        Assertions.assertTrue(maybeCmd.isPresent());
        var cmd = maybeCmd.get();
        Assertions.assertEquals(JoinCommand.class, cmd.getClass());
        var joinCmd = (JoinCommand) cmd;
        Assertions.assertEquals(DATA_1_PID, joinCmd.pid());
        Assertions.assertEquals(DATA_1_ID, joinCmd.id());
    }
}
