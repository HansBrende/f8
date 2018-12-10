package org.rypt.f8;

import static org.junit.Assert.assertTrue;

enum Sem {
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
        assertTrue(this.min <= this.max);
        assertTrue(this.count > 0);
    }

    byte generate() {
        byte b = (byte)(Math.random() * count + min);
        assertTrue(b >= (byte)min);
        assertTrue(b <= (byte)max);
        return b;
    }

    public interface CombinationConsumer {
        void acc(Combination c) throws Exception;

        default void accept(Combination c) {
            try {
                acc(c);
            } catch (Exception e) {
                sneakyThrow(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    static void testAllCombinations(CombinationConsumer tester) {
        tester.accept(new Combination());
        for (Sem s0 : values()) {
            tester.accept(new Combination(s0));
            for (Sem s1 : values()) {
                tester.accept(new Combination(s0, s1));
                for (Sem s2 : values()) {
                    tester.accept(new Combination(s0, s1, s2));
                    for (Sem s3 : values()) {
                        tester.accept(new Combination(s0, s1, s2, s3));
                        for (Sem s4 : values()) {
                            tester.accept(new Combination(s0, s1, s2, s3, s4));
                        }
                    }
                }
            }
        }
    }

    public static class Combination {
        public final Sem[] array;

        public Combination(Sem... array) {
            this.array = array;
        }

        public byte[] generate() {
            byte[] bytes = new byte[array.length];
            int pos = 0;
            for (Sem sem : array) {
                bytes[pos++] = sem.generate();
            }
            return bytes;
        }
    }

}
