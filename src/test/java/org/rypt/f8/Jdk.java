package org.rypt.f8;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Jdk {

    public static Validity validity(byte[] b, int from, int to) {
        return validity(from == 0 && to == b.length ? b : Arrays.copyOfRange(b, from, to));
    }

    public static Validity validity(byte[] bytes) {
        ByteBuffer in = ByteBuffer.wrap(bytes);
        CoderResult result = UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(in, CharBuffer.allocate(bytes.length), false);
        if (result.isMalformed()) {
            return Validity.MALFORMED;
        }
        if (result.isUnderflow()) {
            switch (in.remaining()) {
                case 0:
                    for (byte b : bytes)
                        if (b < 0)
                            return Validity.UNDERFLOW_R0;
                    return Validity.ASCII;
                case 1:
                    return Validity.UNDERFLOW_R1;
                case 2:
                    return in.get() == (byte)0xed && (in.get() >> 5) == (((byte)0xa0) >> 5)
                            ? Validity.MALFORMED : Validity.UNDERFLOW_R2;
                case 3:
                    return Validity.UNDERFLOW_R3;
                default:
                    throw new AssertionError("remaining: " + in.remaining());
            }
        }

        throw new AssertionError(result);
    }
}
