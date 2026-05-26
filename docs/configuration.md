# Configuration Reference

JExUnit is configured through a `jexunit.properties` file placed on the classpath (e.g. `src/test/resources/jexunit.properties`). All settings have defaults and can also be changed programmatically at runtime.

## Programmatic Configuration

```java
@BeforeClass
public static void configure() {
    // Override a built-in config key
    JExUnitConfig.setConfigProperty(JExUnitConfig.ConfigKey.DATE_PATTERN.getKey(), "MM/dd/yyyy");

    // Register multiple properties at once
    Properties props = new Properties();
    props.setProperty("mytest.mykey", "myvalue");
    JExUnitConfig.registerConfig(props);

    // Store custom application config alongside framework config
    JExUnitConfig.setConfigProperty("mytest.apiUrl", "http://localhost:8080");
}
```

Custom keys are also accessible later:

```java
String url = JExUnitConfig.getStringProperty("mytest.apiUrl");
```

## Framework Configuration Keys

### Date & Time Formatting

| Key | Default | Description |
|---|---|---|
| `jexunit.datePattern` | `dd.MM.yyyy` | Pattern used to parse `Date` values from Excel cells |
| `jexunit.dateTimePattern` | `dd.MM.yyyy hh:mm:ss` | Pattern used to parse `Date`+time values |

### Built-in Command Names

The names used in Excel for built-in commands can be changed. An optional prefix can also be applied to all of them at once.

| Key | Default | Description |
|---|---|---|
| `jexunit.defaultcommand_prefix` | *(empty)* | Prepended to every built-in command name |
| `jexunit.defaultcommand.disabled` | `disabled` | Keyword to disable a worksheet |
| `jexunit.defaultcommand.report` | `report` | Keyword to log a message from the Excel file |
| `jexunit.defaultcommand.exception_expected` | `exception` | Column name that signals an expected exception |
| `jexunit.defaultcommand.comment` | `comment` | Column name for assertion failure messages |
| `jexunit.defaultcommand.fastfail` | `fastFail` | Column name for per-row fast-fail override |
| `jexunit.defaultcommand.multiline` | `multiline` | Column name for per-row multiline override |
| `jexunit.defaultcommand.breakpoint` | `breakpoint` | Column name for in-Excel breakpoints |
| `jexunit.multiline_commands` | *(empty)* | Comma-separated list of command names that are always multiline |

Example — prefix all built-in keywords with `jx.` and use a US date format:

```properties
jexunit.defaultcommand_prefix=jx.
jexunit.datePattern=MM/dd/yyyy
jexunit.dateTimePattern=MM/dd/yyyy hh:mm:ss
```

### Excel Header Keyword

| Key | Default | Description |
|---|---|---|
| `jexunit.command_statement` | `command` | The keyword that marks a header row in the Excel file |

### Command Discovery

| Key | Default | Description |
|---|---|---|
| `jexunit.annotation-scan.package` | *(empty — whole classpath)* | Comma-separated packages to restrict `@TestCommand` scanning to |

Restricting the scan improves startup time in large projects:

```properties
jexunit.annotation-scan.package=com.example.tests,com.example.commands
```

### Command Name Resolution

Prefix and postfix settings allow shorter command names in Excel by matching against the stripped method/class name.

| Key | Default | Description |
|---|---|---|
| `jexunit.command.class_prefix` | *(empty)* | Prefix stripped from class names during matching |
| `jexunit.command.class_postfix` | *(empty)* | Postfix stripped from class names during matching |
| `jexunit.command.method_prefix` | *(empty)* | Prefix stripped from method names during matching |
| `jexunit.command.method_postfix` | *(empty)* | Postfix stripped from method names during matching |

### Command Validation

| Key | Default | Description |
|---|---|---|
| `jexunit.command.validation.type` | `WARN` | What to do when an Excel command has no matching implementation |

Allowed values:

| Value | Behaviour |
|---|---|
| `WARN` | Log a warning; the row is skipped |
| `FAIL` | Throw an exception; the test fails immediately |
| `NONE` | Silently skip unmatched rows |

### Sheet Lifecycle Hooks

| Key | Default | Description |
|---|---|---|
| `jexunit.sheet.before` | *(empty)* | Fully-qualified class name of a `BeforeSheet` implementation to run before each worksheet |
| `jexunit.sheet.after` | *(empty)* | Fully-qualified class name of an `AfterSheet` implementation to run after each worksheet |

Example:

```properties
jexunit.sheet.before=com.example.tests.SetupHook
jexunit.sheet.after=com.example.tests.TeardownHook
```

These can also be registered as SPI services — see [spi.md](spi.md).

## Complete Example `jexunit.properties`

```properties
# Date formats
jexunit.datePattern=MM/dd/yyyy
jexunit.dateTimePattern=MM/dd/yyyy hh:mm:ss

# Restrict annotation scanning for faster startup
jexunit.annotation-scan.package=com.example.tests

# Fail hard on unknown commands instead of just warning
jexunit.command.validation.type=FAIL

# Commands that always receive all their rows as a multiline block
jexunit.multiline_commands=createUser,createOrder

# Sheet hooks
jexunit.sheet.before=com.example.tests.DbResetHook
jexunit.sheet.after=com.example.tests.LoggingHook
```
