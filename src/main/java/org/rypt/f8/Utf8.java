package org.rypt.f8;

import java.io.IOException;
import java.io.InputStream;

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

    private static final int SURROGATE_PREFIX = -1;
    private static final int OTHER_ERROR = -2;

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
    public static int nextState(int s, byte b) {
        int z = s + 2;
        if ((z & (b + 64)) >= 0) {
            if ((z | b & (b - (byte)0xc2 | ((byte)0xf4) - b)) >= 0) {
                return b;
            }
            return OTHER_ERROR;
        } else if (s >> 5 == -2) {
            return codePoint(s, b);
        } else if (s >= 0xfffff000) {
            if ((s & 0xfb) == 0xf0 && ((s << 28) + 0x70 + b >> 30) != 0
                    || s == (byte)0xe0 && b < (byte)0xa0) {
                return OTHER_ERROR;
            }
            int c = s << 8 | b & 0xff;
            return c >> 5 == 0xffffff6d ? SURROGATE_PREFIX : c;
        } else if (s >> 12 == -2) {
            return codePoint(s >> 8, (byte)s, b);
        } else {
            return codePoint(s >> 16, (byte)(s >> 8), (byte)s, b);
        }
    }

    private static <X extends Exception> void transferState(int s, int nextByte, Utf8ByteHandler<X> handler) throws X {
        if (isIncompleteState(s)) { //missing continuation
            if (s >= 0xffffffc0) {
                handler.handleContinuationError(s, nextByte);
            } else if (s >= 0xffffe000) {
                handler.handleContinuationError(s >> 8, (byte)s, nextByte);
            } else {
                handler.handleContinuationError(s >> 16, (byte)(s >> 8), (byte)s, nextByte);
            }
        }
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
    public static <X extends Exception> int nextState(int s, byte b, Utf8ByteHandler<X> handler) throws X {
        if (((s + 2) & (b + 64)) >= 0) { //same as: s >= -2 || b >= (byte)0xc0
            transferState(s, b, handler);
            if (b >= 0) {
                handler.handle1ByteCodePoint(b);
                return 0;
            } else if ((-63 - b & b + 11) < 0) { //same as: b >= 0xc2 && b <= 0xf4
                return b;
            } else if (isSurrogatePrefixErrorState(s) && b < (byte)0xc0) {
                handler.handleIgnoredByte(b);
                return 0;
            } else {
                handler.handlePrefixError(b);
                return 0;
            }
        } else if (s >> 5 == -2) {
            handler.handle2ByteCodePoint(s, b);
            return 0;
        } else if (s >> 12 == -2) {
            handler.handle3ByteCodePoint(s >> 8, (byte)s, b);
            return 0;
        } else if (s >> 19 == -2) {
            handler.handle4ByteCodePoint(s >> 16, (byte)(s >> 8), (byte)s, b);
            return 0;
        } else if ((s & 0xfb) == 0xf0 && ((s << 28) + 0x70 + b >> 30) != 0
                || s == (byte)0xe0 && b < (byte)0xa0) {
            handler.handleContinuationError(s, b); //missing continuation
            handler.handlePrefixError(b); //invalid first byte
            return 0;
        } else {
            int c = s << 8 | b & 0xff;
            if (c >> 5 == 0xffffff6d) {
                handler.handleContinuationError((byte)0xed, b);
                handler.handleIgnoredByte(b);
                return SURROGATE_PREFIX;
            } else {
                return c;
            }
        }
    }

    public static <X extends Exception> int nextState(int state, byte[] b, int off, int len, Utf8ByteHandler<X> handler) throws X {
        final int to = off + len;
        while ((state & (off - to)) < 0) {
            state = nextState(state, b[off++], handler);
        }

        if (state < 0) {
            return state;
        }

        for (;;) {
            int b1 = 0;
            while (off < to && (b1 = b[off++]) >= 0) {
                handler.handle1ByteCodePoint(b1);
            }
            if (b1 >= 0) { //0xxxxxxx
                return 0;
            } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                if (off < to) { //110xxxxx 10xxxxxx
                    int b2 = b[off++];
                    if ((b2 & 0xc0) != 0x80) { //is not continuation
                        handler.handleContinuationError(b1, b2);
                        off--;
                    } else {
                        handler.handle2ByteCodePoint(b1, b2);
                    }
                } else { //110xxxxx
                    return b1;
                }
            } else if ((b1 >> 4) == -2) {
                if (off + 1 < to) { //1110xxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3;
                    if (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80
                            || (b2 & 0xc0) != 0x80) {
                        handler.handleContinuationError(b1, b2);
                        off--;
                    } else if ((b3 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3);
                        off--;
                    } else if (b1 == (byte)0xed && (b2 & 0xe0) == 0xa0) { //surrogate
                        handler.handleContinuationError(b1, b2);
                        handler.handleIgnoredByte(b2);
                        handler.handleIgnoredByte(b3);
                    } else {
                        handler.handle3ByteCodePoint(b1, b2, b3);
                    }
                } else {
                    if (off < to) {
                        b1 = nextState(b1, b[off], handler);
                    }
                    return b1;
                }
            } else if ((b1 >> 3) == -2) {
                if (off + 2 < to) { //11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3, b4;
                    if ((b2 >> 6 ^ -2 | (b1 << 28) + 0x70 + b2 >> 30) != 0) {
                        handler.handleContinuationError(b1, b2);
                        off--;
                    } else if ((b3 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3);
                        off--;
                    } else if ((b4 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3, b4);
                        off--;
                    } else {
                        handler.handle4ByteCodePoint(b1, b2, b3, b4);
                    }
                } else if (b1 > (byte)0xf4) {
                    handler.handlePrefixError(b1);
                } else {
                    while (off < to) {
                        b1 = nextState(b1, b[off++], handler);
                    }
                    return b1;
                }
            } else {
                handler.handlePrefixError(b1);
            }
        }
    }

    /**
     * If the final state is incomplete, allows handler to handle final error state
     * @param finalState the final state
     * @param handler the handler
     */
    public static <X extends Exception> void finish(int finalState, Utf8ByteHandler<X> handler) throws X {
        transferState(finalState, Utf8ByteHandler.END_OF_STREAM, handler);
    }

    private static final int BUFFER_SIZE = 8192;
    public static <X extends Exception> void transfer(InputStream inputStream, Utf8ByteHandler<X> handler) throws IOException, X {
        int state = 0;
        byte[] bytes = new byte[BUFFER_SIZE];
        int n;
        while ((n = inputStream.read(bytes, 0, BUFFER_SIZE)) != -1) {
            state = nextState(state, bytes, 0, n, handler);
        }
        finish(state, handler);
    }


    public static boolean isFullyValid(InputStream is) throws IOException {
        try {
            transfer(is, Validator.strict);
            return true;
        } catch (Utf8Error e) {
            return false;
        }
    }

    public static boolean isValidUpToTruncation(InputStream is) throws IOException {
        try {
            transfer(is, Validator.allowingTruncation);
            return true;
        } catch (Utf8Error e) {
            return false;
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
        return s < OTHER_ERROR;
    }

    /**
     * Tests if this state corresponds to any invalid UTF-8 byte sequence.
     * @param s the state to test
     * @return true if this state corresponds to an invalid UTF-8 byte sequence.
     */
    public static boolean isErrorState(int s) {
        return s >> 1 == -1;
    }

    /**
     * Tests if this state corresponds to an invalid UTF-8 2-byte sequence that prefixes
     * a surrogate code point, i.e., in the range 0xED 0xA0 to 0xED 0xBF.
     * @param s the state to test
     * @return true if this state corresponds to an invalid surrogate code point prefix
     */
    public static boolean isSurrogatePrefixErrorState(int s) {
        return s == SURROGATE_PREFIX;
    }

    private static abstract class Validator implements Utf8ByteHandler<Utf8Error> {

        abstract void continuationError(int err) throws Utf8Error;

        private static final Validator strict = new Validator() {
            @Override
            void continuationError(int err) throws Utf8Error {
                Utf8Error.fail();
            }
        };

        private static final Validator allowingTruncation = new Validator() {
            @Override
            void continuationError(int err) throws Utf8Error {
                if (err != END_OF_STREAM) {
                    Utf8Error.fail();
                }
            }
        };

        @Override
        public void handle1ByteCodePoint(int b1) {
        }
        @Override
        public void handle2ByteCodePoint(int b1, int b2) {
        }
        @Override
        public void handle3ByteCodePoint(int b1, int b2, int b3) {
        }
        @Override
        public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
        }
        @Override
        public void handleContinuationError(int b1, int err) throws Utf8Error {
            continuationError(err);
        }
        @Override
        public void handleContinuationError(int b1, int b2, int err) throws Utf8Error {
            continuationError(err);
        }
        @Override
        public void handleContinuationError(int b1, int b2, int b3, int err) throws Utf8Error {
            continuationError(err);
        }
        @Override
        public void handlePrefixError(int err) throws Utf8Error {
            Utf8Error.fail();
        }
    }
}
