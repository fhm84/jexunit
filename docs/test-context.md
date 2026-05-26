# TestContext

`TestContext` is a thread-safe, in-memory key/value store that lives for the duration of a single test run. It allows commands to share state across multiple rows in the same Excel worksheet without relying on static fields.

## Accessing TestContext

### Via Parameter Injection

Declare `TestContext` as a parameter in any `@TestCommand` method:

```java
@TestCommand("storeResult")
public void storeResult(TestCase<?> testCase, TestContext ctx) {
    ctx.add(MyResult.class, computeResult(testCase));
}

@TestCommand("assertResult")
public void assertResult(TestCase<?> testCase, TestContext ctx) {
    MyResult result = ctx.get(MyResult.class);
    assertThat(result.getValue(), equalTo(...));
}
```

### Via `@Context` Annotation

Annotate a parameter with `@Context` to inject a value from the context directly, without declaring `TestContext` itself:

```java
@TestCommand("assertResult")
public void assertResult(TestCase<?> testCase, @Context MyResult result) {
    assertThat(result.getValue(), equalTo(...));
}
```

## API Reference

### Storing Values

```java
// Store by type — only one instance per type allowed
ctx.add(MyEntity.class, entity);

// Store by string ID — allows multiple instances of the same type
ctx.add("primaryOrder", order1);
ctx.add("secondaryOrder", order2);
```

Both `add` methods return `this` for fluent chaining:

```java
ctx.add(User.class, user)
   .add("token", authToken)
   .add("sessionId", sessionId);
```

### Retrieving Values

```java
// Retrieve by type
MyEntity entity = ctx.get(MyEntity.class);

// Retrieve by type + string ID
Order primary = ctx.get(Order.class, "primaryOrder");
```

Returns `null` if no value is found for the given key; does not throw.

### Clearing the Context

```java
ctx.clear();
```

Removes all entries. Useful inside a setup command to reset state at the beginning of a test group.

## `@Context` Annotation

The `@Context` annotation can be applied to method parameters in `@TestCommand` methods. The framework resolves the value from the current `TestContext` automatically.

```java
// Inject by type (uses the class name as key internally)
@Context MyEntity entity

// Inject by string ID
@Context("myId") MyEntity entity
```

The annotation is also supported on fields if you extend `JExUnitBase`.

## Lifetime

`TestContext` is scoped to a single **test group**:
- When `worksheetAsTest = true` (default) a new context is created for each worksheet.
- When `worksheetAsTest = false` a new context is created for each individual command row.

There is one `TestContext` instance per test group; commands within the same group share the same instance.

## Implementation Notes

The store is backed by a `ConcurrentHashMap`. Keys are either the fully-qualified class name (when stored by type) or the user-supplied string ID. Values stored by type can only have one entry per type — a second `add` for the same type overwrites the first.
