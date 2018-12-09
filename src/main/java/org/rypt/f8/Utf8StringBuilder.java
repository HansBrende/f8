package org.rypt.f8;

/**
 * An implementation of {@link Utf8Handler} that records byte statistics and decodes a UTF-8 encoded
 * byte stream into a string.
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8StringBuilder extends Utf8Statistics implements CharSequence {
    protected final StringBuilder sb;

    public Utf8StringBuilder(StringBuilder sb) {
        this.sb = sb;
    }

    public Utf8StringBuilder() {
        this(new StringBuilder());
    }

    @Override
    public void handleCodePoint(int codePoint) {
        super.handleCodePoint(codePoint);
        sb.appendCodePoint(codePoint);
    }

    @Override
    public void reset() {
        super.reset();
        sb.setLength(0);
    }

    @Override
    public void handleError() {
        super.handleError();
        sb.append('�');
    }

    @Override
    public void handle1ByteCodePoint(int ascii) {
        super.handle1ByteCodePoint(ascii);
        sb.append((char)ascii);
    }

    @Override
    public void handle2ByteCodePoint(int b1, int b2) {
        super.handle2ByteCodePoint(b1, b2);
        sb.append((char)Utf8.codePoint(b1, b2));
    }

    @Override
    public void handle3ByteCodePoint(int b1, int b2, int b3) {
        super.handle3ByteCodePoint(b1, b2, b3);
        sb.append((char)Utf8.codePoint(b1, b2, b3));
    }

    @Override
    public void handle4ByteCodePoint(int b1, int b2, int b3, int b4) {
        super.handle4ByteCodePoint(b1, b2, b3, b4);
        int cp = Utf8.codePoint(b1, b2, b3, b4);
        sb.append(Character.highSurrogate(cp));
        sb.append(Character.lowSurrogate(cp));
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
