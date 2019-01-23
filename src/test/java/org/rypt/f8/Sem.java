package org.rypt.f8;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

public enum Sem {
    ASCII(0, 0x7f),
    Cont8(0x80, 0x8f),
    Cont9(0x90, 0x9f),
    ContAB(0xa0, 0xbf),
    cErr(0xc0, 0xc1),
    cd(0xc2, 0xdf),
    e0(0xe0, 0xe0),
    eL(0xe1, 0xec),
    ed(0xed, 0xed),
    eU(0xee, 0xef),
    f0(0xf0, 0xf0),
    f(0xf1, 0xf3),
    f4(0xf4, 0xf4),
    fErr(0xf5, 0xff);

    final int min;
    final int max;
    final int count;

    Sem(int min, int max) {
        this.min = min;
        this.max = max;
        this.count = max - min + 1;
    }

    private static final int COUNT = values().length;
    private static final Sem FIRST = values()[0];

    byte generate() {
        return (byte)(Math.random() * count + min);
    }

    public interface CombinationConsumer extends Consumer<Combination> {
        void acc(Combination c) throws Exception;

        default void accept(Combination c) {
            try {
                acc(c);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void testAllCombinations(CombinationConsumer test) {
        combinations(5).forEach(test);
        combinations(5).parallel().forEach(test);
    }

    public static Stream<Combination> combinations(int maxArraySize) {
        long maxSize = 0;

        for (int i = 0; i <= maxArraySize; i++) {
            long prod = 1;
            for (int j = 0; j < i; j++) {
                prod *= COUNT;
            }
            maxSize += prod;
        }
        return Stream.iterate(new Combination(), Combination::next).limit(maxSize).unordered();
    }

    public static class Combination {
        public final Sem[] array;

        public Combination(Sem... array) {
            this.array = array;
        }

        public byte[] generate() {
            Sem[] array = this.array;
            byte[] bytes = new byte[array.length];
            int pos = 0;
            for (Sem sem : array) {
                bytes[pos++] = sem.generate();
            }
            return bytes;
        }

        Combination next() {
            Sem[] array = this.array;
            int len = array.length;
            for (int i = len - 1; i >= 0; i--) {
                int nextOrd = array[i].ordinal() + 1;
                if (nextOrd != COUNT) {
                    Sem[] copy = Arrays.copyOf(array, len);
                    copy[i++] = values()[nextOrd];
                    for (; i < len; i++) {
                        copy[i] = FIRST;
                    }
                    return new Combination(copy);
                }
            }
            Sem[] n = new Sem[len + 1];
            for (int i = 0; i <= len; i++) {
                n[i] = FIRST;
            }
            return new Combination(n);
        }
    }

}
