# JExUnit Architecture

JExUnit executes tests defined in Excel spreadsheets with JUnit 5 (recommended) or JUnit 4 (legacy). Each Excel row maps to a *test command* dispatched to an annotated Java method.

## Data Flow

```
JUnit 5: @ExtendWith(JExUnitExtension.class)     JUnit 4: @RunWith(JExUnit.class)
  └─ JExUnitExtension                              └─ JExUnit (Suite runner)
       │  (TestTemplateInvocationContextProvider)       │
       └──────────────────────┬─────────────────────────┘
                              │
  ├─ ServiceRegistry (SPI) — discovers DataProvider implementations
  │
  ├─ ExcelDataProvider
  │    └─ ExcelLoader (Apache POI): reads .xlsx → List<TestCase<ExcelMetadata>> per test group
  │
  ├─ CommandValidator — removes/fails rows without command implementation
  │
  └─ per test group (worksheet, or row with worksheetAsTest = false):
       JUnit 5: one @ExcelTest invocation (JExUnitInterceptor)
       JUnit 4: one JExUnitBase.test() run (via the Parameterized runner)
         ├─ new TestContext
         └─ for each TestCase:
              ├─ built-in commands (disabled, report) and flags (exception, fastFail, …)
              └─ TestCommandRunner
                   ├─ TestCommandScanner finds the @TestCommand method/class
                   ├─ parameters: TestCase, TestContext, @Context, @TestParam, entities (TestObjectHelper/OGNL)
                   └─ invokes the command; failures are collected and reported per test group
```

## Module Structure

| Module | Artifact | Role |
|---|---|---|
| `jexunit-base/` | `jexunit-base` | JUnit-agnostic kernel: Excel parsing, OGNL mapping, command dispatch, SPI, configuration |
| `jexunit-jupiter/` | `jexunit-jupiter` | JUnit 5 extension (recommended) |
| `jexunit-core/` | `jexunit-core` | JUnit 4 runner (legacy, no new features) |
| `examples-simple/` | — | JUnit 4 examples |
| `examples-simple-jupiter/` | — | JUnit 5 examples |
| `examples-complex/` | — | Additional JUnit 4 examples |

The example modules are only built with the `-Pexample-tests` Maven profile. Java source/target compatibility: **Java 8**.

## Package Layout (`com.jexunit.core`)

```
jexunit-base
com.jexunit.core
├── JExUnitConfig.java          — Configuration (defaults, jexunit.properties, programmatic overrides)
├── commands/
│   ├── annotation/
│   │   ├── TestCommand.java    — @TestCommand (repeatable; on methods or classes)
│   │   └── TestParam.java      — @TestParam for single column values
│   ├── validation/             — CommandValidator, ValidationType (IGNORE, WARN, FAIL)
│   ├── DefaultCommands.java    — Built-in command and parameter keywords
│   ├── TestCommandHost.java    — Runner-specific callbacks (implemented by the JUnit 4/5 layers)
│   ├── TestCommandRunner.java  — Finds and invokes the right @TestCommand method
│   └── TestCommandScanner.java — Classpath scanner for @TestCommand annotations
├── context/
│   ├── Context.java            — @Context injection annotation
│   ├── TestContext.java        — Key/value store shared by the commands of a test group
│   └── TestContextManager.java — Holds the TestContext of the current thread
├── data/
│   ├── TestObjectHelper.java   — Creates/populates Java objects from TestCase data
│   ├── OgnlUtils.java          — OGNL-based property setting/getting
│   └── PropertyUtils.java      — String-to-typed-value conversion (numbers, dates, enums, …)
├── dataprovider/
│   ├── ExcelDataProvider.java  — SPI DataProvider implementation for Excel
│   ├── ExcelFile.java          — @ExcelFile annotation
│   ├── ExcelLoader.java        — Apache POI parser; row-wise and transposed layouts
│   └── ExcelMetadata.java      — Metadata (worksheet, cell) attached to each loaded TestCase
├── model/
│   ├── TestCase.java           — A single command invocation with its parameters
│   ├── TestCell.java           — One cell: value + identifier (cell address, e.g. "B5")
│   └── Metadata.java           — Base class for test case metadata (test group, identifier)
└── spi/
    ├── ServiceRegistry.java    — Loads DataProvider implementations via ServiceLoader
    ├── BeforeSheet.java        — Hook run before the worksheets (configured via jexunit.properties)
    ├── AfterSheet.java         — Hook run after the worksheets (configured via jexunit.properties)
    └── data/DataProvider.java  — SPI for custom data sources

jexunit-jupiter
com.jexunit.core.jupiter
├── JExUnitExtension.java       — Loads the test data; one invocation per test group
├── ExcelTest.java              — @ExcelTest: the template method the invocations run for
├── JExUnitInterceptor.java     — Runs the command loop of a test group
└── JUnit5TestCommandHost.java  — TestCommandHost for the JUnit 5 test instance

jexunit-core
com.jexunit.core
├── JExUnit.java                — JUnit 4 Suite runner (entry point)
├── JExUnitBase.java            — Parameterized test; runs the command loop of a test group
└── junit/Parameterized.java    — Parameterized runner creating one test per test group
```

## Key Classes

### `JExUnitExtension` (JUnit 5)
A `TestTemplateInvocationContextProvider` for the `@ExcelTest` method. It asks the `ServiceRegistry` for a `DataProvider`, loads and validates the test data and provides one invocation per test group (worksheet, or row with `worksheetAsTest = false`). `JExUnitInterceptor` replaces the invocation of the (empty) `@ExcelTest` method by the command loop and reports all collected failures at the end. Command methods declared in the test class are invoked on the JUnit 5 test instance.

### `JExUnit` / `JExUnitBase` (JUnit 4)
`JExUnit` extends JUnit's `Suite`. It asks the `ServiceRegistry` for a `DataProvider` and creates one `Parameterized` runner per Excel file, which runs `JExUnitBase.test()` once per test group. Failures are collected with an `ErrorCollector`.

### `ExcelLoader`
Reads `.xlsx` files via Apache POI. Parses each row into a `TestCase`:
- Column A = command name
- The preceding `COMMAND` header row defines the keys for subsequent data rows (multiple commands per row are possible)
- Supports *transposed* layout (columns instead of rows) via `@ExcelFile(transpose = true)`

### `TestCommandRunner`
Uses `TestCommandScanner` results to find the `@TestCommand` method or class matching the current command name (case-insensitive). Resolves method parameters by type and annotation (see [test-commands.md](test-commands.md)).

### `TestObjectHelper`
Given a `TestCase` and a Java class, creates an instance and uses OGNL to set each column value as a property. Supports nested properties (`address.street`), lists (`items[0].name`), conditions (`items[id=5].name`), maps and enum conversion. See [object-mapping.md](object-mapping.md).

### `TestContext`
A `ConcurrentHashMap`-backed store for the current test group. Commands can share state by storing and retrieving objects by type or by string ID. See [test-context.md](test-context.md).

### `JExUnitConfig`
Holds the configuration: the defaults, overridden by `jexunit.properties` from the classpath (read once), and by values set via `JExUnitConfig.setConfigProperty(key, value)` / `registerConfig(...)`. See [configuration.md](configuration.md).

## SPI Extension Points

| Interface | Purpose | Registration |
|---|---|---|
| `com.jexunit.core.spi.data.DataProvider` | Custom data source (replaces or augments Excel) | `META-INF/services` (`ServiceLoader`) |
| `com.jexunit.core.spi.BeforeSheet` | Callback before the worksheets are executed | `jexunit.sheet.before` in `jexunit.properties` |
| `com.jexunit.core.spi.AfterSheet` | Callback after the worksheets are executed | `jexunit.sheet.after` in `jexunit.properties` |

See [spi.md](spi.md) for implementation details.

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JUnit Jupiter | 5.14.4 | Test engine API (`jexunit-jupiter`) |
| JUnit | 4.13.2 | Test runner base (`jexunit-core`) |
| Apache POI (OOXML) | 5.5.1 | Excel file parsing |
| OGNL | 3.4.16 | Object property mapping |
| Annotation Detector | 3.0.5 | Classpath scanning for `@TestCommand` |
| Lombok | 1.18.48 | Boilerplate reduction (compile-time only) |
