package org.rypt.f8;

import java.io.IOException;

/**
 * Base interface for handling a UTF-8 encoded byte stream
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public interface Utf8Handler<X extends Exception> extends Utf8ByteHandler<X> {

    /**
     * Called when a valid unicode code point is encountered
     * @param codePoint the code point that was encountered
     */
    void handleCodePoint(int codePoint) throws X;

    /**
     * Called when an invalid UTF-8 byte sequence is detected
     */
    void handleError() throws X;


    @Override
    default void handle1ByteCodePoint(int ascii) throws X {
        handleCodePoint(ascii);
    }

    @Override
    default void handle2ByteCodePoint(int b1, int b2) throws X {
        handleCodePoint(Utf8.codePoint(b1, b2));
    }

    @Override
    default void handle3ByteCodePoint(int b1, int b2, int b3) throws X {
        handleCodePoint(Utf8.codePoint(b1, b2, b3));
    }

    @Override
    default void handle4ByteCodePoint(int b1, int b2, int b3, int b4) throws X {
        handleCodePoint(Utf8.codePoint(b1, b2, b3, b4));
    }

    @Override
    default void handleContinuationError(int b1, int nextByte) throws X {
        handleError();
    }

    @Override
    default void handleContinuationError(int b1, int b2, int nextByte) throws X {
        handleError();
    }

    @Override
    default void handleContinuationError(int b1, int b2, int b3, int nextByte) throws X {
        handleError();
    }

    @Override
    default void handlePrefixError(int b1) throws X {
        handleError();
    }

    static Utf8Handler<IOException> of(Appendable writer) {
        return new Utf8Handler<IOException>() {
            @Override
            public void handleCodePoint(int codePoint) throws IOException {
                if (Character.isBmpCodePoint(codePoint)) {
                    writer.append((char)codePoint);
                } else {
                    writer.append(Character.highSurrogate(codePoint));
                    writer.append(Character.lowSurrogate(codePoint));
                }
            }
            @Override
            public void handleError() throws IOException {
                writer.append('�');
            }
            @Override
            public void handle1ByteCodePoint(int ascii) throws IOException {
                writer.append((char)ascii);
            }
            @Override
            public void handle2ByteCodePoint(int b1, int b2) throws IOException {
                writer.append((char)Utf8.codePoint(b1, b2));
            }
            @Override
            public void handle3ByteCodePoint(int b1, int b2, int b3) throws IOException {
                writer.append((char)Utf8.codePoint(b1, b2, b3));
            }
            @Override
            public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) throws IOException {
                int cp = Utf8.codePoint(b1, b2, b3, b4);
                writer.append(Character.highSurrogate(cp));
                writer.append(Character.lowSurrogate(cp));
            }
        };
    }

}
