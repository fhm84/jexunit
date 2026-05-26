# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build and run all tests (includes examples)
mvn clean verify -Pexample-tests

# Build and test core module only
mvn clean verify

# Run tests for a specific module
mvn test -f core/pom.xml

# Build without running tests
mvn clean install -DskipTests

# Run a single test class
mvn test -f core/pom.xml -Dtest=ExcelLoaderTest

# Run a single test method
mvn test -f core/pom.xml -Dtest=ExcelLoaderTest#testMethodName
```

## Module Structure

- **`core/`** — The framework library (the main deliverable)
- **`examples-simple/`** — Example test implementations (always included)
- **`examples-complex/`** — Additional examples (only with `-Pexample-tests` Maven profile)

Java source compatibility target is **Java 8**.

## Architecture

JExUnit is a JUnit 4 extension that lets tests be defined in Excel spreadsheets. Each row in an Excel worksheet maps to a "test command" that is dispatched to an annotated Java method.

### Data Flow

```
@RunWith(JExUnit.class) test class
  → ServiceRegistry discovers ExcelDataProvider (SPI)
  → ExcelLoader reads .xlsx files (specified via @ExcelFile)
  → Each Excel row → TestCase object (command name + cells)
  → JExUnitBase runs as parameterized test per TestCase
  → TestCommandRunner finds matching @TestCommand method via reflection
  → TestObjectHelper maps TestCell values to Java objects (OGNL)
  → Command method invoked; ErrorCollector aggregates failures
```

### Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `JExUnit` | `core/.../JExUnit.java` | JUnit Suite runner — entry point |
| `JExUnitBase` | `core/.../JExUnitBase.java` | Parameterized test base class; holds the test loop |
| `ExcelDataProvider` | `core/.../dataprovider/` | SPI implementation; delegates to ExcelLoader |
| `ExcelLoader` | `core/.../dataprovider/` | Apache POI-based Excel parser; supports row-wise and transposed (column-wise) layouts |
| `TestCommandScanner` | `core/.../commands/` | Scans classpath for `@TestCommand`-annotated methods |
| `TestObjectHelper` | `core/.../data/` | Creates/populates Java objects from TestCell map via OGNL |
| `TestContext` | `core/.../context/` | Fluent, thread-safe store for sharing data between command invocations |
| `JExUnitConfig` | `core/...` | Configuration via Apache Commons Configuration (reads `jexunit.properties`) |

### Key Annotations

| Annotation | Target | Purpose |
|-----------|--------|---------|
| `@RunWith(JExUnit.class)` | Test class | Activates the framework |
| `@ExcelFile` | Test class | Specifies the Excel file(s) to load |
| `@TestCommand` | Method | Marks a method as a dispatchable command; `value` is the command name used in Excel |
| `@TestParam` | Parameter | Maps a named Excel column to a method parameter |

### SPI Extension Points

`core/src/main/resources/META-INF/services/` contains service descriptors. Custom `DataProvider`, `BeforeSheet`, or `AfterSheet` implementations are discovered via Java's `ServiceLoader`.

## CI

GitHub Actions workflows in `.github/workflows/`:
- **`maven.yml`** — runs on every push/PR; executes `mvn clean verify -Pexample-tests` on JDK 8
- **`release.yml`** — runs on the `release` branch; uses Maven release plugin and publishes to GitHub Packages