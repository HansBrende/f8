package org.rypt.f8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The core UTF-8 state machine.
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8 {

    private Utf8() { throw new AssertionError(); }

    static final int SURROGATE_PREFIX = -1;
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
     * @deprecated Use {@link #nextState(int, byte, Utf8ByteHandler)} instead.
     */
    @Deprecated
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
     * @param s the previous UTF-8 state returned from this function, or 0 for the initial state
     * @param b the next byte
     * @param handler the handler to delegate all code point and error handling to
     * @return the next UTF-8 state
     * @see Utf8#isIncompleteState(int)
     */
    public static <X extends Exception> int nextState(int s, byte b, Utf8ByteHandler<X> handler) throws X {
        if (s >= OTHER_ERROR || b >= (byte)0xc0) {
            transferState(s, b, handler);
            if (b >= 0) {
                handler.handle1ByteCodePoint(b);
                return 0;
            } else if (b >= (byte)0xc2 && b <= (byte)0xf4) {
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

    public static <X extends Exception> int nextState(int state, byte[] b, int from, int to, Utf8ByteHandler<X> handler) throws X {
        if (from >= to) {
            if (from > to) {
                throw new IllegalArgumentException(from + " > " + to);
            }
            return state;
        }

        if (state < 0) {
            byte n;
            do {
                state = nextState(state, n = b[from++], handler);
                if (from == to)
                    return state;
            } while (state != 0 && state != n);
            from += state >> 31;
        }

        do {
            int n = b[from++];
            if (n < 0)
                return __.state(b, from - 1, to, handler);
            handler.handle1ByteCodePoint(n);
        } while (from != to);

        return 0;
    }

    private static final int BUFFER_SIZE = 8192;
    private static final AtomicReference<byte[]> buf = new AtomicReference<>();

    public static <X extends Exception> int nextState(int state, InputStream inputStream, Utf8ByteHandler<X> handler) throws IOException, X {
        AtomicReference<byte[]> buf = Utf8.buf;
        byte[] bytes = buf.get();
        if (bytes == null || !buf.compareAndSet(bytes, null)) {
            bytes = new byte[BUFFER_SIZE];
        }
        int n;
        while ((n = inputStream.read(bytes, 0, BUFFER_SIZE)) != -1) {
            state = nextState(state, bytes, 0, n, handler);
        }
        buf.set(bytes);
        return state;
    }

    /**
     * If the final state is incomplete, allows handler to handle final error state
     * @param finalState the final state
     * @param handler the handler
     */
    public static <X extends Exception> void finish(int finalState, Utf8ByteHandler<X> handler) throws X {
        transferState(finalState, Utf8ByteHandler.END_OF_STREAM, handler);
    }

    /**
     * This method is semantically equivalent to:
     * <pre>{@code
     * int finalState = Utf8.nextState(0, is, handler);
     * Utf8.finish(finalState, handler);
     * }</pre>
     * @param is the input stream
     * @param handler the handler
     * @param <X> the handler exception type
     * @throws IOException if the input stream threw this exception
     * @throws X if the handler threw this exception
     */
    public static <X extends Exception> void transfer(InputStream is, Utf8ByteHandler<X> handler) throws IOException, X {
        finish(nextState(0, is, handler), handler);
    }

    public static Validity validity(InputStream is) throws IOException {
        AtomicReference<byte[]> buf = Utf8.buf;
        byte[] b = buf.get();
        if (b == null || !buf.compareAndSet(b, null)) {
            b = new byte[BUFFER_SIZE];
        }
        int n;
        while ((n = is.read(b, 0, BUFFER_SIZE)) >= 0) {
            for (int i = 0; i < n; i++) {
                if (b[i] < 0) {
                    for (Validity v = __.validity(b, i, n); v != Validity.MALFORMED; v = __.validity(b, 0, n)) {
                        int r = 0;
                        switch (v) {
                            case UNDERFLOW_R3: b[r++] = b[n - 3];
                            case UNDERFLOW_R2: b[r++] = b[n - 2];
                            case UNDERFLOW_R1: b[r++] = b[n - 1];
                        }
                        if ((n = is.read(b, r, BUFFER_SIZE - r)) < 0) {
                            buf.set(b);
                            return v;
                        }
                        n += r;
                    }
                    buf.set(b);
                    return Validity.MALFORMED;
                }
            }
        }
        buf.set(b);
        return Validity.ASCII;
    }

    /**
     * Returns the validity of the specified byte array between the specified indexes
     * @param b the byte array
     * @param from the start index
     * @param to the end index, exclusive
     * @return the validity
     */
    public static Validity validity(byte[] b, int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException(from + " > " + to);
        }

        for (int i = from; i < to; i++) {
            if (b[i] < 0) {
                return __.validity(b, i, to);
            }
        }
        return Validity.ASCII;
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

}
