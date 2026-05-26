# Test Commands

A *test command* is a Java method annotated with `@TestCommand`. JExUnit discovers all such methods on the classpath and dispatches Excel rows to them by command name.

## Defining a Command

```java
@TestCommand("add")
public static void runAddCommand(TestCase<?> testCase, ArithmeticalTestObject obj) {
    assertThat(obj.getParam1() + obj.getParam2(), equalTo(obj.getResult()));
}
```

- The method may be **static or non-static** — both work.
- The method may live **anywhere on the classpath**: in the test class itself or in a standalone command-provider class. No base class is required.
- Command names are **case-insensitive** at match time.

### Multiple Names for One Command

Pass an array of names to handle several Excel keywords with the same method:

```java
@TestCommand({"sub", "subtract"})
public void runSubCommand(TestCase<?> testCase, ArithmeticalTestObject obj) {
    assertThat(obj.getParam1() - obj.getParam2(), equalTo(obj.getResult()));
}
```

### Fast-Fail

Setting `fastFail = true` aborts the entire test group as soon as this command fails, instead of collecting the error and continuing:

```java
@TestCommand(value = "createUser", fastFail = true)
public void createUser(TestCase<?> testCase) { ... }
```

The `fastFail` flag can also be overridden per row in the Excel file — see [excel-format.md](excel-format.md).

## Standalone Command-Provider Classes

Commands do not have to live in the test class. Any class on the classpath with `@TestCommand` methods is discovered automatically:

```java
// No special annotation on the class itself is required
public class ArithmeticalTestCommands {

    @TestCommand("add")
    public void runAddCommand(TestCase<?> testCase, ArithmeticalTestObject obj) { ... }
}
```

This lets you organise commands in separate files while a minimal test class holds only the `@RunWith` and `@ExcelFile`:

```java
@RunWith(JExUnit.class)
public class ArithmeticalTest {
    @ExcelFile
    static String[] excelFiles = {"src/test/resources/ArithmeticalTests.xlsx"};
}
```

## Parameter Injection

JExUnit resolves command method parameters by type and annotation. You can declare any combination of the following:

### `TestCase<?>`

The raw test case. Gives access to command name, all cell values, and flags set in the Excel row (multiline, breakpoint, …).

```java
@TestCommand("myCmd")
public void handle(TestCase<?> testCase) {
    String raw = testCase.getValues().get("columnName").getValue();
}
```

### Your Own Entity (automatic object creation)

Declare any Java class as a parameter. JExUnit creates a new instance and uses OGNL to populate its fields from the column values in the current row. Column names must match property names (supports nested paths like `address.street`):

```java
@TestCommand("createOrder")
public void createOrder(TestCase<?> testCase, Order order) {
    // order.getCustomerName() is already populated from the "customerName" column
}
```

Multiple different entity types are allowed. The same type must not appear twice.

See [object-mapping.md](object-mapping.md) for advanced OGNL usage.

### `@TestParam` — Typed Primitive Parameters

Use `@TestParam` to inject a single column value, type-converted to the declared parameter type:

```java
@TestCommand("calculate")
public void calculate(@TestParam("param1") double a,
                      @TestParam("param2") double b,
                      @TestParam("result") double expected) {
    assertThat(a + b, equalTo(expected));
}
```

- The `value` attribute specifies the column name. If omitted the Java parameter name is used (requires `-parameters` compiler flag).
- `required = false` makes the injection optional (parameter receives `null`/default if the column is absent).

Supported types: primitives and their wrappers, `String`, `Date`, `BigDecimal`, enums, and any type registered via custom `PropertyUtils` conversion.

### `TestContext`

Inject the shared test context to read or write state between commands:

```java
@TestCommand("storeUser")
public void storeUser(TestCase<?> testCase, TestContext ctx) {
    User user = buildUser(testCase);
    ctx.add(User.class, user);
}

@TestCommand("verifyUser")
public void verifyUser(TestCase<?> testCase, TestContext ctx) {
    User user = ctx.get(User.class);
    assertThat(user.getName(), equalTo(...));
}
```

See [test-context.md](test-context.md) for the full API.

### `@Context` — Inject from TestContext Directly

Annotate a parameter with `@Context` to have the framework pull a value out of `TestContext` automatically, without declaring `TestContext` itself:

```java
@TestCommand("verifyOrder")
public void verifyOrder(TestCase<?> testCase, @Context Order order) {
    // order was stored by an earlier command via ctx.add(Order.class, order)
}
```

To retrieve a value stored under a string ID (when multiple instances of the same type exist):

```java
@TestCommand("verifyOrder")
public void verifyOrder(@Context("primaryOrder") Order order) { ... }
```

## Command Discovery and Validation

JExUnit scans the classpath for `@TestCommand` annotations at startup. You can limit the scan to specific packages for faster startup and fewer false positives:

```properties
# jexunit.properties
jexunit.annotation-scan.package=com.example.mytests,com.example.commands
```

### Validation Modes

Control what happens when a command name in the Excel file has no matching implementation:

| `jexunit.command.validation.type` | Behaviour |
|---|---|
| `WARN` (default) | Logs a warning; the row is skipped |
| `FAIL` | Throws an exception; the whole test fails |
| `NONE` | Silently skips unmatched rows |

### Name Prefix/Postfix

If all your command classes or methods follow a naming convention you can configure prefixes/postfixes so shorter names can be used in Excel:

```properties
jexunit.command.class_prefix=Test
jexunit.command.class_postfix=Commands
jexunit.command.method_prefix=run
jexunit.command.method_postfix=Command
```

With the above settings a method named `runAddCommand` in class `TestArithmeticCommands` is reachable by the Excel command name `add` in the `arithmetic` class.
