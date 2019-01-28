package org.rypt.f8;

class __ {

    static <X extends Exception> int state(byte[] b, int off, int to, Utf8ByteHandler<X> handler) throws X {
        while (off < to) {
            int b1 = b[off++];
            if (b1 >= 0) { //0xxxxxxx
                do {
                    handler.handle1ByteCodePoint(b1);
                } while (off < to && (b1 = b[off++]) >= 0);
                if (b1 >= 0) {
                    return 0;
                }
            }
            if (b1 < (byte)0xe0) {
                if (b1 < (byte)0xc2) {
                    handler.handlePrefixError(b1);
                } else if (off < to) { //110xxxxx 10xxxxxx
                    int b2 = b[off++];
                    if (b2 > (byte)0xbf) { //is not continuation
                        handler.handleContinuationError(b1, b2);
                        off--;
                    } else {
                        handler.handle2ByteCodePoint(b1, b2);
                    }
                } else { //110xxxxx
                    return b1;
                }
            } else if (b1 < (byte)0xf0) {
                if (off + 1 < to) { //1110xxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3;

                    // Sneaky shortcut for:
//                    if (b2 > (byte)0xbf
//                       || b1 == (byte)0xe0 && b2 < (byte)0xa0
//                       || b1 == (byte)0xed && b2 > (byte)0x9f) {
                    if (((b1+1^1) + (b2|~0xe0) & 0xee) << 24 >= 0xae000000) {
                        handler.handleContinuationError(b1, b2);
                        if (b1 == (byte)0xed && b2 < (byte)0xc0) {
                            handler.handleIgnoredByte(b2);
                            if ((b3 = b[off++]) < (byte)0xc0) {
                                handler.handleIgnoredByte(b3);
                                off++;
                            }
                        }
                        off--;
                    } else if ((b3 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3);
                        off--;
                    } else {
                        handler.handle3ByteCodePoint(b1, b2, b3);
                    }
                } else if (off < to) {
                    int b2 = b[off];
                    if (((b1+1^1) + (b2|~0xe0) & 0xee) << 24 >= 0xae000000) {
                        handler.handleContinuationError(b1, b2);
                        if (b1 == (byte)0xed && b2 < (byte)0xc0) {
                            handler.handleIgnoredByte(b2);
                            return Utf8.SURROGATE_PREFIX;
                        }
                    } else {
                        return b1 << 8 | b2 & 0xff;
                    }
                } else {
                    return b1;
                }
            } else {
                if (off + 2 < to) { //11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++], b3, b4;
                    // Sneaky shortcut for:
//                    if (b2 > (byte)0xbf || b1 > (byte)0xf4
//                       || b1 == (byte)0xf0 && b2 < (byte)0x90
//                       || b1 == (byte)0xf4 && b2 > (byte)0x8f) {
                    if (b2 > (byte)0xbf || (b1 << 28) + 0x70 + b2 >> 30 != 0) {
                        if (b1 > (byte)0xf4) {
                            handler.handlePrefixError(b1);
                        } else {
                            handler.handleContinuationError(b1, b2);
                        }
                        off--;
                    } else if ((b3 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3);
                        off--;
                    } else if ((b4 = b[off++]) > (byte)0xbf) {
                        handler.handleContinuationError(b1, b2, b3, b4);
                        off--;
                    } else {
                        handler.handle4ByteCodePoint(b1, b2, b3, b4);
                    }
                } else if (off < to) {
                    int b2 = b[off++], b3;
                    if (b2 > (byte)0xbf || (b1 << 28) + 0x70 + b2 >> 30 != 0) {
                        if (b1 > (byte)0xf4) {
                            handler.handlePrefixError(b1);
                        } else {
                            handler.handleContinuationError(b1, b2);
                        }
                        off--;
                    } else if (off < to) {
                        if ((b3 = b[off]) > (byte)0xbf) {
                            handler.handleContinuationError(b1, b2, b3);
                        } else {
                            return b1 << 16 | b2 << 8 & 0xff00 | b3 & 0xff;
                        }
                    } else {
                        return b1 << 8 | b2 & 0xff;
                    }
                } else if (b1 > (byte)0xf4) {
                    handler.handlePrefixError(b1);
                    return 0;
                } else {
                    return b1;
                }
            }
        }
        return 0;
    }


    static Validity validity(byte[] b, int off, int to) {
        while (off < to) {
            int b1 = b[off++];
            if (b1 >= 0) { //0xxxxxxx
                //noinspection StatementWithEmptyBody
                while (off < to && (b1 = b[off++]) >= 0);
                if (b1 >= 0) {
                    return Validity.UNDERFLOW_R0;
                }
            }
            if (b1 < (byte)0xe0) {
                if (b1 < (byte)0xc2) {
                    return Validity.MALFORMED;
                } else if (off < to) { //110xxxxx 10xxxxxx
                    if (b[off++] > (byte)0xbf) { //is not continuation
                        return Validity.MALFORMED;
                    }
                } else { //110xxxxx
                    return Validity.UNDERFLOW_R1; //1 of 2
                }
            } else if (b1 < (byte)0xf0) {
                if (off + 1 < to) { //1110xxxx 10xxxxxx 10xxxxxx
                    if (((b1+1^1) + (b[off++]|~0xe0) & 0xee) << 24 >= 0xae000000
                            | b[off++] > (byte)0xbf) {
                        return Validity.MALFORMED;
                    }
                } else if (off < to) {
                    if (((b1+1^1) + (b[off]|~0xe0) & 0xee) << 24 >= 0xae000000) {
                        return Validity.MALFORMED;
                    } else {
                        return Validity.UNDERFLOW_R2; //2 of 3
                    }
                } else {
                    return Validity.UNDERFLOW_R1; //1 of 3
                }
            } else {
                if (off + 2 < to) { //11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                    int b2 = b[off++];
                    if (b2 > (byte)0xbf | (b1 << 28) + 0x70 + b2 >> 30 != 0
                            | b[off++] > (byte)0xbf | b[off++] > (byte)0xbf) {
                        return Validity.MALFORMED;
                    }
                } else if (off < to) {
                    int b2 = b[off++];
                    if (b2 > (byte)0xbf || (b1 << 28) + 0x70 + b2 >> 30 != 0) {
                        return Validity.MALFORMED;
                    } else if (off < to) {
                        if (b[off] > (byte)0xbf) {
                            return Validity.MALFORMED;
                        } else {
                            return Validity.UNDERFLOW_R3; //3 of 4
                        }
                    } else {
                        return Validity.UNDERFLOW_R2; //2 of 4
                    }
                } else if (b1 > (byte)0xf4) {
                    return Validity.MALFORMED;
                } else {
                    return Validity.UNDERFLOW_R1; //1 of 4
                }
            }
        }
        return Validity.UNDERFLOW_R0;
    }
}
