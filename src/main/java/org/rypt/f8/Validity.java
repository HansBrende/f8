package org.rypt.f8;

/**
 * This class encapsulates the possible UTF-8 validities of a sequence of bytes
 */
public enum Validity {

    /**
     * Pure ASCII
     */
    ASCII,

    /**
     * Valid UTF-8 that is not necessarily truncated and that is not pure ASCII
     */
    UNDERFLOW_R0,

    /**
     * Valid UTF-8 that is truncated after the first byte in a multi-byte sequence
     */
    UNDERFLOW_R1,

    /**
     * Valid UTF-8 that is truncated after the second byte in a multi-byte sequence
     */
    UNDERFLOW_R2,

    /**
     * Valid UTF-8 that is truncated after the third byte in a multi-byte sequence
     */
    UNDERFLOW_R3,

    /**
     * Malformed UTF-8
     */
    MALFORMED;

    public boolean isFullyValid() {
        return ordinal() < 2;
    }

    public boolean isValidOrTruncated() {
        return this != MALFORMED;
    }

}
