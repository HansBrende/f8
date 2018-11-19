package org.rypt.f8;

/**
 * An implementation of {@link Utf8Handler} that records byte statistics and decodes a UTF-8 encoded
 * byte stream into a string.
 *
 * @author Hans Brende (hansbrende@apache.org)
 */
public class Utf8StringBuilder extends Utf8Statistics implements CharSequence {
    private final StringBuilder sb;

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
