package org.rypt.f8;

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

    public String toString() {
        return sb.toString();
    }
}
