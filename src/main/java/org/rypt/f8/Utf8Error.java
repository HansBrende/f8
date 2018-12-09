package org.rypt.f8;

public class Utf8Error extends Exception {

    private static final Utf8Error error = new Utf8Error();

    private Utf8Error() {
        super(null, null, false, false);
    }

    public static void fail() throws Utf8Error {
        throw error;
    }

}
