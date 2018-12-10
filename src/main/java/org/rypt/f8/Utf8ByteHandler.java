package org.rypt.f8;

/**
 * Base interface for handling a UTF-8 encoded byte stream
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public interface Utf8ByteHandler<X extends Exception> {

    /**
     * A flag to indicate the absence of a trailing continuation byte
     * because the end of the stream has been reached, in
     * {@link #handleContinuationError(int, int)},
     * {@link #handleContinuationError(int, int, int)}, and
     * {@link #handleContinuationError(int, int, int, int)}
     */
    int END_OF_STREAM = -257;

    /**
     * Called when a valid 1-byte sequence is encountered.
     * @param b1 the valid ASCII character encountered, in the range 0-0x7F
     */
    void handle1ByteCodePoint(int b1) throws X;

    /**
     * Called when a valid 2-byte sequence is encountered.
     * @param b1 the prefix byte, in the range (byte)0xC2-(byte)0xDF
     * @param b2 the continuation byte, in the range (byte)0x80-(byte)0xBF
     * @see Utf8#codePoint(int, int)
     */
    void handle2ByteCodePoint(int b1, int b2) throws X;

    /**
     * Called when a valid 3-byte sequence is encountered.
     * @param b1 the prefix byte, in the range (byte)0xE0-(byte)0xEF
     * @param b2 the first continuation byte, in the range (byte)0x80-(byte)0xBF, unless
     *           the prefix byte is (byte)0xED or (byte)0xE0, in which case the range
     *           is (byte)0x80-(byte)0x9F or (byte)0xA0-(byte)0xBF, respectively
     * @param b3 the second continuation byte, in the range (byte)0x80-(byte)0xBF
     * @see Utf8#codePoint(int, int, int)
     */
    void handle3ByteCodePoint(int b1, int b2, int b3) throws X;

    /**
     * Called when a valid 4-byte sequence is encountered.
     * @param b1 the prefix byte, in the range (byte)0xF0-(byte)0xF4
     * @param b2 the first continuation byte, in the range (byte)0x80-(byte)0xBF, unless
     *           the prefix byte is (byte)0xF4 or (byte)0xF0, in which case the range
     *           is (byte)0x80-(byte)0x8F or (byte)0x90-(byte)0x9F, respectively
     * @param b3 the second continuation byte, in the range (byte)0x80-(byte)0xBF
     * @param b4 the third continuation byte, in the range (byte)0x80-(byte)0xBF
     * @see Utf8#codePoint(int, int, int, int)
     */
    void handle4ByteCodePoint(int b1, int b2, int b3, int b4) throws X;

    /**
     * Called when an invalid prefix byte is encountered.
     * @param err the invalid prefix byte, in the range (byte)0x80-(byte)0xC1 or (byte)0xF5-(byte)0xFF
     */
    void handlePrefixError(int err) throws X;

    /**
     * Called when an invalid continuation byte is encountered in a 2, 3, or 4-byte sequence.
     * @param b1 the prefix byte
     * @param err the invalid first continuation byte, or {@link #END_OF_STREAM}
     *           to indicate the end of the byte stream
     */
    void handleContinuationError(int b1, int err) throws X;

    /**
     * Called when an invalid continuation byte is encountered in a 3 or 4-byte sequence.
     * @param b1 the prefix byte
     * @param b2 the first continuation byte
     * @param err the invalid second continuation byte, or {@link #END_OF_STREAM}
     *            to indicate the end of the byte stream
     */
    void handleContinuationError(int b1, int b2, int err) throws X;

    /**
     * Called when an invalid continuation byte is encountered in a 4-byte sequence.
     * @param b1 the prefix byte
     * @param b2 the first continuation byte
     * @param b3 the second continuation byte
     * @param err the invalid third continuation byte, or {@link #END_OF_STREAM}
     *            to indicate the end of the byte stream
     */
    void handleContinuationError(int b1, int b2, int b3, int err) throws X;

    /**
     * Called for a trailing byte in an invalid multi-byte sequence for which the error
     * has already been handled, and thus, no action needs to be taken. This method is
     * called for the second and third bytes in a 3-byte sequence corresponding to a
     * surrogate code point.
     * @param b the ignored byte
     * @see Character#isSurrogate(char)
     */
    default void handleIgnoredByte(int b) throws X {
        //this event should be ignored in most cases
    }

}
