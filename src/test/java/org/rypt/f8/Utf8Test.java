package org.rypt.f8;

import org.junit.Test;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Utf8Test {

    @Test
    public void testStates() {
        Utf8StringBuilder sb = new Utf8StringBuilder();
        byte[] bytes = new byte[0];
        testStates(bytes);
        testStrings(bytes, sb);
        for (int b0 = 0; b0 < 256; b0++) {
            bytes = new byte[]{(byte)b0};
            testStates(bytes);
            testStrings(bytes, sb);
            for (int b1 = 0; b1 < 256; b1++) {
                bytes = new byte[]{(byte)b0, (byte)b1};
                testStates(bytes);
                testStrings(bytes, sb);
                for (int b2 = 0; b2 < 256; b2++) {
                    bytes = new byte[]{(byte)b0, (byte)b1, (byte)b2};
                    testStates(bytes);
                    testStrings(bytes, sb);
                    for (int b = 0xf0; b < 0xf5; b++) {
                        bytes = new byte[]{(byte)b, (byte)b0, (byte)b1, (byte)b2};
                        testStates(bytes);
                        testStrings(bytes, sb);
                    }
                }
            }
        }
    }

    private static boolean assertState(int state) {
        if (state >= 0) {
            assertTrue(Character.isValidCodePoint(state));
            assertNotEquals(Character.SURROGATE, Character.getType(state));
            assertFalse(Utf8.isIncompleteState(state));
            assertFalse(Utf8.isErrorState(state));
            for (int b = 0; b < 256; b++) {
                assertEquals(Utf8.nextState(0, (byte)b), Utf8.nextState(state, (byte)b));
            }
            return true;
        } else if (Utf8.isErrorState(state)) {
            assertFalse(Utf8.isIncompleteState(state));
            for (int b = 0; b < 256; b++) {
                assertEquals(Utf8.nextState(0, (byte)b), Utf8.nextState(state, (byte)b));
            }
            return true;
        } else {
            assertTrue(Utf8.isIncompleteState(state));
            return false;
        }
    }

    @Test
    public void testStatesV2() {
        for (int b0 = 0; b0 < 256; b0++) {
            int state0 = Utf8.nextState(0, (byte) b0);
            if (assertState(state0)) {
                continue;
            }
            for (int b1 = 0; b1 < 256; b1++) {
                int state1 = Utf8.nextState(state0, (byte) b1);
                if (assertState(state1)) {
                    continue;
                }
                for (int b2 = 0; b2 < 256; b2++) {
                    int state2 = Utf8.nextState(state1, (byte) b2);
                    if (assertState(state2)) {
                        continue;
                    }
                    for (int b3 = 0; b3 < 256; b3++) {
                        int state3 = Utf8.nextState(state2, (byte) b3);
                        if (assertState(state3)) {
                            continue;
                        }
                        fail();
                    }
                }
            }
        }
    }

    @Test
    public void testSurrogatePrefixes() {
        assertTrue(Utf8.isIncompleteState(Utf8.nextState(0, (byte)0xED)));
        for (int b = 0xA0; b < 0xC0; b++) {
            assertTrue(Utf8.isSurrogatePrefixErrorState(Utf8.nextState(Utf8.nextState(0, (byte)0xED), (byte)b)));
        }

        for (int b0 = 0; b0 < 256; b0++) {
            int state0 = Utf8.nextState(0, (byte)b0);
            if (Utf8.isSurrogatePrefixErrorState(state0)) {
                fail();
            } else if (state0 >= 0 || Utf8.isErrorState(state0)) {
                continue;
            }

            for (int b1 = 0; b1 < 256; b1++) {
                int state1 = Utf8.nextState(state0, (byte)b1);
                if (Utf8.isSurrogatePrefixErrorState(state1)) {
                    assertEquals(0xED, b0);
                    assertTrue(b1 >= 0xA0 && b1 < 0xC0);
                    continue;
                } else if (state1 >= 0 || Utf8.isErrorState(state1)) {
                    continue;
                }

                for (int b2 = 0; b2 < 256; b2++) {
                    int state2 = Utf8.nextState(state1, (byte)b2);
                    if (Utf8.isSurrogatePrefixErrorState(state2)) {
                        fail();
                    } else if (state2 >= 0 || Utf8.isErrorState(state2)) {
                        continue;
                    }
                    for (int b3 = 0; b3 < 256; b3++) {
                        int state3 = Utf8.nextState(state2, (byte)b3);
                        if (Utf8.isSurrogatePrefixErrorState(state3)) {
                            fail();
                        }
                    }
                }
            }
        }
    }

    private static void testStrings(byte[] bytes, Utf8StringBuilder sb) {
        String utf8 = new String(bytes, UTF_8);
        boolean validExpected = Arrays.equals(bytes, utf8.getBytes(UTF_8));

        sb.reset();
        sb.write(bytes);
        sb.close();
        String utf8Actual = sb.toString();
        boolean validActual = sb.countInvalid() == 0;

        assertEquals(validExpected, validActual);
        assertEquals(utf8, utf8Actual);
    }

    private static void testStates(byte[] bytes) {
        String utf8 = new String(bytes, UTF_8);
        boolean isValid = Arrays.equals(bytes, utf8.getBytes(UTF_8));
        int state = 0;
        boolean isValidTest = true;
        int numIncompleteStates = 0;
        for (byte b : bytes) {
            boolean prevWasError = Utf8.isErrorState(state);
            boolean prevWasDone = state >= 0;
            state = Utf8.nextState(state, b);

            if (Utf8.isErrorState(state)) {
                isValidTest = false;
                assertFalse(state >= 0);
                assertFalse(Utf8.isIncompleteState(state));
                numIncompleteStates = 0;
            } else if (state >= 0) {
                assertFalse(Utf8.isIncompleteState(state));
                numIncompleteStates = 0;
            } else {
                assertTrue(Utf8.isIncompleteState(state));
                numIncompleteStates++;
                assertNotEquals(4, numIncompleteStates);
            }

            if (prevWasDone || prevWasError) {
                assertEquals(Utf8.nextState(0, b), state);
            }

        }
        if (Utf8.isIncompleteState(state)) {
            isValidTest = false;
        }

        assertEquals(isValid, isValidTest);
    }

}
