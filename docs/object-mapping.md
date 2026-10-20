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

Nested objects that are `null` are created automatically via their default constructor. Collection and map properties declared with an interface type get a default implementation: `List`/`Collection` → `ArrayList`, `Set` → `LinkedHashSet`, `SortedSet` → `TreeSet`, `Map` → `HashMap`, `SortedMap` → `TreeMap`. Indexing a list beyond its size (`items[3].name`) adds new elements up to that index.

In a condition like `items[id=5]`, the value is converted to the type of the property (numbers, booleans, enums, dates, …) before comparing.

> **Security note:** the column headers are evaluated as OGNL expressions, and OGNL can call public (static) methods. A header like `@java.lang.Runtime@getRuntime().exec('…').x` runs a program while the row is mapped, even though mapping the value fails afterwards. Treat Excel test files like test code: only run files from trusted sources. The cell *values* are not evaluated.

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
