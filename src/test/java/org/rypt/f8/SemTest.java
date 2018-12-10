package org.rypt.f8;

import org.junit.Test;

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

        AtomicLong count = new AtomicLong();
        Sem.testAllCombinations(sems -> {
            byte[] bytes1 = sems.generate();
            byte[] bytes2 = sems.generate();
            String s1 = new String(bytes1, UTF_8);
            String s2 = new String(bytes2, UTF_8);
            assertEquals(s1.length(), s2.length());
            boolean v1 = Arrays.equals(bytes1, s1.getBytes(UTF_8));
            boolean v2 = Arrays.equals(bytes2, s2.getBytes(UTF_8));
            assertEquals(v1, v2);
            count.incrementAndGet();
        });

        long l = Sem.values().length;
        assertEquals(1 + l + l*l + l*l*l + l*l*l*l + l*l*l*l*l, count.get());
    }
}
