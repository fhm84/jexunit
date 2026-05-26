# JExUnit Architecture

JExUnit is a JUnit 4 extension that executes tests defined in Excel spreadsheets. Each Excel row maps to a *test command* dispatched to an annotated Java method.

## Data Flow

```
@RunWith(JExUnit.class) test class
  │
  ├─ ServiceRegistry (SPI)
  │    └─ discovers DataProvider implementations
  │
  ├─ ExcelDataProvider
  │    └─ delegates to ExcelLoader (Apache POI)
  │         └─ reads .xlsx → List<TestCase<ExcelMetadata>>
  │
  ├─ JExUnit (Suite runner)
  │    └─ creates one JExUnitBase runner per test (worksheet or command)
  │
  └─ JExUnitBase (Parameterized runner)
       └─ for each TestCase:
            ├─ handles built-in commands (disabled, report, ...)
            └─ TestCommandRunner
                 ├─ TestCommandScanner finds @TestCommand method
                 ├─ TestObjectHelper maps TestCell values → Java objects (OGNL)
                 └─ invokes command method; ErrorCollector aggregates failures
```

## Module Structure

| Module | Artifact | Role |
|---|---|---|
| `core/` | `jexunit-core` | Framework library — the main deliverable |
| `examples-simple/` | — | Basic usage examples, always built |
| `examples-complex/` | — | Advanced examples, built with `-Pexample-tests` Maven profile |

Java source/target compatibility: **Java 8**.

## Package Layout (`com.jexunit.core`)

```
com.jexunit.core
├── JExUnit.java               — JUnit Suite runner (entry point)
├── JExUnitBase.java           — Parameterized test base; drives the test loop
├── JExUnitConfig.java         — Configuration (reads jexunit.properties)
├── commands/
│   ├── annotation/
│   │   ├── TestCommand.java   — @TestCommand (repeatable)
│   │   └── TestParam.java     — @TestParam for primitive parameters
│   ├── validation/            — Command validation types (WARN, FAIL, NONE)
│   ├── DefaultCommands.java   — Enum of built-in command keywords
│   ├── TestCommandRunner.java — Finds and invokes the right @TestCommand method
│   └── TestCommandScanner.java— Classpath scanner for @TestCommand annotations
├── context/
│   ├── Context.java           — @Context injection annotation
│   ├── TestContext.java       — Thread-safe key/value store shared across commands
│   └── TestContextManager.java— Manages TestContext lifecycle
├── data/
│   ├── TestObjectHelper.java  — Creates/populates Java objects from TestCase data
│   ├── OgnlUtils.java         — OGNL-based property setting/getting
│   └── PropertyUtils.java     — String-to-typed-value conversion (dates, enums, …)
├── dataprovider/
│   ├── ExcelDataProvider.java — SPI DataProvider implementation for Excel
│   ├── ExcelFile.java         — @ExcelFile annotation
│   ├── ExcelLoader.java       — Apache POI parser; row-wise and transposed layouts
│   └── ExcelMetadata.java     — Metadata attached to each loaded TestCase
├── model/
│   ├── TestCase.java          — A single command invocation with its parameters
│   ├── TestCell.java          — One cell: identifier (column name) + string value
│   └── Metadata.java          — Base interface for test case metadata
└── spi/
    ├── ServiceRegistry.java   — Loads SPI implementations via ServiceLoader
    ├── BeforeSheet.java       — Hook run before each worksheet
    ├── AfterSheet.java        — Hook run after each worksheet
    └── data/DataProvider.java — SPI for custom data sources
```

## Key Classes

### `JExUnit`
Extends JUnit's `Suite`. Reads the test class's `@ExcelFile` annotation, asks the `ServiceRegistry` for a `DataProvider`, and builds one child runner (`JExUnitBase`) per test group (worksheet or individual command, depending on `worksheetAsTest`).

### `JExUnitBase`
A `@Parameterized` JUnit runner. Its `@Parameters` method returns the loaded `TestCase` list. The `@Test` method iterates through commands:
- Applies built-in commands (disabled, report, exception, …)
- Delegates user commands to `TestCommandRunner`

### `ExcelLoader`
Reads `.xlsx` files via Apache POI. Parses each row into a `TestCase`:
- Column A = command name
- The preceding `COMMAND` header row defines the keys for subsequent data rows
- Supports *transposed* layout (columns instead of rows) via `@ExcelFile(transpose = true)`

### `TestCommandRunner`
Uses `TestCommandScanner` results to find the `@TestCommand` method matching the current command name (case-insensitive). Resolves method parameters by type (see [test-commands.md](test-commands.md)).

### `TestObjectHelper`
Given a `TestCase` and a Java class, creates an instance and uses OGNL to set each column value as a property. Supports nested properties (`address.street`), collections, and enum conversion.

### `TestContext`
A `ConcurrentHashMap`-backed store scoped to the current test run. Commands can share state by storing and retrieving objects by type or by string ID. See [test-context.md](test-context.md).

### `JExUnitConfig`
Loads `jexunit.properties` from the classpath once at startup. All settings can also be changed programmatically via `JExUnitConfig.setConfigProperty(key, value)` before the test run (e.g. in a `@BeforeClass`). See [configuration.md](configuration.md).

## SPI Extension Points

JExUnit uses Java's `ServiceLoader` mechanism. Register implementations in:

```
src/main/resources/META-INF/services/<interface-fully-qualified-name>
```

| Interface | Purpose |
|---|---|
| `com.jexunit.core.spi.data.DataProvider` | Custom data source (replaces or augments Excel) |
| `com.jexunit.core.spi.BeforeSheet` | Callback before each worksheet is executed |
| `com.jexunit.core.spi.AfterSheet` | Callback after each worksheet is executed |

See [spi.md](spi.md) for implementation details.

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JUnit | 4.13.2 | Test runner base |
| Apache POI (OOXML) | 5.5.1 | Excel file parsing |
| OGNL | 3.2.21 | Object property mapping |
| Apache Commons Configuration | 2.15.0 | Configuration management |
| Apache Commons BeanUtils | 1.11.0 | Bean property utilities |
| Annotation Detector | 3.0.5 | Classpath scanning for `@TestCommand` |
| Lombok | 1.18.46 | Boilerplate reduction (compile-time only) |
