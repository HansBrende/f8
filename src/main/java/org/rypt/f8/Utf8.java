package org.rypt.f8;

public class Utf8 {

    private Utf8() { throw new AssertionError(); }

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
    public static int nextState(int s, byte b) {
        return s << 6 & (s << 1 | s << 2 | s << 3) >> 31
                | b & ~(b >> 1 & b >> 2 & b >> 3 & 0xfffffff0)
                | state[b & 0xff] << (s << 1 >>> 28 << 1) & 0xf4000000;
    }

    /**
     * Returns the next UTF-8 state state given a previous state, a next byte, and a code point handler.
     * This function may call {@link #nextState(int, byte)} twice if the first invocation
     * returns an error state, in which case only the second state will be returned from this method.
     * @param s the previous UTF-8 state returned from this function, or 0 for the initial state
     * @param b the next byte
     * @param handler the handler to delegate all code point and error handling to
     * @return the next UTF-8 state
     */
    public static int nextState(int s, byte b, Utf8Handler handler) {
        int state = nextState(s, b);
        if (state >= 0) {
            handler.handleCodePoint(state);
        } else if (isErrorState(state) &&
                // Report an error only if this is an error state that doesn't correspond to a
                // surrogate code point (as we have already reported an error in that case).
                // This will ensure the number of '�' characters we print out will exactly
                // match StandardCharsets.UTF_8's CharsetDecoder implementation.
                !(isSurrogatePrefixErrorState(s) && b < (byte)0xC0)) {

            handler.handleError();

            //If previous state was incomplete and error doesn't correspond to a
            // surrogate code point prefix, restart state machine at this byte
            if (isIncompleteState(s) && !isSurrogatePrefixErrorState(state)) {
                state = nextState(0, b);
                if (state >= 0) {
                    handler.handleCodePoint(state);
                } else if (isErrorState(state)) {
                    handler.handleError();
                }
            }
        }
        return state;
    }

    /**
     * Tests if at least one more byte is needed to create a code point
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
