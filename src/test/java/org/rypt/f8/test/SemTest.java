package org.rypt.f8.test;

import org.junit.Test;
import org.rypt.f8.Sem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SemTest {

    @Test
    public void semTest() {
        HashSet<Integer> set = new HashSet<>();
        for (Sem sem : Sem.values()) {
            assertTrue(sem.min <= sem.max);
            assertTrue((byte)sem.min <= (byte)sem.max);
            assertTrue(sem.count > 0);
            for (int i = sem.min; i <= sem.max; i++) {
                assertTrue(set.add(i));
            }
            for (int i = 0; i < 100; i++) {
                byte b = sem.generate();
                assertTrue(b >= (byte)sem.min && b <= (byte)sem.max);
            }
        }
        assertEquals(256, set.size());

        for (int i = 0; i < 100; i++) {
            byte[] bytes = new Sem.Combination(Sem.values()).generate();
            assertEquals(bytes.length, Sem.values().length);
            for (int j = 0; j < bytes.length; j++) {
                byte b = bytes[j];
                Sem sem = Sem.values()[j];
                assertTrue(b >= (byte)sem.min && b <= (byte)sem.max);
            }
        }

        for (int maxArraySize = 0; maxArraySize < 6; maxArraySize++) {
            AtomicLong[] counts = new AtomicLong[maxArraySize + 1];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = new AtomicLong();
            }
            Sem.combinations(maxArraySize).forEach(sems -> {
                byte[] bytes1 = sems.generate();
                byte[] bytes2 = sems.generate();
                assertEquals(bytes1.length, bytes2.length);
                counts[bytes1.length].incrementAndGet();
                String s1 = new String(bytes1, UTF_8);
                String s2 = new String(bytes2, UTF_8);
                assertEquals(s1.length(), s2.length());
                boolean v1 = Arrays.equals(bytes1, s1.getBytes(UTF_8));
                boolean v2 = Arrays.equals(bytes2, s2.getBytes(UTF_8));
                assertEquals(v1, v2);
            });

            long l = Sem.values().length;

            for (int i = 0; i < counts.length; i++) {
                long prod = 1;
                for (int j = 0; j < i; j++) {
                    prod *= l;
                }
                assertEquals(prod, counts[i].get());
            }
            assertEquals(1, counts[0].get());
        }

    }
}
