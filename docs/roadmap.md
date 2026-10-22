# Roadmap

Planned work is tracked as [GitHub issues](https://github.com/fhm84/jexunit/issues), grouped by release milestones. An issue's state is its status; this page is only the overview.

## Planned

**Java versions** (decided in [#46](https://github.com/fhm84/jexunit/issues/46)): **1.x requires Java 21**. **0.x stays the Java 8 line** and only gets bug fixes and security updates after 0.8.0. JUnit 6 and Apache POI 6 both need Java 17+, and POI is used by `jexunit-base`, so the whole framework moves instead of adding a separate `jexunit-jupiter6` module (the previous plan).

### [0.8.0](https://github.com/fhm84/jexunit/milestone/1): last feature release on Java 8

- Migrate to OGNL 3.4 ([#40](https://github.com/fhm84/jexunit/issues/40))
- Field injection for command classes: implement or remove ([#37](https://github.com/fhm84/jexunit/issues/37))
- Overloaded command methods ([#3](https://github.com/fhm84/jexunit/issues/3))
- Mark the JUnit 4 runner as deprecated ([#48](https://github.com/fhm84/jexunit/issues/48))
- Variables in cells: `${name}` placeholders from the `TestContext` ([#56](https://github.com/fhm84/jexunit/issues/56))
- Built-in `verify` command: compare object properties with the expected values ([#57](https://github.com/fhm84/jexunit/issues/57))
- Dependency injection for command classes (instance factory SPI) ([#60](https://github.com/fhm84/jexunit/issues/60))
- Test names with cell and command for `worksheetAsTest = false` ([#62](https://github.com/fhm84/jexunit/issues/62))

### [1.0.0](https://github.com/fhm84/jexunit/milestone/2): Java 21, Maven Central

- Set up the 0.x maintenance line ([#52](https://github.com/fhm84/jexunit/issues/52))
- Raise the minimum Java version to 21 ([#54](https://github.com/fhm84/jexunit/issues/54))
- Upgrade to Apache POI 6 ([#47](https://github.com/fhm84/jexunit/issues/47)) and JUnit 6 ([#53](https://github.com/fhm84/jexunit/issues/53))
- Remove the JUnit 4 runner `jexunit-core` ([#49](https://github.com/fhm84/jexunit/issues/49))
- Object mapping: records via the canonical constructor ([#61](https://github.com/fhm84/jexunit/issues/61))
- Change the groupId to `io.github.fhm84` ([#55](https://github.com/fhm84/jexunit/issues/55)); the Java packages stay `com.jexunit.*`
- Release on Maven Central ([#12](https://github.com/fhm84/jexunit/issues/12)). Until then, JExUnit is available via JitPack (no authentication) and GitHub Packages, see the README

### [Later](https://github.com/fhm84/jexunit/milestone/3)

- Object mapping: create entity values in maps and enlarge arrays ([#50](https://github.com/fhm84/jexunit/issues/50))
- Write the test results back into a copy of the workbook ([#58](https://github.com/fhm84/jexunit/issues/58))
- Generate a command catalog and an Excel template from the `@TestCommand` methods ([#59](https://github.com/fhm84/jexunit/issues/59))
- Reviewable test files: text diff for `.xlsx` and a CSV `DataProvider` ([#63](https://github.com/fhm84/jexunit/issues/63))
- Run selected sheets only (`jexunit.sheets.include` / `exclude`) ([#64](https://github.com/fhm84/jexunit/issues/64))

### Java 21 ideas

Possible once 1.x is on Java 21 (#54), each in its own PR:

- **Records** for `TestCase` / `TestCell` model classes (immutable, compact syntax); records as command parameters are #61
- **`@ParameterizedClass`** hooks (JUnit 6) for cleaner sheet-per-class parameterization
- **Sealed interfaces** and **pattern matching for `switch`** for command dispatch result types

---

## Released

### Code audit fixes, list mapping, multiline commands per block (v0.7.0)

Fixes from a full code audit (data loss when reading Excel values, `TestContext` lifetime, expected exceptions, runner error reporting, OGNL headers restricted to property access), filling lists from Excel (#4), multiline commands with multiple commands per row (#28), and a [migration guide](migration-junit5.md) from JUnit 4 to JUnit 5. See the release notes of v0.7.0 for the behaviour changes to check when upgrading.

### JUnit 5 support (v0.6.0)

Added a `jexunit-jupiter` module with a native JUnit 5 extension. JUnit 4 support remains in `jexunit-core` (legacy, no new features).

**Migration:** See the [migration guide](migration-junit5.md).
