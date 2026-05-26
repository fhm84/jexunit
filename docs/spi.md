# SPI Extension Points

JExUnit uses Java's `ServiceLoader` mechanism to discover extensions. Implementations are registered by creating a plain-text service descriptor under `src/main/resources/META-INF/services/`.

## `DataProvider` — Custom Data Sources

`com.jexunit.core.spi.data.DataProvider`

Implement this interface to replace or supplement Excel as the source of test data. The built-in `ExcelDataProvider` is itself registered this way.

```java
public interface DataProvider {
    /** Return true if this provider can handle the given test class. */
    boolean canProvide(Class<?> testClass);

    /** Called once before loadTestData; use for setup/initialisation. */
    void initialize(Class<?> testClass) throws Exception;

    /** How many test groups does this provider supply? */
    int numberOfTests();

    /** Human-readable label for test group number `number`. */
    String getIdentifier(int number);

    /** Return the test cases for test group number `number`. */
    Collection<Object[]> loadTestData(int number) throws Exception;
}
```

### Registration

Create the file:

```
src/main/resources/META-INF/services/com.jexunit.core.spi.data.DataProvider
```

Content — one fully-qualified class name per line:

```
com.example.tests.MyCustomDataProvider
```

### Selection

When multiple `DataProvider` implementations are on the classpath, `ServiceRegistry` calls `canProvide(testClass)` on each one in discovery order and uses the first that returns `true`. The built-in `ExcelDataProvider` returns `true` when the test class has an `@ExcelFile`-annotated field or method.

## `BeforeSheet` — Pre-Worksheet Hook

`com.jexunit.core.spi.BeforeSheet`

Runs once **before** each worksheet is executed. Use it to reset shared state, truncate database tables, or log worksheet boundaries.

```java
public interface BeforeSheet {
    void run();
}
```

### Example

```java
public class DbResetHook implements BeforeSheet {
    @Override
    public void run() {
        Database.truncateAll();
        System.out.println("DB reset before sheet");
    }
}
```

### Registration

Via SPI service descriptor:

```
src/main/resources/META-INF/services/com.jexunit.core.spi.BeforeSheet
```

```
com.example.tests.DbResetHook
```

Or via `jexunit.properties` (single implementation, no SPI file needed):

```properties
jexunit.sheet.before=com.example.tests.DbResetHook
```

## `AfterSheet` — Post-Worksheet Hook

`com.jexunit.core.spi.AfterSheet`

Runs once **after** each worksheet finishes. Use it for cleanup, reporting, or releasing resources.

```java
public interface AfterSheet {
    void run();
}
```

### Example

```java
public class LoggingHook implements AfterSheet {
    @Override
    public void run() {
        System.out.println("Sheet completed");
    }
}
```

### Registration

Via SPI service descriptor:

```
src/main/resources/META-INF/services/com.jexunit.core.spi.AfterSheet
```

```
com.example.tests.LoggingHook
```

Or via `jexunit.properties`:

```properties
jexunit.sheet.after=com.example.tests.LoggingHook
```

## Using the `serviceloader-maven-plugin`

The `core` module uses the `serviceloader-maven-plugin` to auto-generate the SPI descriptor for `ExcelDataProvider`. If you use the same plugin in your project, annotate your implementation and the descriptor is written for you at build time:

```xml
<plugin>
    <groupId>eu.somatik.serviceloader-maven-plugin</groupId>
    <artifactId>serviceloader-maven-plugin</artifactId>
    <configuration>
        <services>
            <param>com.jexunit.core.spi.data.DataProvider</param>
        </services>
    </configuration>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

Otherwise, create the descriptor files manually as shown above.
