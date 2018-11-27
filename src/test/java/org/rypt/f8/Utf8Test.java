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
        byte[] bytes = new byte[0];
        testStates(bytes);
        for (int b0 = 0; b0 < 256; b0++) {
            bytes = new byte[]{(byte)b0};
            testStates(bytes);
            for (int b1 = 0; b1 < 256; b1++) {
                bytes = new byte[]{(byte)b0, (byte)b1};
                testStates(bytes);
                for (int b2 = 0; b2 < 256; b2++) {
                    bytes = new byte[]{(byte)b0, (byte)b1, (byte)b2};
                    testStates(bytes);
                    for (int b = 0xf0; b < 0xf5; b++) {
                        bytes = new byte[]{(byte)b, (byte)b0, (byte)b1, (byte)b2};
                        testStates(bytes);
                    }
                }
            }
        }
    }

    @Test
    public void testStrings() {
        Utf8StringBuilder sb = new Utf8StringBuilder();
        byte[] bytes = new byte[0];
        testStrings(bytes, sb);
        for (int b0 = 0; b0 < 256; b0++) {
            bytes = new byte[]{(byte)b0};
            testStrings(bytes, sb);
            for (int b1 = 0; b1 < 256; b1++) {
                bytes = new byte[]{(byte)b0, (byte)b1};
                testStrings(bytes, sb);
                for (int b2 = 0; b2 < 256; b2++) {
                    bytes = new byte[]{(byte)b0, (byte)b1, (byte)b2};
                    testStrings(bytes, sb);
                    for (int b = 0xf0; b < 0xf5; b++) {
                        bytes = new byte[]{(byte)b, (byte)b0, (byte)b1, (byte)b2};
                        testStrings(bytes, sb);
                    }
                }
            }
        }
    }

    @Test
    public void testRandom() {
        Utf8StringBuilder testSb = new Utf8StringBuilder();
        for (int shift = 0; shift < 13; shift++) {
            for (int test = 0; test < 1_000_000; test++) {
                int codePointCount = (int) (Math.random() * 16 + 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < codePointCount; i++) {
                    sb.appendCodePoint((int) (Math.random() * (0x110000 >> shift)));
                }
                byte[] bytes = sb.toString().getBytes(UTF_8);
                testStrings(bytes, testSb);
                int a = (int) (Math.random() * bytes.length);
                int b = (int) (Math.random() * bytes.length);
                byte[] subbytes = new byte[Math.abs(b - a) + 1];
                System.arraycopy(bytes, Math.min(a, b), subbytes, 0, subbytes.length);
                testStrings(subbytes, testSb);
            }
        }
    }

    private static boolean assertState(int state) {
        if (state >= 0) {
            assertTrue(Character.isValidCodePoint(state));
            assertTrue(state < Character.MIN_SURROGATE || state > Character.MAX_SURROGATE);
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
        int cutLen = (int)(Math.random() * bytes.length);
        sb.write(bytes, 0, cutLen);
        sb.write(bytes, cutLen, bytes.length - cutLen);
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

    @Test
    public void testCodePointConstruction() {
        for (int i = 0; i <= Character.MAX_CODE_POINT; i++) {
            byte[] b = new String(Character.toChars(i)).getBytes(UTF_8);
            if (b.length == 2) {
                assertEquals(i, Utf8.codePoint(b[0], b[1]));
            } else if (b.length == 3) {
                assertEquals(i, Utf8.codePoint(b[0], b[1], b[2]));
            } else if (b.length == 4) {
                assertEquals(i, Utf8.codePoint(b[0], b[1], b[2], b[3]));
            }
        }
    }

    @Test
    public void testInitialState() {
        for (int b = 0; b < 256; b++) {
            assertEquals(Utf8.nextState(0, (byte)b), Utf8.initialState((byte)b));
        }
    }

}
