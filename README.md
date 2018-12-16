# F8
A super lightweight, lightning-fast UTF-8 state machine for Java.

## Use Cases

### Check if an InputStream is 100% valid UTF-8

```java
public static boolean isValidUtf8(InputStream is, boolean allowTruncation) throws IOException {
    return allowTruncation ? Utf8.isValidUpToTruncation(is) : Utf8.isFullyValid(is);
}
```

### Print out UTF-8 statistics for an InputStream

```java
public static void printStats(InputStream is) throws IOException {
    Utf8Statistics stats = new Utf8Statistics();
    Utf8.transferAndFinish(is, stats);
    System.out.println("Number of legal UTF-8 multibyte sequences: " + stats.countValid());
    System.out.println("Number of illegal UTF-8 sequences: " + stats.countInvalid());
    System.out.println("Number of ASCII characters: " + stats.countAscii());
    System.out.println("Looks like UTF-8: " + stats.looksLikeUtf8());
}
```

### Decode a UTF-8-encoded InputStream to a string

```java
public static String decodeUtf8(InputStream is) throws IOException {
    StringBuilder sb = new StringBuilder();
    Utf8.transferAndFinish(is, Utf8Handler.of(sb));
    return sb.toString();
}
```


## Maven

Add the following dependency to your pom:

```xml
<dependency>
  <groupId>org.rypt</groupId>
  <artifactId>f8</artifactId>
  <version>1.1-RC1</version>
</dependency>
```

## Benchmarks

* JMH version: 1.21
* VM version: JDK 1.8.0_162, Java HotSpot(TM) 64-Bit Server VM, 25.162-b12
* VM invoker: /Library/Java/JavaVirtualMachines/jdk1.8.0_162.jdk/Contents/Home/jre/bin/java
* VM options: -Dvisualvm.id=154019807906310 -javaagent:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar=59782:/Applications/IntelliJ IDEA CE.app/Contents/bin -Dfile.encoding=UTF-8
* Warmup: 5 iterations, 10 s each
* Measurement: 5 iterations, 10 s each
* Timeout: 10 min per iteration
* Threads: 1 thread, will synchronize iterations
* Benchmark mode: Average time, time/op

### Check validity of small (1KB), invalid stream
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|1.059|±   0.005|μs/op|
|guava¹|1.317|±   0.016|μs/op|
|nsVerifier|4.389|±   0.016|μs/op|
|jdk¹|4.721|±   0.048|μs/op|
|nsDetector|129.054|±   1.760|μs/op|

### Check validity of large (1MB), invalid stream
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|6.081|±   0.409|μs/op|
|nsVerifier|17.994|±   3.301|μs/op|
|guava¹|535.554|±  15.209|μs/op|
|nsDetector|965.870|±  14.774|μs/op|
|jdk¹|5597.113|± 727.656|μs/op|

### Check validity of small (1KB), valid stream
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|1.063|±   0.017|μs/op|
|guava¹|1.324|±   0.011|μs/op|
|jdk¹|4.683|±   0.027|μs/op|
|nsVerifier|4.866|±   2.616|μs/op|
|nsDetector|128.531|±   1.436|μs/op|

### Check validity of large (1MB), valid stream
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|1287.941|±   5.982|μs/op|
|guava¹|1301.060|±  34.174|μs/op|
|nsVerifier|4489.327|±  18.586|μs/op|
|jdk¹|5004.361|± 165.839|μs/op|
|nsDetector|65034.387|± 569.180|μs/op|

¹ Does not have the ability to check the validity of a truncated stream.
