package org.rypt.f8;

/**
 * The core UTF-8 state machine.
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8 {

    private Utf8() { throw new AssertionError(); }

//    public static void main(String[] args) {
//
//        TreeMap<String, TreeMap<Charset, Integer>> values = new TreeMap<>();
//
//        for (Charset charset : Charset.availableCharsets().values()) {
//            if (charset.name().startsWith("x-") || charset.name().startsWith("X-") || !charset.canEncode()) {
//                continue;
//            }
//            for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
//                int type = Character.getType(codePoint);
//                if (type == Character.CONTROL || type == Character.UNASSIGNED || type == Character.PRIVATE_USE || type == Character.SURROGATE) {
//                    continue;
//                }
//                String str = new String(Character.toChars(codePoint));
//                byte[] bytes = str.getBytes(charset);
//                if (!str.equals(new String(bytes, charset))) { //invalid code point for charset
//                    continue;
//                }
//                for (byte b : bytes) {
//                    if (b >= 0 && Character.isISOControl(b)) {
//                        values.computeIfAbsent(String.format("0x%02X", b), k -> new TreeMap<>()).merge(charset, 1, (x, y) -> x + y);
//                    }
//                }
//            }
//        }
//
//        values.forEach((desc, map) -> System.out.println(desc + " is used in " + map.entrySet().stream().mapToInt(Map.Entry::getValue).sum()
//                + " code points across charsets: " + map.entrySet().stream().sorted((e0, e1) -> Integer.compare(e1.getValue(), e0.getValue()))
//                .map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.toList())));
//
//    }

    private static final int[] state = {
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888, 0x08888888,
            0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80,
            0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80, 0x8c8cff80,
            0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80,
            0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80, 0x8cc8ff80,
            0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0,
            0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0,
            0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0,
            0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0, 0x8cc8f8f0,
            0x88888888, 0x88888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888,
            0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888,
            0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888,
            0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888, 0xf8888888,
            0xe8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888,
            0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xc8888888, 0xd8888888, 0xc8888888, 0xc8888888,
            0xa8888888, 0x98888888, 0x98888888, 0x98888888, 0xb8888888, 0x88888888, 0x88888888, 0x88888888,
            0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888, 0x88888888
    };

    /**
     * Returns the next UTF-8 state given a previous state and a next byte.
     * If the returned state is non-negative, it is a legal non-surrogate unicode code point.
     * Otherwise, it is either an error state or an incomplete state.
     * @param s the previous UTF-8 state returned from this function, or 0 for the initial state
     * @param b the next byte
     * @return the next UTF-8 state
     * @see Utf8#isErrorState(int)
     * @see Utf8#isIncompleteState(int)
     */
    public static int nextState(int s, int b) {
        return s << 6 & (s << 1 | s << 2 | s << 3) >> 31
                | b & ~(b >> 1 & b >> 2 & b >> 3 & 0xfffffff0)
                | state[b & 0xff] << (s >> 26 & 0x1c) & 0xf4000000;
    }

    /**
     * Returns the next UTF-8 state given no previous initial state and a next byte.
     * This method is semantically equivalent to: {@code nextState(0, b)}.
     * @param b the next byte
     * @return the next UTF-8 state
     * @see Utf8#nextState(int, byte)
     */
    public static int initialState(byte b) {
        return b & ~(b >> 1 & b >> 2 & b >> 3 & 0xfffffff0) | state[b & 0xff] & 0xf4000000;
    }

    /**
     * Returns the next UTF-8 state given a previous state, a next byte, and a code point handler.
     * This function may call {@link #nextState(int, byte)} twice if the first invocation
     * returns an error state, in which case only the second state will be returned from this method.
     * @param s the previous UTF-8 state returned from this function, or 0 for the initial state
     * @param b the next byte
     * @param handler the handler to delegate all code point and error handling to
     * @return the next UTF-8 state
     */
    public static int nextState(int s, byte b, Utf8Handler handler) {
        int next = nextState(s, b);
        if (next >= 0) {
            handler.handleCodePoint(next);
        } else if (isErrorState(next) &&
                // Report an error only if this is an error state that doesn't correspond to a
                // surrogate code point (as we have already reported an error in that case).
                // This will ensure the number of '�' characters we print out will exactly
                // match StandardCharsets.UTF_8's CharsetDecoder implementation.
                !(isSurrogatePrefixErrorState(s) && b < (byte)0xC0)) {

            handler.handleError();

            //If previous state was incomplete and error doesn't correspond to a
            // surrogate code point prefix, restart state machine at this byte
            if (isIncompleteState(s) && !isSurrogatePrefixErrorState(next)) {
                next = nextState(0, b);
                if (next >= 0) {
                    handler.handleCodePoint(next);
                } else if (isErrorState(next)) {
                    handler.handleError();
                }
            }
        }
        return next;
    }

    public static int nextState(int state, byte[] b, int off, int len, Utf8Handler handler) {
        final int to = off + len;
        while (state < 0 && off < to) {
            state = nextState(state, b[off++], handler);
        }

        if (state < 0) {
            return state;
        }

        for (;;) {
            int b1 = 0;
            while (off < to && (b1 = b[off++]) >= 0) {
                handler.handleAscii(b1);
            }
            if (b1 >= 0) { //0xxxxxxx
                return b1;
            } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                if (off < to) { //110xxxxx 10xxxxxx
                    int b2 = b[off++];
                    if ((b2 & 0xc0) != 0x80) { //is not continuation
                        handler.handleError();
                        off--;
                    } else {
                        handler.handle2ByteCodePoint(b1, b2);
                    }
                } else { //110xxxxx
                    return initialState((byte)b1);
                }
            } else if ((b1 >> 4) == -2) {
                if (off + 1 < to) { //1110xxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3;
                    if (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80 || (b2 & 0xc0) != 0x80 || (b3 = b[off++]) > (byte)0xbf) {
                        handler.handleError();
                        off--;
                    } else if (b1 == (byte)0xed && (b2 & 0xe0) == 0xa0) { //surrogate
                        handler.handleError();
                    } else {
                        handler.handle3ByteCodePoint(b1, b2, b3);
                    }
                } else {
                    state = initialState((byte)b1);
                    if (off < to) {
                        state = nextState(state, b[off], handler);
                    }
                    return state;
                }
            } else if ((b1 >> 3) == -2) {
                if (off + 2 < to) { //11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3, b4;
                    // Dark magic
                    if ((b2 >> 6 ^ -2 | (b1 << 28) + 0x70 + b2 >> 30) != 0
                            // if (b1 > (byte)0xf4
                            //      || b2 > (byte)0xbf
                            //      || b1 == (byte)0xf0 && b2 < (byte)0x90
                            //      || b1 == (byte)0xf4 && b2 > (byte)0x8f
                            || (b3 = b[off++]) > (byte)0xbf
                            || (b4 = b[off++]) > (byte)0xbf) {
                        handler.handleError();
                        off--;
                    } else {
                        handler.handle4ByteCodePoint(b1, b2, b3, b4);
                    }
                } else if (b1 > (byte)0xf4) {
                    handler.handleError();
                } else {
                    state = initialState((byte)b1);
                    while (off < to) {
                        state = nextState(state, b[off++], handler);
                    }
                    return state;
                }
            } else {
                handler.handleError();
            }
        }
    }

    /**
     * Returns a valid unicode code point given a valid UTF-8 2-byte sequence. The maximum code point
     * returned by this method is U+07FF. <b>Results for invalid 2-byte sequences are undefined.</b>
     * @param b1 a negative byte of the form {@code (byte)0b110xxxxx}
     * @param b2 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @return the code point corresponding to this valid UTF-8 2-byte sequence
     */
    public static int codePoint(int b1, int b2) {
        return b1 << 6 ^ b2 ^ 0xf80;
    }

    /**
     * Returns a valid unicode code point given a valid UTF-8 3-byte sequence. The maximum code point
     * returned by this method is U+FFFF. <b>Results for invalid 3-byte sequences are undefined.</b>
     * @param b1 a negative byte of the form {@code (byte)0b1110xxxx}
     * @param b2 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @param b3 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @return the code point corresponding to this valid UTF-8 3-byte sequence
     */
    public static int codePoint(int b1, int b2, int b3) {
        return b1 << 12 ^ b2 << 6 ^ b3 ^ 0xfffe1f80;
    }

    /**
     * Returns a valid unicode code point given a valid UTF-8 4-byte sequence. The maximum code point
     * returned by this method is U+10FFFF. <b>Results for invalid 4-byte sequences are undefined.</b>
     * @param b1 a negative byte of the form {@code (byte)0b11110xxx}
     * @param b2 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @param b3 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @param b4 a negative byte of the form {@code (byte)0b10xxxxxx}
     * @return the code point corresponding to this valid UTF-8 4-byte sequence
     */
    public static int codePoint(int b1, int b2, int b3, int b4) {
        return b1 << 18 ^ b2 << 12 ^ b3 << 6 ^ b4 ^ 0x381f80;
    }

    /**
     * Tests if at least one more continuation byte is needed to create a code point
     * @param s the state to test
     * @return true if at least one more byte is needed to create a code point
     */
    public static boolean isIncompleteState(int s) {
        return (s << 1 | s << 2 | s << 3) >> 31 == -1;
    }

    /**
     * Tests if this state corresponds to any invalid UTF-8 byte sequence.
     * @param s the state to test
     * @return true if this state corresponds to an invalid UTF-8 byte sequence.
     */
    public static boolean isErrorState(int s) {
        return (s & 0xf0000000) == 0x80000000;
    }

    /**
     * Tests if this state corresponds to an invalid UTF-8 2-byte sequence that prefixes
     * a surrogate code point, i.e., in the range 0xED 0xA0 to 0xED 0xBF.
     * @param s the state to test
     * @return true if this state corresponds to an invalid surrogate code point prefix
     */
    public static boolean isSurrogatePrefixErrorState(int s) {
        return (s & 0xffffffe0) == 0x84000360;
    }

}
