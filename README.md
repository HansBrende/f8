# F8
A super lightweight, lightning-fast UTF-8 state machine for Java.

## Use Cases

### Check if an array or InputStream is 100% valid UTF-8

```java
boolean valid = Utf8.validity(inputStream).isFullyValid();
```

### Check if an array or InputStream is valid or truncated UTF-8
```java
boolean valid = Utf8.validity(inputStream).isValidOrTruncated();
```

### Get detailed UTF-8 statistics for an InputStream

```java
public static void printStats(InputStream is) throws IOException {
    Utf8Statistics stats = new Utf8Statistics();
    Utf8.transfer(is, stats);
    System.out.println("Number of legal UTF-8 code points: " + stats.countCodePoints());
    System.out.println("Number of errors: " + stats.countInvalid());
    System.out.println("Is UTF-8: " + stats.looksLikeUtf8());
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
* VM version: JDK 11.0.1, Java HotSpot(TM) 64-Bit Server VM, 11.0.1+13-LTS
* Warmup: 5 iterations, 10 s each
* Measurement: 5 iterations, 10 s each
* Timeout: 10 min per iteration
* Threads: 1 thread, will synchronize iterations
* Benchmark mode: Average time, time/op

### Check validity of small (1KB), valid stream (mostly ASCII)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.278|±   0.001|μs/op|
|guava¹|1.089|±   0.020|μs/op|
|jdk|2.385|±   0.018|μs/op|

### Check validity of large (1MB), valid stream (mostly ASCII)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|285.033|±   1.048|μs/op|
|guava¹|1016.110|±  30.400|μs/op|
|jdk|2372.054|±  12.848|μs/op|

### Check validity of small (1KB), valid stream (Latin)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.479|±   0.001|μs/op|
|guava¹|1.155|±   0.005|μs/op|
|jdk|1.993|±   0.050|μs/op|

### Check validity of large (1MB), valid stream (Latin)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|463.924|±   1.642|μs/op|
|guava¹|1137.092|±  14.823|μs/op|
|jdk|1798.416|±  13.872|μs/op|

### Check validity of small (1KB), valid stream (Asian)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.625|±   0.001|μs/op|
|guava¹|1.239|±   0.016|μs/op|
|jdk|2.059|±   0.009|μs/op|

### Check validity of large (1MB), valid stream (Asian)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|604.933|±   2.406|μs/op|
|guava¹|1150.243|±  61.086|μs/op|
|jdk|1888.871|±  13.152|μs/op|

### Check validity of small (1KB), valid stream (Random)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.789 ±   0.018|μs/op|
|guava¹|1.459 ±   0.013|μs/op|
|jdk|3.035 ±   0.019|μs/op|

### Check validity of large (1MB), valid stream (Random)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|1776.979|±   4.526|μs/op|
|guava¹|2343.484|±  17.019|μs/op|
|jdk|3674.982|±   7.860|μs/op|

### Check validity of small (1KB), malformed stream
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.046|±   0.001|μs/op|
|guava¹|0.755|±   0.032|μs/op|
|jdk|1.088|±   0.004|μs/op|

### Check validity of large (1MB), malformed stream
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.194|±   0.001|μs/op|
|guava¹|586.142|±   3.535|μs/op|
|jdk|758.279|±   6.973|μs/op|

### Check validity of small (1KB), valid array (mostly ASCII)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.231|±   0.002|μs/op|
|guava¹|0.346|±   0.002|μs/op|
|jdk|1.739|±  0.039|μs/op|

### Check validity of large (1MB), valid array (mostly ASCII)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|255.731|±   1.481|μs/op|
|guava¹|391.040|±   2.014|μs/op|
|jdk|1832.193|±  96.147|μs/op|

### Check validity of small (1KB), valid array (Latin)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.432|±   0.002|μs/op|
|guava¹|0.762|±   0.002|μs/op|
|jdk|1.359|±  0.009|μs/op|

### Check validity of large (1MB), valid array (Latin)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|428.642|±   9.042|μs/op|
|guava¹|809.458|± 231.040|μs/op|
|jdk|1236.243|±   6.211|μs/op|

### Check validity of small (1KB), valid array (Asian)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.581|±   0.002|μs/op|
|guava¹|0.785|±   0.001|μs/op|
|jdk|1.436|±  0.009|μs/op|

### Check validity of large (1MB), valid array (Asian)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|569.532|±   1.102|μs/op|
|guava¹|808.560|±  46.631|μs/op|
|jdk|1344.186|± 100.171|μs/op|

### Check validity of small (1KB), valid array (Random)
|Method|Score|Error|Units|
|---|---:|---|---|
|**f8**|0.741|±   0.006|μs/op|
|guava¹|0.742|±   0.006|μs/op|
|jdk|2.216|±  0.010|μs/op|

### Check validity of large (1MB), valid array (Random)
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|1797.348|±   5.115|μs/op|
|**guava¹**|1660.678|±  28.553|μs/op|
|jdk|3132.032|±  22.856|μs/op|

### Check validity of small (1KB), malformed array
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|0.007|±   0.001|μs/op|
|**guava¹**|0.005|±   0.001|μs/op|
|jdk|0.384|±  0.004|μs/op|

### Check validity of large (1MB), malformed array
|Method|Score|Error|Units|
|---|---:|---|---|
|f8|0.007|±   0.001|μs/op|
|**guava¹**|0.005|±   0.001|μs/op|
|jdk|175.668|±  16.053|μs/op|

¹ Does not have the ability to check the validity of a truncated stream.
