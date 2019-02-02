package org.rypt.f8.test;

import org.junit.Test;
import org.rypt.f8.Jdk;
import org.rypt.f8.Sem;
import org.rypt.f8.Validity;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;

public class JdkTest {

    @Test
    public void test() {
        Sem.testAllCombinations(sem -> {
            byte[] b = sem.generate();
            boolean isValid = Arrays.equals(b, new String(b, UTF_8).getBytes(UTF_8));
            Validity result = Jdk.validity(b);
            assertEquals(isValid, result.isFullyValid());

            if (result.isValidOrTruncated()) {
                for (int n = 1; n < b.length; n++) {
                    assertTrue(Jdk.validity(b, 0, n).isValidOrTruncated());
                }
            }

            for (byte i : b) {
                if (i == (byte)0xc0 || i == (byte)0xc1 || i < 0 && i > (byte)0xf4) {
                    assertEquals(Validity.MALFORMED, result);
                }
            }

            for (byte i : b) {
                if (i < 0) {
                    assertNotEquals(Validity.ASCII, result);
                    return;
                }
            }
            assertEquals(Validity.ASCII, result);
        });
    }

    @Test
    public void testPartial() {
        byte[] b = {0, 0, 0, 0, (byte)0xc0};

        for (int from = 0; from < b.length; from++) {
            assertEquals(Validity.ASCII, Jdk.validity(b, from, b.length - 1));
            assertEquals(Validity.MALFORMED, Jdk.validity(b, from, b.length));
        }
    }
}
