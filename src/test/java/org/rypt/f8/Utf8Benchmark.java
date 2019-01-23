package org.rypt.f8;

import org.mozilla.intl.chardet.nsUTF8Verifier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("ALL")
@State(Scope.Benchmark)
public class Utf8Benchmark {

//    private static final int l1 = 0x20, u1 = 0x80;
//    private static final int l2 = 0x80, u2 = 0x800;
//    private static final int l3 = 0x800, u3 = 0xD800;
//    private static final int l4 = 0x20000, u4 = 0x110000; //jchardet incorrectly counts 0x10000 to 0x20000 as illegal
//
//    private static final int count1 = 1000, count2 = 1000, count3 = 1000, count4 = 0;
//    private static final boolean random = true;
//
//    enum Range {
//        a(l1, u1, count1),
//        b(l2, u2, count2),
//        c(l3, u3, count3),
//        d(l4, u4, count4);
//        final int l, u, count;
//        Range(int l, int u, int count) {
//            this.l = l;
//            this.u = u;
//            this.count = count;
//        }
//        IntStream stream() {
//            return IntStream.range(0, count).map(i -> i % (u - l) + l);
//        }
//    }

    private static final Random random = new Random(0);
    private static int random() {
        return rand[random.nextInt(rand.length)];
//        int ret;
//        do {
//            ret = random.nextInt(Character.MAX_CODE_POINT + 1 - 0x20) + 0x20;
//        } while (!Character.isDefined(ret) || ret >= Character.MIN_SURROGATE && ret <= Character.MAX_SURROGATE);
//        return ret;
    }

    private static final int[] rand = IntStream.range(0, Character.MAX_CODE_POINT + 1).filter(i -> Character.isDefined(i) && (i < Character.MIN_SURROGATE || i > Character.MAX_SURROGATE)).toArray();

    private static final int maxByteLength = 1024 * 1024;

    private static byte[] bytes(IntStream s) {
        byte[] bytes = s.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString().getBytes(UTF_8);
        if (bytes.length > maxByteLength) {
            return Arrays.copyOf(bytes, maxByteLength);
        }
        return bytes;
    }

    public enum Range {
//        MOSTLY_ASCII {
//            @Override
//            byte[] generate(int minBytes) {
//                return bytes(IntStream.range(0, minBytes)
//                        .map(i -> i % 256 != 255 ? i % (0x80 - 0x20) + 0x20 : random()));
//            }
//        },
//        LATIN {
//            @Override
//            byte[] generate(int minBytes) {
//                int count = (minBytes + 1) / 2;
//                return bytes(IntStream.range(0, count).map(i -> i % (0x800 - 0x80) + 0x80));
//            }
//        },
//        ASIAN {
//            @Override
//            byte[] generate(int minBytes) {
//                int count = (minBytes + 2) / 3;
//                return bytes(IntStream.range(0, count).map(i -> i % (0xD800 - 0x800) + 0x800));
//            }
//        },
        RANDOM {
            @Override
            byte[] generate(int minBytes) {
                return bytes(IntStream.range(0, minBytes).map(i -> random()));
            }
//        },
//        INVALID {
//            @Override
//            byte[] generate(int minBytes) {
//                byte[] bytes = new byte[minBytes];
//                bytes[0] = (byte)0xc0;
//                return bytes;
//            }
        };

        abstract byte[] generate(int minBytes);

        final byte[] bytes = generate(maxByteLength);
    }

    private static final int LENGTH_BIG = 1024 * 1024;
    private static final int LENGTH_SMALL = 1024;

    @Param({//""+LENGTH_SMALL,
            ""+LENGTH_BIG})
    public int length;

    @Param
    public Utf8Detector utf8Detector;

    public enum Utf8Detector {
        f8 {
            @Override
            boolean isValid(InputStream in) throws IOException {
                return Utf8.validity(in).isValidOrTruncated();
            }

            @Override
            boolean isValid(byte[] bytes, int len) {
                return Utf8.validity(bytes, 0, len).isValidOrTruncated();
            }
        },
//        jdk {
//            @Override
//            boolean isValid(InputStream in) throws IOException {
//                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();
//
//                return Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8));
//            }
//
//            @Override
//            boolean isValid(byte[] bytes, int len) {
//                return Arrays.equals(Arrays.copyOf(bytes, len),
//                        new String(bytes, 0, len, UTF_8).getBytes(UTF_8));
//            }
//        },
        guava {
            @Override
            boolean isValid(InputStream in) throws IOException {
                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();
                return com.google.common.base.Utf8.isWellFormed(bytes);
            }

            @Override
            boolean isValid(byte[] bytes, int len) {
                return com.google.common.base.Utf8.isWellFormed(bytes, 0, len);
            }
//        },
//        jchardet {
//            @Override
//            boolean isValid(InputStream in) throws IOException {
//                int b;
//                byte state = 0;
//                while ((b = in.read()) != -1) {
//                    state = nsVerifier.getNextState(jchardetUtf8Verifier, (byte)b, state);
//                    if (state == 1) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//
//            @Override
//            boolean isValid(byte[] bytes, int len) {
//                byte state = 0;
//                for (byte b : bytes) {
//                    state = nsVerifier.getNextState(jchardetUtf8Verifier, (byte)b, state);
//                    if (state == 1) {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        },
//        jchardetNsDetector {
//            @Override
//            boolean isValid(InputStream in) throws IOException {
//                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();
//
//                nsDetector det = new nsDetector(nsDetector.ALL);
//                det.DoIt(bytes, bytes.length, false);
//                det.DataEnd();
//                return "UTF-8".equalsIgnoreCase(det.getProbableCharsets()[0]);
//            }
//
//            @Override
//            boolean isValid(byte[] bytes, int len) {
//                nsDetector det = new nsDetector(nsDetector.ALL);
//                det.DoIt(bytes, len, false);
//                det.DataEnd();
//                return "UTF-8".equalsIgnoreCase(det.getProbableCharsets()[0]);
//            }
        };

        abstract boolean isValid(InputStream in) throws IOException;

        abstract boolean isValid(byte[] bytes, int len);
    }


//    @Benchmark
//    public boolean testLegalStream() throws IOException {
//        return utf8Detector.isValid(new ByteArrayInputStream(
//                        testBytes[0], 0, length));
//    }
//
//    @Benchmark
//    public boolean testIllegalStream() throws IOException {
//        return utf8Detector.isValid(new ByteArrayInputStream(
//                testBytes[1], 0, length));
//    }

    @Param
    public Range chars;

    @Benchmark
    public boolean testArrayValidity() {
        return utf8Detector.isValid(chars.bytes, length);
    }

//    @Benchmark
//    public boolean testStreamValidity() throws IOException {
//        return utf8Detector.isValid(new ByteArrayInputStream(chars.bytes, 0, length));
//    }

//    @Benchmark
//    public boolean testIllegalArray() throws IOException {
//        return utf8Detector.isValid(testBytes[1], length);
//    }


    public static void main(String[] args) throws RunnerException {
//        for (byte[] bytes : testBytes) {
//            if (LENGTH_BIG > bytes.length) {
//                throw new AssertionError();
//            }
//        }

        Options opt = new OptionsBuilder()
                .include(MethodHandles.lookup().lookupClass().getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupIterations(2)
                .measurementIterations(2)
                .forks(1)
                .build();

        new Runner(opt).run();
    }



    private static <O extends OutputStream> O copy(InputStream in, O out) throws IOException {
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.close();
        return out;
    }

    private static final nsUTF8Verifier jchardetUtf8Verifier = new nsUTF8Verifier();

//    private static final byte[][] testBytes = {
//            createValidBytes(),
//            createInvalidBytes()
//    };
//
//    private static byte[] createValidBytes() {
//        IntStream s = IntStream.concat(
//                    IntStream.concat(Range.a.stream(), Range.b.stream()),
//                    IntStream.concat(Range.c.stream(), Range.d.stream()));
//        if (random) {
//            s = s.boxed().sorted(Comparator.comparingLong(i -> new Random(i).nextLong())).mapToInt(i -> i);
//        }
//
//        return s.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
//                .toString().getBytes(UTF_8);
//    }
//
//    private static byte[] createInvalidBytes() {
//        byte[] array = createValidBytes();
//        array[array.length / 256] = (byte)0xc0;
//        return array;
//    }

}
