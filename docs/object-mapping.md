# Object Mapping (OGNL)

JExUnit uses [OGNL](https://commons.apache.org/proper/commons-ognl/) (Object-Graph Navigation Language) to map Excel column values to Java object properties. This happens automatically when you declare a non-framework type as a parameter in a `@TestCommand` method, and can also be triggered manually via `TestObjectHelper`.

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

Nested objects must already be initialised (not `null`) before OGNL tries to set them. If `order.getAddress()` returns `null`, setting `address.street` will throw. Initialise child objects in the parent constructor or use a custom factory.

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
| `int`, `Integer` | `Integer.parseInt` |
| `long`, `Long` | `Long.parseLong` |
| `double`, `Double` | `Double.parseDouble` |
| `float`, `Float` | `Float.parseFloat` |
| `boolean`, `Boolean` | `Boolean.parseBoolean` |
| `Date` | Parsed with `jexunit.datePattern` or `jexunit.dateTimePattern` |
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
