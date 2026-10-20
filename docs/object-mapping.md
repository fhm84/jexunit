# Object Mapping (OGNL)

JExUnit uses [OGNL](https://github.com/orphan-oss/ognl) (Object-Graph Navigation Language) to map Excel column values to Java object properties. This happens automatically when you declare a non-framework type as a parameter in a `@TestCommand` method, and can also be triggered manually via `TestObjectHelper`.

## Automatic Mapping

Declare your entity as a method parameter. JExUnit creates a new instance (via the default constructor) and sets each Excel column value as a property:

```java
@TestCommand("createOrder")
public void createOrder(TestCase<?> testCase, Order order) {
    // order is already populated:
    // order.getCustomerName() == value of the "customerName" column
    // order.getAmount()       == value of the "amount" column (converted to the field type)
}
```

Column names in the Excel header row must match the property path of the object.

## Property Path Syntax

OGNL supports the full dot-notation for nested objects:

| Excel column header | Java property accessed |
|---|---|
| `name` | `obj.setName(value)` |
| `address.street` | `obj.getAddress().setStreet(value)` |
| `items[0].name` | `obj.getItems().get(0).setName(value)` |
| `items[id=5].name` | `setName(value)` on the element of `obj.getItems()` whose `id` equals `5` |
| `tags[0]` | `obj.getTags().set(0, value)` (converted to the element type, e.g. `List<Integer>`) |
| `map.key` | `obj.getMap().put("key", value)` |

Nested objects that are `null` are created automatically via their default constructor, at every level of the path. Collection and map properties declared with an interface type get a default implementation: `List`/`Collection` → `ArrayList`, `Set` → `LinkedHashSet`, `SortedSet` → `TreeSet`, `Map` → `HashMap`, `SortedMap` → `TreeMap`.

In a condition like `items[id=5]`, the value is converted to the type of the property (numbers, booleans, enums, dates, …) before comparing.

### Collections

| Type | Supported |
|---|---|
| `List` (of entities) | Indexing beyond the size (`items[3].name`) adds new elements of the element type up to that index (they need a default constructor). Works for lists declared in a superclass and in sub-entities (`customer.addresses[0].street`) |
| `List` (of simple values) | `tags[0]`, `numbers[2]`: missing elements up to the index are filled with `null`, the value is converted to the element type (`String`, numbers, `Date`, enums, …) |
| `Set` | Only conditions on existing elements (`items[name=a].qty`); a set has no order, so there is no index access |
| `Map` | Simple values (`map.key`). Entity values are **not** created: `map.key.name` only works if the map already contains an entry for `key` |
| Arrays | **Not** created or enlarged: `array[0].name` / `array[0]` only work for an existing array, within its length |

The element type is taken from the generic type of the property (e.g. `List<Item>`). For raw or otherwise unresolvable types (`List`, `List<T>`), new list elements are `null` and simple values are not converted.

> **Security note:** the column headers are evaluated as OGNL expressions. By default, only property access is allowed in them (as in the table above); headers with method calls, static access (e.g. `@java.lang.Runtime@getRuntime().exec('…').x`), constructors or assignments are rejected and the row fails. If you need full OGNL expressions, set `jexunit.ognl.property_access_only=false` — OGNL can then call any public (static) method, so only run Excel files from trusted sources. The cell *values* are never evaluated.

## Manual Mapping via `TestObjectHelper`

### Create a new instance and populate it

```java
@TestCommand("myCmd")
public void myCmd(TestCase<?> testCase) throws Exception {
    MyEntity entity = TestObjectHelper.createObject(testCase, MyEntity.class);
    // entity is populated from all column values in the current row
}
```

### Populate an existing instance (partial update)

```java
MyEntity existing = ctx.get(MyEntity.class);
TestObjectHelper.createObject(testCase, existing);
// Only columns present in the current row are updated
```

### Read a single column value

```java
String rawValue = TestObjectHelper.getPropertyByKey(testCase, "columnName");
```

### Read an object property

```java
Object value = TestObjectHelper.getProperty(myEntity, "address.street");
```

### Convert a string to a typed value

```java
Object typed = TestObjectHelper.convertPropertyStringToObject(Double.class, "3.14");
```

## Supported Type Conversions

`PropertyUtils` handles the following target types automatically:

| Target type | Conversion |
|---|---|
| `String` | Identity |
| `int`, `Integer` | Integral value (e.g. `5`, `5.0`); values with a fraction or out of range are rejected |
| `long`, `Long` | Integral value (e.g. `5`, `5.0`); values with a fraction or out of range are rejected |
| `double`, `Double` | `Double.parseDouble` |
| `float`, `Float` | `Float.parseFloat` |
| `boolean`, `Boolean` | `Boolean.parseBoolean` |
| `Date` | Parsed with `jexunit.dateTimePattern`, else `jexunit.datePattern`; the whole value must match |
| `BigDecimal` | `new BigDecimal(value)` |
| Enum | `Enum.valueOf(type, value)` |

Dates use the patterns configured in `jexunit.properties` — see [configuration.md](configuration.md).

## Multiple Entities per Command

You can declare multiple entity parameters as long as their types differ:

```java
@TestCommand("transfer")
public void transfer(TestCase<?> testCase, SourceAccount source, TargetAccount target) {
    // Both are populated from the same row's columns
    // Column names must be unique and unambiguously match each entity's properties
}
```

If two entities share a property name (e.g. both have a `name` field), both receive the same column value.
