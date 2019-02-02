package org.rypt.f8;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
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

    private static final Random random = new Random(0);
    private static int random() {
        return allCharacters[random.nextInt(allCharacters.length)];
    }

    private static boolean isValidCodePoint(int codePoint) {
        switch (Character.getType(codePoint)) {
            case Character.UNASSIGNED:
            case Character.SURROGATE:
            case Character.PRIVATE_USE:
                return false;
            default:
                return true;
        }
    }

    private static final int[] allCharacters = IntStream.rangeClosed(0, Character.MAX_CODE_POINT)
            .filter(Utf8Benchmark::isValidCodePoint)
            .toArray();

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
        MOSTLY_ASCII {
            @Override
            byte[] generate(int minBytes) {
                return bytes(IntStream.range(0, minBytes)
                        .map(i -> i % 256 != 255 ? i % 0x80 : random()));
            }
        },
        LATIN {
            @Override
            byte[] generate(int minBytes) {
                int count = (minBytes + 1) / 2;
                return bytes(IntStream.range(0, count).map(i -> i % (0x800 - 0x80) + 0x80));
            }
        },
        ASIAN {
            @Override
            byte[] generate(int minBytes) {
                int count = (minBytes + 2) / 3;
                return bytes(IntStream.range(0, count).map(i -> i % (0xD800 - 0x800) + 0x800));
            }
        },
        RANDOM {
            @Override
            byte[] generate(int minBytes) {
                return bytes(IntStream.range(0, minBytes).map(i -> random()));
            }
        },
        INVALID {
            @Override
            byte[] generate(int minBytes) {
                byte[] bytes = new byte[minBytes];
                bytes[0] = (byte)0xc0;
                return bytes;
            }
        };

        abstract byte[] generate(int minBytes);

        final byte[] bytes = generate(maxByteLength);
    }

    private static final int LENGTH_BIG = 1024 * 1024;
    private static final int LENGTH_SMALL = 1024;

    @Param({""+LENGTH_SMALL,
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
        jdk {
            @Override
            boolean isValid(InputStream in) throws IOException {
                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();
                return Jdk.validity(bytes).isValidOrTruncated();
            }

            @Override
            boolean isValid(byte[] bytes, int len) {
                return Jdk.validity(bytes, 0, len).isValidOrTruncated();
            }
        },
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
        };

        abstract boolean isValid(InputStream in) throws IOException;

        abstract boolean isValid(byte[] bytes, int len);
    }

    @Param
    public Range chars;

    @Benchmark
    public boolean testArrayValidity() {
        return utf8Detector.isValid(chars.bytes, length);
    }

    @Benchmark
    public boolean testStreamValidity() throws IOException {
        return utf8Detector.isValid(new ByteArrayInputStream(chars.bytes, 0, length));
    }


    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(MethodHandles.lookup().lookupClass().getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupIterations(5)
                .measurementIterations(5)
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

}
