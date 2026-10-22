# Roadmap

Planned work is tracked as [GitHub issues](https://github.com/fhm84/jexunit/issues), grouped by release milestones. An issue's state is its status; this page is only the overview.

## Planned

### [0.8.0](https://github.com/fhm84/jexunit/milestone/1): maintenance on the Java 8 line

- Decide on a Java 17 baseline: JUnit 6 and Apache POI 6 both need Java 17 ([#46](https://github.com/fhm84/jexunit/issues/46))
- Migrate to OGNL 3.4 ([#40](https://github.com/fhm84/jexunit/issues/40))
- Field injection for command classes: implement or remove ([#37](https://github.com/fhm84/jexunit/issues/37))
- Overloaded command methods ([#3](https://github.com/fhm84/jexunit/issues/3))
- Mark the JUnit 4 runner as deprecated ([#48](https://github.com/fhm84/jexunit/issues/48))

### [1.0.0](https://github.com/fhm84/jexunit/milestone/2): API clean-up

- Remove the JUnit 4 runner `jexunit-core` ([#49](https://github.com/fhm84/jexunit/issues/49))
- Upgrade to Apache POI 6, depends on #46 ([#47](https://github.com/fhm84/jexunit/issues/47))

### [Later](https://github.com/fhm84/jexunit/milestone/3)

- Object mapping: create entity values in maps and enlarge arrays ([#50](https://github.com/fhm84/jexunit/issues/50))
- Release on Maven Central ([#12](https://github.com/fhm84/jexunit/issues/12)). Until then, JExUnit is available via JitPack (no authentication) and GitHub Packages, see the README

### Java 17 ideas

If #46 moves JExUnit to Java 17, these become possible. A separate `jexunit-jupiter6` module next to the Java 8 modules (the previous plan) wouldn't be enough: POI 6 is used by `jexunit-base`, which all modules share.

- **Records** for `TestCase` / `TestCell` model classes (immutable, compact syntax)
- **`@ParameterizedClass`** hooks (if JUnit 6 exposes them) for cleaner sheet-per-class parameterization
- **Sealed interfaces** for command dispatch result types

---

## Released

### Code audit fixes, list mapping, multiline commands per block (v0.7.0)

Fixes from a full code audit (data loss when reading Excel values, `TestContext` lifetime, expected exceptions, runner error reporting, OGNL headers restricted to property access), filling lists from Excel (#4), multiline commands with multiple commands per row (#28), and a [migration guide](migration-junit5.md) from JUnit 4 to JUnit 5. See the release notes of v0.7.0 for the behaviour changes to check when upgrading.

### JUnit 5 support (v0.6.0)

Added a `jexunit-jupiter` module with a native JUnit 5 extension. JUnit 4 support remains in `jexunit-core` (legacy, no new features).

**Migration:** See the [migration guide](migration-junit5.md).
