package org.rypt.f8;

import java.io.OutputStream;

/**
 * An implementation of {@link Utf8Handler} that records statistics for valid and invalid UTF-8 byte sequences
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8Statistics extends OutputStream implements Utf8Handler<RuntimeException> {

    private int state;
    private long numValid;
    private long numInvalid;
    private long numAscii;
    private long numTruncated;

    public void reset() {
        state = 0;
        numValid = 0;
        numInvalid = 0;
        numAscii = 0;
        numTruncated = 0;
    }

    /**
     * @deprecated Use {@link Utf8#nextState(int, byte, Utf8ByteHandler)} instead.
     */
    @Override
    @Deprecated
    public void write(int b) {
        state = Utf8.nextState(state, (byte)b, this);
    }

    /**
     * @deprecated Use {@link Utf8#nextState(int, byte[], int, int, Utf8ByteHandler)} instead.
     */
    @Override
    @Deprecated
    public void write(byte[] b, int off, int len) {
        state = Utf8.nextState(state, b, off, off + len, this);
    }

    /**
     * @deprecated Use {@link Utf8#nextState(int, byte[], int, int, Utf8ByteHandler)} instead.
     */
    @Override
    @Deprecated
    public void write(byte[] b) {
        write(b, 0, b.length);
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

    /**
     * @deprecated Use {@link Utf8#finish(int, Utf8ByteHandler)} instead.
     */
    @Override
    @Deprecated
    public void close() {
        Utf8.finish(state, this);
        state = 0;
    }

    /**
     * @return the number of valid UTF-8 multi-byte sequences encountered
     */
    public long countValid() {
        return numValid;
    }

    /**
     * @return the number of invalid UTF-8 byte sequences encountered
     */
    public long countInvalid() {
        return numInvalid;
    }

    /**
     * @return the number of invalid UTF-8 byte sequences encountered, ignoring
     * a final invalid byte sequence that could have been valid given more bytes
     */
    public long countInvalidIgnoringTruncation() {
        return numInvalid - numTruncated;
    }

    /**
     * @return the number of bytes encountered with a leading 0-bit (i.e., those in the range 0-0x7F)
     */
    public long countAscii() {
        return numAscii;
    }

    @Override
    public void handleError() {
        numInvalid++;
    }

    /**
     * @return true if the encountered byte sequence looks like UTF-8
     */
    public boolean looksLikeUtf8() {
        //condition for what "looks like" UTF-8 borrowed from ICU4j
        return countValid() > countInvalidIgnoringTruncation() * 10;
    }

    @Override
    public void handleCodePoint(int codePoint) {
        if (codePoint > 0x7F) {
            numValid++;
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
        numValid++;
    }

    @Override
    public void handle3ByteCodePoint(int b1, int b2, int b3) {
        numValid++;
    }

    @Override
    public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
        numValid++;
    }

    @Override
    public String toString() {
        return "state=0x" + Integer.toHexString(state)
                + "; valid=" + numValid
                + "; invalid=" + numInvalid
                + (numTruncated != 0 ? " (was truncated); " : "; ")
                + "; ascii=" + numAscii;
    }
}
