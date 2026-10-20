# Migrating from JUnit 4 to JUnit 5

The JUnit 4 runner (`jexunit-core`, `@RunWith(JExUnit.class)`) is deprecated and will be removed in a future major release. This guide shows how to move your tests to the JUnit 5 extension (`jexunit-jupiter`, `@ExtendWith(JExUnitExtension.class)`).

For most test classes the migration takes a few minutes: the Excel files, `@ExcelFile`, `@TestCommand`, `@TestParam`, `@Context`, `TestContext` and `jexunit.properties` stay the same. Plan more time for classes that override `runCommand(...)` (see [below](#classes-overriding-runcommand)).

## 1. Dependencies

Replace `jexunit-core` by `jexunit-jupiter`, and add the JUnit 5 engine to run the tests:

```xml
<dependency>
    <groupId>com.jexunit</groupId>
    <artifactId>jexunit-jupiter</artifactId>
    <version>${jexunit.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>${junit.jupiter.version}</version>
    <scope>test</scope>
</dependency>
```

(With JitPack, use the group ID `com.github.fhm84.jexunit`, see the [README](../README.md#how-to-use-jexunit-in-my-own-project).)

- Use the JUnit 5 version that `jexunit-jupiter` is built with (see `junit.jupiter.version` in the [pom.xml](../pom.xml)), or align all JUnit 5 artifacts with the `org.junit:junit-bom`.
- Use a current `maven-surefire-plugin` (3.x). It runs JUnit 5 tests without further configuration.

## 2. Migrating step by step (optional)

You don't have to migrate all test classes at once. `jexunit-core` and `jexunit-jupiter` can be used side by side. With the JUnit Vintage engine, the remaining JUnit 4 test classes run on the JUnit 5 platform together with the migrated ones:

```xml
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <version>${junit.jupiter.version}</version>
    <scope>test</scope>
</dependency>
```

Once all test classes are migrated, remove `jexunit-core` and `junit-vintage-engine`.

## 3. Migrating a test class

```java
// JUnit 4 (jexunit-core)                          // JUnit 5 (jexunit-jupiter)
@RunWith(JExUnit.class)                             @ExtendWith(JExUnitExtension.class)
public class ArithmeticalTest {                     public class ArithmeticalTest {

    @ExcelFile                                          @ExcelFile
    static String[] excelFiles = {                      static String[] excelFiles = {
        "src/test/resources/Arithmetical.xlsx"};            "src/test/resources/Arithmetical.xlsx"};

                                                        @ExcelTest
                                                        void test() {}

    @TestCommand("mul")                                 @TestCommand("mul")
    public static void mul(MyEntity e) { ... }          public static void mul(MyEntity e) { ... }
}                                                   }
```

1. Replace `@RunWith(JExUnit.class)` by `@ExtendWith(JExUnitExtension.class)` (`com.jexunit.core.jupiter.JExUnitExtension`).
2. Add exactly one (empty) method annotated with `@ExcelTest` (`com.jexunit.core.jupiter.ExcelTest`). The worksheets run as invocations of this method; its body is not executed.
3. Remove `extends JExUnitBase`, if present. No base class is needed.
4. Keep `@ExcelFile` and the `@TestCommand` methods as they are.
5. Migrate the JUnit 4 annotations and assertions of the class (see [section 4](#4-junit-4-annotations-and-assertions)).

### Classes overriding `runCommand`

Test classes extending `JExUnitBase` could override `runCommand(TestCase)` to handle commands without a `@TestCommand` implementation. There is no equivalent in JUnit 5: turn each command handled there into a `@TestCommand` method.

```java
// JUnit 4                                          // JUnit 5
@Override                                           @TestCommand("add")
public void runCommand(TestCase<?> testCase) {      public void add(TestCase<?> testCase) {
    switch (testCase.getTestCommand()) {                ...
        case "add": ...; break;                     }
        case "sub": ...; break;
    }                                               @TestCommand("sub")
}                                                   public void sub(TestCase<?> testCase) {
                                                        ...
                                                    }
```

> **Check this with `FAIL` validation.** A command without any `@TestCommand` implementation is *not valid* for the command validation. With the default validation type `WARN`, its rows are removed before the test runs, with only a warning in the log, so the test **passes without executing them**. While migrating, set `jexunit.command.validation.type=FAIL` in `jexunit.properties` to make such rows fail the test.

## 4. JUnit 4 annotations and assertions

| JUnit 4 | JUnit 5 |
|---|---|
| `@Before` / `@After` | `@BeforeEach` / `@AfterEach` |
| `@BeforeClass` / `@AfterClass` | `@BeforeAll` / `@AfterAll` (static) |
| `@Ignore` | `@Disabled` |
| `@Test` (additional plain test methods) | `@Test` (`org.junit.jupiter.api.Test`) |
| `org.junit.Assert.assertEquals(message, expected, actual)` | `org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message)` (the message is the **last** parameter) |
| `org.junit.Assume.assumeTrue(...)` | `org.junit.jupiter.api.Assumptions.assumeTrue(...)` |

Hamcrest's `assertThat` works with both.

These changes are not specific to JExUnit. For larger code bases, the [OpenRewrite](https://docs.openrewrite.org/recipes/java/testing/junit5/junit4to5migration) recipe `org.openrewrite.java.testing.junit5.JUnit4to5Migration` (artifact `org.openrewrite.recipe:rewrite-testing-frameworks`) automates most of them. The JExUnit-specific steps of [section 3](#3-migrating-a-test-class) remain manual.

> **Assumptions in test commands:** in a command, use JUnit 5's `Assumptions`. With JExUnit 0.7.0 and later, a failed assumption skips the worksheet. JUnit 4's `Assume` in a command run by JUnit 5 is reported as an unexpected exception (a failure).

## 5. Behaviour differences

| | JUnit 4 (`jexunit-core`) | JUnit 5 (`jexunit-jupiter`) |
|---|---|---|
| Non-static `@TestCommand` methods of the test class | invoked on a **separate** instance of the test class (cached in the `TestContext`); fields set in `@Before` are not visible | invoked on the JUnit 5 **test instance**; fields set in `@BeforeEach` are visible |
| Test instance per worksheet | — | a new instance per worksheet (JUnit 5's default lifecycle); share state between worksheets via `@TestInstance(PER_CLASS)` or static fields, not instance fields |
| `@Before`/`@BeforeEach` | only run around the worksheets when extending `JExUnitBase` | run before every worksheet |
| `BeforeSheet`/`AfterSheet` hooks (`jexunit.sheet.before`/`after`) | once per Excel file | once per test class |
| Configuration set programmatically | `@BeforeClass` runs *after* the Excel files are read; settings for reading them (date patterns, keywords, …) must be in `jexunit.properties` | `@BeforeAll` runs *before* the Excel files are read, so these settings apply |
| Test names | `test[File.xlsx - sheet1 [A2]]` (with the cell of the first command) | `[File.xlsx - sheet1]` |
| Disabled worksheets (`disabled = true`) | skipped (assumption failure) | skipped (aborted) |

Unchanged in both: the Excel format, the `TestContext` (a new one per worksheet), command discovery and validation, `jexunit.properties`, and the reporting of failures (all failures of a worksheet are collected and reported together).

## 6. Checklist

- [ ] `jexunit-jupiter` and `junit-jupiter-engine` added (and `junit-vintage-engine`, if migrating step by step)
- [ ] `@RunWith(JExUnit.class)` → `@ExtendWith(JExUnitExtension.class)`, plus one empty `@ExcelTest` method
- [ ] `extends JExUnitBase` removed; `runCommand(...)` overrides turned into `@TestCommand` methods
- [ ] JUnit 4 annotations, assertions and assumptions migrated
- [ ] Ran the tests once with `jexunit.command.validation.type=FAIL`
- [ ] Checked the [behaviour differences](#5-behaviour-differences): instance state in commands, hooks, programmatic configuration
- [ ] `jexunit-core` (and `junit-vintage-engine`) removed
