package org.rypt.f8;

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
}
