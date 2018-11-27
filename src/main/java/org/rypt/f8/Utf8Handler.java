package org.rypt.f8;

/**
 * Base interface for handling a UTF-8 encoded byte stream
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public interface Utf8Handler {
    /**
     * Called when a valid unicode code point is encountered
     * @param codePoint the code point that was encountered
     */
    void handleCodePoint(int codePoint);

    /**
     * Called when an invalid UTF-8 byte sequence is detected
     */
    void handleError();


    default void handleAscii(int ascii) {
        handleCodePoint(ascii);
    }

    default void handle2ByteCodePoint(int b1, int b2) {
        handleCodePoint(Utf8.codePoint(b1, b2));
    }

    default void handle3ByteCodePoint(int b1, int b2, int b3) {
        handleCodePoint(Utf8.codePoint(b1, b2, b3));
    }

    default void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
        handleCodePoint(Utf8.codePoint(b1, b2, b3, b4));
    }
}
