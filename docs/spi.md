# SPI Extension Points

JExUnit uses Java's `ServiceLoader` mechanism to discover data providers. Implementations are registered by creating a plain-text service descriptor under `src/main/resources/META-INF/services/`. The lifecycle hooks (`BeforeSheet`, `AfterSheet`) and the `CommandInstanceFactory` are configured via `jexunit.properties` instead.

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

When multiple `DataProvider` implementations are on the classpath, the runner calls `canProvide(testClass)` on each one in discovery order and uses the **last** one that returns `true`. A new instance of that provider is created for each test class (via its public no-arg constructor), so providers can keep per-class state. The built-in `ExcelDataProvider` returns `true` when the test class has exactly one `@ExcelFile`-annotated static field or public static method.

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

## `CommandInstanceFactory` — Dependency Injection for Commands

`com.jexunit.core.spi.CommandInstanceFactory`

Creates the instances of the [command classes](test-commands.md#command-classes) and of the [command-provider classes](test-commands.md#standalone-command-provider-classes). By default they are created via their no-arg constructor (`DefaultCommandInstanceFactory`). Plug in a dependency injection container (Spring, CDI, Guice, …) to get services or repositories injected into the commands.

```java
public interface CommandInstanceFactory {
    <T> T create(Class<T> type, Context context) throws Exception;

    interface Context {
        Object getTestInstance();                 // the running test instance
        <T> Optional<T> unwrap(Class<T> type);    // JUnit 5: the ExtensionContext
    }
}
```

- A **command class** is created for each invocation (row), a **command provider** once per test group (worksheet) and then reused.
- Commands declared in the **test class** itself are invoked on the test instance, which the test framework creates (and e.g. Spring's `SpringExtension` already injects), so they don't use the factory.
- With **JUnit 5**, `context.unwrap(ExtensionContext.class)` returns the `ExtensionContext` of the test. With JUnit 4 there is none; `getTestInstance()` is the test instance if the test class extends `JExUnitBase`, else `null`.
- If the factory throws an exception or returns `null`, the row fails (and doesn't satisfy `exception = true`).
- `@TestParam`/`@Context` on parameters work as usual.

### Example: Spring (JUnit 5)

```java
public class SpringCommandInstanceFactory implements CommandInstanceFactory {

    @Override
    public <T> T create(Class<T> type, Context context) {
        ExtensionContext extensionContext = context.unwrap(ExtensionContext.class)
                .orElseThrow(() -> new IllegalStateException("JUnit 5 is needed"));
        // the application context of the test class (@ExtendWith(SpringExtension.class), @SpringBootTest, ...)
        return SpringExtension.getApplicationContext(extensionContext)
                .getAutowireCapableBeanFactory()
                .createBean(type);
    }
}
```

```java
@SpringBootTest
@ExtendWith(JExUnitExtension.class)
class OrderTest {
    @ExcelFile
    static String file = "src/test/resources/orders.xlsx";

    @ExcelTest
    void test() {}
}

@TestCommand("createOrder")
public class CreateOrderCommand {
    @Autowired
    private OrderService orderService;

    public void run(@TestParam("customer") String customer, TestContext ctx) {
        ctx.add("order", orderService.create(customer));
    }
}
```

`createBean` creates a new instance and injects its dependencies; the command classes don't have to be Spring beans. To use beans of the application context instead (e.g. with their own scope), return `getBean(type)`. Your factory can delegate to `new DefaultCommandInstanceFactory().create(type, context)` for classes the container doesn't manage.

### Registration

Via `jexunit.properties` (a single implementation with a public no-arg constructor, created once):

```properties
jexunit.command.instance_factory=com.example.tests.SpringCommandInstanceFactory
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
