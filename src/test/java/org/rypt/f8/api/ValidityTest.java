package org.rypt.f8.api;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.Test;
import org.rypt.f8.Jdk;
import org.rypt.f8.Sem;
import org.rypt.f8.Utf8;
import org.rypt.f8.Validity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

public class ValidityTest {

    @Test
    public void testCharacteristics() {
        for (Validity v : Validity.values()) {
            assertEquals(v == Validity.ASCII || v == Validity.UNDERFLOW_R0, v.isFullyValid());
            assertNotEquals(v == Validity.MALFORMED, v.isValidOrTruncated());
        }
    }

    @Test
    public void testArray() {
        Sem.testAllCombinations(test -> {
            byte[] b = test.generate();
            for (int from = 0; from < b.length; from++) {
                for (int to = from; to <= b.length; to++) {
                    Validity expected = Jdk.validity(b, from, to);
                    Validity actual = Utf8.validity(b, from, to);
                    assertSame(expected, actual);
                }
            }
        });
    }

    @Test
    public void testStream() {
        Sem.testAllCombinations(test -> {
            byte[] b = test.generate();
            for (int from = 0; from < b.length; from++) {
                for (int to = from; to <= b.length; to++) {
                    Validity expected = Jdk.validity(b, from, to);
                    Validity actual = Utf8.validity(new ByteArrayInputStream(b, from, to - from));
                    assertSame(expected, actual);
                }
            }
        });
    }

    @Test
    public void testBigStream() throws IOException {
        RandomStringGenerator anyChar = new RandomStringGenerator.Builder()
                .withinRange(0, Character.MAX_CODE_POINT)
                .build();

        for (int i = 0; i < 100; i++) {
            byte[] b = anyChar.generate(10000).getBytes(UTF_8);
            for (;;) {
                Validity result = Jdk.validity(b);
                assertSame(result, Utf8.validity(new ByteArrayInputStream(b)));
                if (result == Validity.MALFORMED) {
                    break;
                }
                assertTrue(Utf8.validity(new ByteArrayInputStream(b, 0, b.length - 1)).isValidOrTruncated());
                b[(int)(Math.random() * b.length)] = (byte)(Math.random() * 256);
            }
        }
    }

    @Test
    public void testAscii() throws IOException {
        for (int len = 0; len < 10000; len += 500) {
            byte[] b = new byte[len];
            IntStream.range(0, len).forEach(i -> b[i] = (byte)(i % 128));

            assertSame(Validity.ASCII, Utf8.validity(b, 0, b.length));
            assertSame(Validity.ASCII, Utf8.validity(new ByteArrayInputStream(b)));
        }
    }
}
