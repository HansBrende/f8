package org.rypt.f8;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsUTF8Verifier;
import org.mozilla.intl.chardet.nsVerifier;
import org.openjdk.jmh.annotations.Benchmark;
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
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("ALL")
public class Utf8Benchmark {

    @Benchmark
    public boolean guava() throws IOException {
        byte[] bytes = copy(new TestInputStream(), new ByteArrayOutputStream()).toByteArray();

        return com.google.common.base.Utf8.isWellFormed(bytes);
    }

    @Benchmark
    public boolean jdk() throws IOException {
        byte[] bytes = copy(new TestInputStream(), new ByteArrayOutputStream()).toByteArray();

        return Arrays.equals(bytes, new String(bytes, UTF_8).getBytes(UTF_8));
    }

    @Benchmark
    public boolean f8() throws IOException {
        Utf8Statistics stats = copy(new TestInputStream(), new Utf8Statistics());

        return stats.countInvalid() == 0;
    }

    @Benchmark
    public boolean jchardetNsDetector() throws IOException {
        byte[] bytes = copy(new TestInputStream(), new ByteArrayOutputStream()).toByteArray();

        nsDetector det = new nsDetector(nsDetector.ALL);
        det.DoIt(bytes, bytes.length, false);
        det.DataEnd();
        return "UTF-8".equalsIgnoreCase(det.getProbableCharsets()[0]);
    }

    @Benchmark
    public boolean jchardetNsVerifier() throws IOException {
        JchardetVerifier verifier = copy(new TestInputStream(), new JchardetVerifier());

        return verifier.state != 1;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MethodHandles.lookup().lookupClass().getSimpleName())
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

    private static class JchardetVerifier extends OutputStream {
        private final nsUTF8Verifier utf8Verifier = new nsUTF8Verifier();
        byte state = 0;
        @Override
        public void write(int b) {
            state = nsVerifier.getNextState(utf8Verifier, (byte)b, state);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            nsUTF8Verifier utf8Verifier = this.utf8Verifier;
            byte state = this.state;
            for (byte b : bytes) {
                state = nsUTF8Verifier.getNextState(utf8Verifier, b, state);
            }
            this.state = state;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            nsUTF8Verifier utf8Verifier = this.utf8Verifier;
            byte state = this.state;
            for (int i = off, to = off + len; i < to; i++) {
                state = nsUTF8Verifier.getNextState(utf8Verifier, b[i], state);
            }
            this.state = state;
        }
    }

    private static class TestInputStream extends ByteArrayInputStream {
        TestInputStream() {
            super(testBytes);
        }
    }

    private static final byte[] testBytes = IntStream.concat(IntStream.range(0x20, 0xD800), IntStream.range(0xE000, 0x10000))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString().getBytes(UTF_8);
}
