# Roadmap

## Released

### Code audit fixes, list mapping, multiline commands per block (v0.7.0)

Fixes from a full code audit (data loss when reading Excel values, `TestContext` lifetime, expected exceptions, runner error reporting, OGNL headers restricted to property access), filling lists from Excel (#4), multiline commands with multiple commands per row (#28), and a [migration guide](migration-junit5.md) from JUnit 4 to JUnit 5. See the release notes of v0.7.0 for the behaviour changes to check when upgrading.

### JUnit 5 support (v0.6.0)

Added a `jexunit-jupiter` module with a native JUnit 5 extension. JUnit 4 support remains in `jexunit-core` (legacy, no new features).

**Migration:** See the [migration guide](migration-junit5.md).

---

## Optional / Future

### JUnit 6 / Java 17 module (`jexunit-jupiter6`)

JUnit 6 (stable since Feb 2026) targets Java 17+ and evolves the same `org.junit.jupiter` API. A future `jexunit-jupiter6` module could exploit Java 17+ language features:

- **Records** for `TestCase` / `TestCell` model classes (immutable, compact syntax)
- **`@ParameterizedClass`** hooks (if JUnit 6 exposes them) for cleaner sheet-per-class parameterization
- **Sealed interfaces** for command dispatch result types
- **Kotlin coroutines** support via JUnit 6 extension points (if added upstream)
- Raise source compatibility from Java 8 to Java 17

This would be a new Maven module alongside `jexunit-jupiter` (which stays on Java 8 / JUnit 5), so existing users are not broken.

**Prerequisites before starting:**
1. Confirm JUnit 6 API stability and finalized extension points
2. Decide minimum Java version for `jexunit-jupiter6` (17 or 21)
3. Evaluate whether `jexunit-jupiter` and `jexunit-jupiter6` need a shared base or can stay independent
