# SPI Extension Points

JExUnit uses Java's `ServiceLoader` mechanism to discover data providers. Implementations are registered by creating a plain-text service descriptor under `src/main/resources/META-INF/services/`. The lifecycle hooks (`BeforeSheet`, `AfterSheet`) are configured via `jexunit.properties` instead.

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

When multiple `DataProvider` implementations are on the classpath, the runner calls `canProvide(testClass)` on each one in discovery order and uses the **last** one that returns `true`. The provider instances are shared by all test classes. The built-in `ExcelDataProvider` returns `true` when the test class has exactly one `@ExcelFile`-annotated static field or public static method.

## `BeforeSheet` — Before Hook

`com.jexunit.core.spi.BeforeSheet`

Runs once **before** the worksheets are executed: with JUnit 5 once per test class, with JUnit 4 once per Excel file. Use it to prepare shared state, e.g. truncate database tables.

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
        System.out.println("DB reset before the worksheets");
    }
}
```

### Registration

Via `jexunit.properties` (a single implementation with a public no-arg constructor):

```properties
jexunit.sheet.before=com.example.tests.DbResetHook
```

## `AfterSheet` — After Hook

`com.jexunit.core.spi.AfterSheet`

Runs once **after** the worksheets are executed: with JUnit 5 once per test class, with JUnit 4 once per Excel file. Use it for cleanup, reporting, or releasing resources.

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
        System.out.println("Worksheets completed");
    }
}
```

### Registration

Via `jexunit.properties` (a single implementation with a public no-arg constructor):

```properties
jexunit.sheet.after=com.example.tests.LoggingHook
```

## Using the `serviceloader-maven-plugin`

The `jexunit-base` module uses the `serviceloader-maven-plugin` to generate the SPI descriptor for `ExcelDataProvider` at build time. If you use the same plugin in your project, it finds the implementations of the configured interfaces in your compiled classes and writes the descriptor for you:

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
