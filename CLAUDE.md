# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build and run all tests (includes examples)
mvn clean verify -Pexample-tests

# Build only the framework modules (no examples)
mvn clean verify

# Run tests for a specific module
mvn test -f jexunit-core/pom.xml
mvn test -f jexunit-jupiter/pom.xml

# Build without running tests
mvn clean install -DskipTests

# Run a single test class
mvn test -f jexunit-core/pom.xml -Dtest=ExcelLoaderTest

# Run a single test method
mvn test -f jexunit-core/pom.xml -Dtest=ExcelLoaderTest#testMethodName
```

## Module Structure

- **`jexunit-base/`** — JUnit-agnostic shared kernel: Excel parsing, OGNL mapping, command dispatch, SPI, configuration
- **`jexunit-core/`** — JUnit 4 runner layer (legacy); depends on `jexunit-base`
- **`jexunit-jupiter/`** — JUnit 5 Jupiter extension; depends on `jexunit-base`
- **`examples-simple/`** — JUnit 4 example tests (always included)
- **`examples-simple-jupiter/`** — JUnit 5 example tests (always included)
- **`examples-complex/`** — Additional JUnit 4 examples (only with `-Pexample-tests` Maven profile)

Java source compatibility target is **Java 8**.

## Architecture

JExUnit lets tests be defined in Excel spreadsheets. Each row maps to a "test command" dispatched to an annotated Java method. It supports both JUnit 4 (legacy) and JUnit 5 (recommended).

### Data Flow

```
JUnit 4 (legacy):                         JUnit 5 (recommended):
@RunWith(JExUnit.class) test class         @ExtendWith(JExUnitExtension.class) test class
  → JExUnit (Suite runner)                   → JExUnitExtension (TestTemplateInvocationContextProvider)
                    ↘                                           ↙
              ServiceRegistry discovers ExcelDataProvider (SPI)
              ExcelLoader reads .xlsx files (specified via @ExcelFile)
              Each Excel row → TestCase object (command name + cells)
                    ↓
              TestCommandRunner finds matching @TestCommand method via reflection
              TestObjectHelper maps TestCell values to Java objects (OGNL)
              Command method invoked; errors aggregated and reported
```

### Key Classes

| Class | Module | Purpose |
|-------|--------|---------|
| `JExUnit` | `jexunit-core` | JUnit 4 Suite runner — legacy entry point |
| `JExUnitBase` | `jexunit-core` | JUnit 4 parameterized base class — legacy |
| `JExUnitExtension` | `jexunit-jupiter` | JUnit 5 extension — recommended entry point |
| `ExcelDataProvider` | `jexunit-base` | SPI implementation; delegates to ExcelLoader |
| `ExcelLoader` | `jexunit-base` | Apache POI-based Excel parser; supports row-wise and transposed layouts |
| `TestCommandScanner` | `jexunit-base` | Scans classpath for `@TestCommand`-annotated methods |
| `TestObjectHelper` | `jexunit-base` | Creates/populates Java objects from TestCell map via OGNL |
| `TestContext` | `jexunit-base` | Fluent, thread-safe store for sharing data between command invocations |
| `JExUnitConfig` | `jexunit-base` | Configuration via Apache Commons Configuration (reads `jexunit.properties`) |

### Key Annotations

| Annotation | Target | Purpose |
|-----------|--------|---------|
| `@RunWith(JExUnit.class)` | Test class | **JUnit 4** — activates the framework (legacy) |
| `@ExtendWith(JExUnitExtension.class)` | Test class | **JUnit 5** — activates the framework (recommended) |
| `@ExcelFile` | Field / Method | Specifies the Excel file(s) to load |
| `@ExcelTest` | Method | **JUnit 5** — marks the single template entry-point method |
| `@TestCommand` | Method / Class | Marks a method as a dispatchable command; `value` is the Excel command name |
| `@TestParam` | Parameter / Field | Maps a named Excel column to a method parameter |

### Migration: JUnit 4 → JUnit 5

```java
// JUnit 4 (legacy)                       // JUnit 5 (recommended)
@RunWith(JExUnit.class)                    @ExtendWith(JExUnitExtension.class)
public class MyTest extends JExUnitBase {  public class MyTest {
    @ExcelFile                                 @ExcelFile
    static String file = "tests.xlsx";        static String file = "tests.xlsx";
                                               @ExcelTest
                                               void test() {}
    @TestCommand("cmd")                        @TestCommand("cmd")
    void cmd(...) { ... }                      void cmd(...) { ... }
}                                          }
```

Key differences: no base class needed; `@ExcelTest` replaces the inherited `test()` method; `@Before`/`@After` become standard JUnit 5 `@BeforeEach`/`@AfterEach`.

### SPI Extension Points

`jexunit-base/src/main/resources/META-INF/services/` contains service descriptors. Custom `DataProvider`, `BeforeSheet`, or `AfterSheet` implementations are discovered via Java's `ServiceLoader`.

## CI

GitHub Actions workflows in `.github/workflows/`:
- **`maven.yml`** — runs on every push/PR; executes `mvn clean verify -Pexample-tests` on JDK 8, 11, and 21
- **`release.yml`** — runs on the `release` branch; uses Maven release plugin and publishes to GitHub Packages