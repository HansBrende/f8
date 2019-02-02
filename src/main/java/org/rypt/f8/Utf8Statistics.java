package org.rypt.f8;

import java.io.OutputStream;

/**
 * An implementation of {@link Utf8Handler} that records statistics for valid and invalid UTF-8 byte sequences
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8Statistics extends OutputStream implements Utf8Handler<RuntimeException> {

    private int state;
    private long numAscii;
    private long num2byte;
    private long num3byte;
    private long num4byte;
    private long numTruncated;
    private long numError;

    public void reset() {
        state = 0;
        numAscii = 0;
        num2byte = 0;
        num3byte = 0;
        num4byte = 0;
        numTruncated = 0;
        numError = 0;
    }

    @Override
    public void write(int b) {
        state = Utf8.nextState(state, (byte)b, this);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void write(byte[] b, int off, int len) {
        state = Utf8.nextState(state, b, off, off + len, this);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void write(byte[] b) {
        state = Utf8.nextState(state, b, 0, b.length, this);
    }

    @Override
    public void close() {
        Utf8.finish(state, this);
        state = 0;
    }

    @Override
    public void handleError() {
        numError++;
    }

    @Override
    public void handleContinuationError(int b1, int b2, int b3, int nextByte) {
        if (nextByte == END_OF_STREAM) {
            numTruncated++;
        }
        handleError();
    }

    @Override
    public void handleContinuationError(int b1, int b2, int nextByte) {
        if (nextByte == END_OF_STREAM) {
            numTruncated++;
        }
        handleError();
    }

    @Override
    public void handleContinuationError(int b1, int nextByte) {
        if (nextByte == END_OF_STREAM) {
            numTruncated++;
        }
        handleError();
    }

    @Override
    public void handleCodePoint(int codePoint) {
        if (codePoint > 0xFFFF) {
            num4byte++;
        } else if (codePoint > 0x7FF) {
            num3byte++;
        } else if (codePoint > 0x7F) {
            num2byte++;
        } else {
            numAscii++;
        }
    }

    @Override
    public void handle1ByteCodePoint(int ascii) {
        numAscii++;
    }

    @Override
    public void handle2ByteCodePoint(int b1, int b2) {
        num2byte++;
    }

    @Override
    public void handle3ByteCodePoint(int b1, int b2, int b3) {
        num3byte++;
    }

    @Override
    public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
        num4byte++;
    }

    /**
     *
     * @return the total number of valid UTF-8 code points encountered
     */
    public long countCodePoints() {
        return numAscii + num2byte + num3byte + num4byte;
    }

    /**
     * @deprecated Use {@link #countCodePoints()} {@code - }{@link #countAscii()} instead.
     * @return the number of valid UTF-8 multi-byte sequences encountered
     */
    @Deprecated
    public long countValid() {
        return countCodePoints() - numAscii;
    }

    /**
     * @return the number of invalid UTF-8 byte sequences encountered
     */
    public long countInvalid() {
        return numError;
    }

    /**
     * @return the number of invalid UTF-8 byte sequences encountered, ignoring
     * a final invalid byte sequence that could have been valid given more bytes
     */
    public long countInvalidIgnoringTruncation() {
        return numError - numTruncated;
    }

    /**
     * @return the number of bytes encountered with a leading 0-bit (i.e., those in the range 0-0x7F)
     */
    public long countAscii() {
        return numAscii;
    }

    /**
     *
     * @return the number of 2-byte UTF-8 characters encountered (i.e., those in the range 0x80-0x7FF)
     */
    public long count2Byte() {
        return num2byte;
    }

    /**
     *
     * @return the number of 3-byte UTF-8 characters encountered (i.e., those in the range 0x800-0xFFFF)
     */
    public long count3Byte() {
        return num3byte;
    }

    /**
     *
     * @return the number of 4-byte UTF-8 characters encountered (i.e., those in the range 0x10000-0x10FFFF)
     */
    public long count4Byte() {
        return num4byte;
    }

    /**
     * @return true if the encountered byte sequence looks like UTF-8 (and not plain ASCII)
     */
    public boolean looksLikeUtf8() {
        //condition for what "looks like" UTF-8 borrowed from ICU4j
        return count2Byte() + count3Byte() + count4Byte() > countInvalidIgnoringTruncation() * 10;
    }

    @Override
    public String toString() {
        return "state=0x" + Integer.toHexString(state)
                + "; ascii=" + numAscii
                + "; 2-byte=" + num2byte
                + "; 3-byte=" + num3byte
                + "; 4-byte=" + num4byte
                + "; error=" + numError
                + (numTruncated != 0 ? " (was truncated)" : "");
    }
}
