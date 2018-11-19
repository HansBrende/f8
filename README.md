# F8
A super lightweight, lightning-fast UTF-8 state machine for Java.

## Use Cases

### Check if an InputStream is 100% valid UTF-8

```java
public static boolean isValidUtf8(InputStream is) throws IOException {
    int state = 0;
    int b;
    while ((b = is.read()) != -1) {
        state = Utf8.nextState(state, (byte)b);
        if (Utf8.isErrorState(state)) {
            return false;
        }
    }
    return state >= 0; //Or return true if stream was truncated
}
```

### Print out UTF-8 statistics for an InputStream

```java
public static void printStats(InputStream is) throws IOException {
    Utf8Statistics stats = new Utf8Statistics();
    int b;
    while ((b = is.read()) != -1) {
        stats.write(b);
    }
    stats.close();
    System.out.println("Number of legal UTF-8 multibyte sequences: " + stats.countValid());
    System.out.println("Number of illegal UTF-8 sequences: " + stats.countInvalid());
    System.out.println("Number of ASCII characters: " + stats.countAscii());
    System.out.println("Looks like UTF-8: " + stats.looksLikeUtf8());
}
```

### Decode a UTF-8-encoded InputStream to a string

```java
public static String decodeUtf8(InputStream is) throws IOException {
    Utf8StringBuilder stringBuilder = new Utf8StringBuilder();
    int b;
    while ((b = is.read()) != -1) {
        stringBuilder.write(b);
    }
    stringBuilder.close();
    return stringBuilder.toString();
}
```


## Maven

Add the following dependency to your pom:

```xml
<dependency>
  <groupId>org.rypt</groupId>
  <artifactId>f8</artifactId>
  <version>1.0</version>
</dependency>
```