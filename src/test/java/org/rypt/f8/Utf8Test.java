package org.rypt.f8;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        Sem.testAllCombinations(sems -> testStates(sems.generate()));
    }

    @Test
    public void testStrings() {
        Sem.testAllCombinations(sems -> testStrings(sems.generate(), new TestStringBuilder()));
    }

    @Test
    public void testIsValid() {
        Sem.testAllCombinations(sems -> {
            byte[] bytes = sems.generate();
            boolean valid = Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8));
            assertEquals(valid, Utf8.isFullyValid(new ByteArrayInputStream(bytes)));
            Utf8Statistics stats = new Utf8Statistics();
            Utf8.transferAndFinish(new ByteArrayInputStream(bytes), stats);
            assertEquals(valid, stats.countInvalid() == 0);
            assertEquals(stats.countInvalidIgnoringTruncation() == 0,
                    Utf8.isValidUpToTruncation(new ByteArrayInputStream(bytes)));
        });
    }

    @Test
    public void testAppendable() {
        Sem.testAllCombinations(sems -> {
            byte[] bytes = sems.generate();
            StringBuilder appendable = new StringBuilder();
            Utf8.transferAndFinish(new ByteArrayInputStream(bytes), Utf8Handler.of(appendable));
            assertEquals(new String(bytes, UTF_8), appendable.toString());
        });
    }

    @Test
    public void testRandom() throws IOException {
        TestStringBuilder testSb = new TestStringBuilder();
        for (int shift = 0; shift < 13; shift++) {
            for (int test = 0; test < 100000; test++) {
                int codePointCount = (int) (Math.random() * 16 + 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < codePointCount; i++) {
                    sb.appendCodePoint((int) (Math.random() * (0x110000 >> shift)));
                }
                byte[] bytes = sb.toString().getBytes(UTF_8);
                testStrings(bytes, testSb);
                int a = (int) (Math.random() * bytes.length);
                int b = (int) (Math.random() * bytes.length);
                testStrings(Arrays.copyOfRange(bytes, Math.min(a, b), Math.max(a, b) + 1), testSb);
                testIsValidAllowingTruncation(true, Arrays.copyOf(bytes, a));

                boolean valid;
                do {
                    bytes[a] = (byte) (Math.random() * 256);
                    valid = Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8));
                } while (valid);
                testStrings(bytes, testSb);
                if (a < bytes.length - 3) {
                    testIsValidAllowingTruncation(false, bytes);
                }
            }
        }
    }

    public static void testIsValidAllowingTruncation(boolean expected, byte[] bytes) throws IOException {
        assertEquals(expected, Utf8.isValidUpToTruncation(new ByteArrayInputStream(bytes)));
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

    private static class TestStringBuilder extends Utf8StringBuilder {

        private int index;
        byte[] bytes;

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
            this.index = 0;
        }

        private void assertByte(int b) {
            assertEquals(bytes[index++], b);
        }

        @Override
        public void close() {
            super.close();
            assertEquals(bytes.length, index);
        }

        @Override
        public void handleCodePoint(int codePoint) {
            super.handleCodePoint(codePoint);
            throw new AssertionError();
        }

        @Override
        public void handleIgnoredByte(int b1) {
            super.handleIgnoredByte(b1);
            assertByte(b1);
        }

        @Override
        public void handlePrefixError(int b1) {
            super.handlePrefixError(b1);
            assertByte(b1);
        }

        @Override
        public void handleContinuationError(int b1, int nextByte) {
            super.handleContinuationError(b1, nextByte);
            assertByte(b1);
        }

        @Override
        public void handleContinuationError(int b1, int b2, int nextByte) {
            super.handleContinuationError(b1, b2, nextByte);
            assertByte(b1);
            assertByte(b2);
        }

        @Override
        public void handleContinuationError(int b1, int b2, int b3, int nextByte) {
            super.handleContinuationError(b1, b2, b3, nextByte);
            assertByte(b1);
            assertByte(b2);
            assertByte(b3);
        }

        @Override
        public void handle1ByteCodePoint(int ascii) {
            super.handle1ByteCodePoint(ascii);
            assertByte(ascii);
        }

        @Override
        public void handle2ByteCodePoint(int b1, int b2) {
            super.handle2ByteCodePoint(b1, b2);
            assertByte(b1);
            assertByte(b2);
        }

        @Override
        public void handle3ByteCodePoint(int b1, int b2, int b3) {
            super.handle3ByteCodePoint(b1, b2, b3);
            assertByte(b1);
            assertByte(b2);
            assertByte(b3);
        }

        @Override
        public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
            super.handle4ByteCodePoint(b1, b2, b3, b4);
            assertByte(b1);
            assertByte(b2);
            assertByte(b3);
            assertByte(b4);
        }
    }

    private static void testStrings(byte[] bytes, TestStringBuilder sb) {
        String utf8 = new String(bytes, UTF_8);
        boolean validExpected = Arrays.equals(bytes, utf8.getBytes(UTF_8));

        sb.reset();
        sb.setBytes(bytes);
        int cutLen = (int)(Math.random() * bytes.length);
        sb.write(bytes, 0, cutLen);
        sb.write(bytes, cutLen, bytes.length - cutLen);
        sb.close();
        String utf8Actual = sb.toString();
        boolean validActual = sb.countInvalid() == 0;

        assertEquals(validExpected, validActual);
        assertEquals(utf8, utf8Actual);

        int state = 0;
        Utf8StringBuilder sbNew = new Utf8StringBuilder();
        for (byte b : bytes) {
            state = Utf8.nextState(state, b, sbNew);
        }
        if (Utf8.isIncompleteState(state)) {
            sbNew.handleError();
        }

        String utf82Actual = sbNew.toString();
        boolean valid2Actual = sbNew.countInvalid() == 0;
        assertEquals(validExpected, valid2Actual);
        assertEquals(utf8, utf82Actual);
    }

    public static String toString(int[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.format("%02X", a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    public static String toString(byte[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.format("%02X", a[i] & 0xff));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
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

}
