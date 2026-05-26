# Excel File Format

JExUnit reads `.xlsx` files (Excel 2007+). Each worksheet normally maps to one test group. The first column drives execution; subsequent columns carry data.

## Basic Structure

| Column A | Column B | Column C | … |
|---|---|---|---|
| *(blank)* | comments, ignored | | |
| `COMMAND` | `param1` | `param2` | header row: defines column keys |
| `myCommand` | `value1` | `value2` | data row: invokes `myCommand` |
| `myCommand` | `value3` | `value4` | invokes `myCommand` again |
| `report` | some message | | built-in command |

### Rules

- A row whose **first column is blank** is ignored — use this for free-form comments.
- The **`COMMAND` keyword** (case-insensitive) resets and redefines the column headers for all following data rows. You can have multiple `COMMAND` headers in one worksheet to switch parameter layouts.
- All built-in keywords and command names are **case-insensitive**.
- Once a `COMMAND` header row is set, subsequent rows with a non-blank first column are dispatched as commands.

## Built-in Commands

These are handled by the framework and do not require any `@TestCommand` method.

### `disabled`

Placed as a row command with `true` in the next column to **disable the entire worksheet**. The worksheet is skipped and reported as ignored.

```
disabled | true
```

### `report`

Logs the content of the subsequent columns to the test output. Useful for annotating the Excel file output with human-readable messages.

```
report | Starting user creation tests
```

### `comment` (parameter)

When a data row includes a column named `comment`, its value is used as the assertion message if the command fails — equivalent to the `message` argument in JUnit `assertThat`.

```
COMMAND | param1 | param2 | result | comment
add     | 1      | 1      | 2      | basic addition
```

## Special Parameters (usable in any data row)

These column names are reserved and handled by the framework.

### `exception`

If `true`, the framework expects the command to throw any exception. The test **passes** if an exception is thrown and **fails** if no exception is thrown — analogous to `@Test(expected = Exception.class)`.

```
COMMAND | param1 | param2 | result | exception
div     | 1      | 0      |        | true
```

### `fastFail`

If `true`, the test group is aborted immediately when this command fails, instead of collecting the error and running subsequent commands. Overrides the `fastFail` attribute on the `@TestCommand` annotation.

```
COMMAND | param1 | param2 | result | fastFail
createUser | Max  | Muster |        | true
```

### `breakpoint`

Setting `breakpoint` to `true` on a row sets `testCase.isBreakpointEnabled() == true` when the command is invoked. Use this to place a conditional breakpoint in the command implementation for targeted debugging:

```java
@TestCommand("myCmd")
public void myCmd(TestCase<?> testCase) {
    if (testCase.isBreakpointEnabled()) {
        // Set a breakpoint on the next line in your IDE
        System.out.println("Breakpoint hit");
    }
    // ... test logic
}
```

### `multiline`

Marks a command as *multiline*, meaning multiple consecutive rows sharing the same command name are collected and delivered together. See the [Multiline Commands](#multiline-commands) section below.

## Multiline Commands

Sometimes a single logical command needs multiple rows of data (e.g. creating an entity with a list of child items). Multiline mode collects those rows and passes them all to the command at once.

### Enabling Multiline for Specific Commands (via properties)

```properties
# jexunit.properties — comma-separated list of command names
jexunit.multiline_commands=createPerson,createOrder
```

Any command listed here is always treated as multiline.

### Enabling Multiline Per Row

Add a `multiline` column and set it to `true` for any row that should start or continue a multiline block.

### Accessing Multiline Data

```java
@TestCommand("createPerson")
public void createPerson(TestCase<?> testCase) {
    if (testCase.isMultiline()) {
        List<Map<String, TestCell>> rows = testCase.getMultilineValues();
        for (Map<String, TestCell> row : rows) {
            String firstname = row.get("firstname").getValue();
            // ...
        }
    }
}
```

`getMultilineValues()` also works for single-line commands — it returns a list with one entry (the current row), so you can write command implementations that handle both cases uniformly.

## Transposed Layout (Column-Wise)

Normally rows = commands. Setting `transpose = true` on `@ExcelFile` rotates the layout so that **columns = commands** and rows = parameters. This suits data that is more naturally expressed column-by-column.

```java
@ExcelFile(transpose = true)
static String[] excelFiles = {"src/test/resources/TransposedData.xlsx"};
```

In transposed mode:
- The first **row** plays the role of column A (command names / header).
- The first **column** plays the role of the header row (parameter keys).

## `worksheetAsTest` Flag

```java
@ExcelFile(worksheetAsTest = false)
static String[] excelFiles = {"src/test/resources/MassTests.xlsx"};
```

| `worksheetAsTest` | JUnit test granularity |
|---|---|
| `true` (default) | One JUnit test per **worksheet** |
| `false` | One JUnit test per **command row** |

Use `false` when you want each row to show up as an individual test case in your IDE's test runner.

## Customising Built-in Keyword Names

All built-in command and parameter names can be changed in `jexunit.properties`. See [configuration.md](configuration.md) for the full list of keys.

Example — prefix all built-in commands with `jx.`:

```properties
jexunit.defaultcommand_prefix=jx.
```

This makes the framework expect `jx.disabled`, `jx.report`, etc. instead of `disabled` and `report`.
