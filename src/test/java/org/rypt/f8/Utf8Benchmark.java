package org.rypt.f8;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsUTF8Verifier;
import org.mozilla.intl.chardet.nsVerifier;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("ALL")
@State(Scope.Benchmark)
public class Utf8Benchmark {

    private static final int LENGTH_BIG = 1024 * 1024;
    private static final int LENGTH_SMALL = 1024;

    @Param({""+LENGTH_SMALL, ""+LENGTH_BIG})
    public int length;

    @Param
    public Utf8Detector utf8Detector;

    public enum Utf8Detector {
        f8 {
            @Override
            boolean isValid(InputStream in) throws IOException {
                return Utf8.isValidUpToTruncation(in);
            }
        },
        jdk {
            @Override
            boolean isValid(InputStream in) throws IOException {
                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();

                return Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8));
            }
        },
        guava {
            @Override
            boolean isValid(InputStream in) throws IOException {
                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();

                return com.google.common.base.Utf8.isWellFormed(bytes);
            }
        },
        jchardet {
            @Override
            boolean isValid(InputStream in) throws IOException {
                int b;
                byte state = 0;
                while ((b = in.read()) != -1) {
                    state = nsVerifier.getNextState(jchardetUtf8Verifier, (byte)b, state);
                    if (state == 1) {
                        return false;
                    }
                }
                return true;
            }
        },
        jchardetNsDetector {
            @Override
            boolean isValid(InputStream in) throws IOException {
                byte[] bytes = copy(in, new ByteArrayOutputStream()).toByteArray();

                nsDetector det = new nsDetector(nsDetector.ALL);
                det.DoIt(bytes, bytes.length, false);
                det.DataEnd();
                return "UTF-8".equalsIgnoreCase(det.getProbableCharsets()[0]);
            }
        };

        abstract boolean isValid(InputStream in) throws IOException;
    }


    @Benchmark
    public boolean testLegal() throws IOException {
        return utf8Detector.isValid(new ByteArrayInputStream(
                        testBytes[0], 0, length));
    }

    @Benchmark
    public boolean testIllegal() throws IOException {
        return utf8Detector.isValid(new ByteArrayInputStream(
                testBytes[1], 0, length));
    }


    public static void main(String[] args) throws RunnerException {
        for (byte[] bytes : testBytes) {
            if (LENGTH_BIG > bytes.length) {
                throw new AssertionError();
            }
        }

        Options opt = new OptionsBuilder()
                .include(MethodHandles.lookup().lookupClass().getSimpleName())
//                .warmupIterations(1)
//                .measurementIterations(1)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
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

    private static final byte[][] testBytes = {
            createValidBytes(),
            createInvalidBytes()
    };

    private static byte[] createValidBytes() {
        return IntStream.concat(IntStream.concat(IntStream.range(0x20, 0xD800),
                //jchardet incorrectly counts code points from 0x10000 to 0x20000 as illegal
                IntStream.range(0xE000, 0x10000)), IntStream.range(0x20000, 0x54828))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString().getBytes(UTF_8);
    }

    private static byte[] createInvalidBytes() {
        byte[] array = createValidBytes();
        array[array.length / 256] = (byte)0xc0;
        return array;
    }

}
