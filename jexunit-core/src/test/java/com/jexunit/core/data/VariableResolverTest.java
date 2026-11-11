package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.context.TestContext;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.data.entity.TestEnum;
import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.data.entity.TestModelSub;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class VariableResolverTest {

    private static final String ENABLED_KEY = JExUnitConfig.ConfigKey.VARIABLES_ENABLED.getKey();

    private TestContext context;

    @Before
    public void setUp() {
        context = new TestContext();
    }

    @After
    public void resetConfig() {
        JExUnitConfig.setConfigProperty(ENABLED_KEY, JExUnitConfig.ConfigKey.VARIABLES_ENABLED.getDefaultConfig());
        TestContextManager.reset();
    }

    @Test
    public void shouldKeepValuesWithoutVariables() {
        assertEquals("plain $ value {x}", VariableResolver.resolve("plain $ value {x}", "B2", context));
        assertNull(VariableResolver.resolve(null, "B2", context));
    }

    @Test
    public void shouldResolveVariables() {
        context.add("orderId", "4711").add("qty", 3);

        assertEquals("4711", VariableResolver.resolve("${orderId}", "B2", context));
        assertEquals("order 4711: 3 items", VariableResolver.resolve("order ${orderId}: ${ qty } items", "B2", context));
    }

    @Test
    public void shouldEscapeVariables() {
        context.add("orderId", "4711");

        assertEquals("${orderId}", VariableResolver.resolve("$${orderId}", "B2", context));
        assertEquals("${orderId} = 4711", VariableResolver.resolve("$${orderId} = ${orderId}", "B2", context));
    }

    @Test
    public void shouldConvertValuesLikeExcelCells() throws Exception {
        final SimpleDateFormat dateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        context.add("amount", new BigDecimal("1E+3"))
                .add("date", dateTime.parse("24.12.2026 00:00:00"))
                .add("dateTime", dateTime.parse("24.12.2026 18:30:00"))
                .add("enum", TestEnum.TYPE_B);

        assertEquals("1000", VariableResolver.resolve("${amount}", "B2", context));
        assertEquals("24.12.2026", VariableResolver.resolve("${date}", "B2", context));
        assertEquals("24.12.2026 18:30:00", VariableResolver.resolve("${dateTime}", "B2", context));
        assertEquals("TYPE_B", VariableResolver.resolve("${enum}", "B2", context));
    }

    @Test
    public void shouldResolvePropertyPaths() {
        final TestModelBase order = new TestModelBase();
        order.setStringAttr("Miller");
        final TestModelSub item = new TestModelSub();
        item.setIntAttr(3);
        order.getSubEntityListAttr().add(item);
        context.add("order", order);
        // stored by type: the class name is the id
        context.add(TestModelSub.class, item);

        assertEquals("Miller", VariableResolver.resolve("${order.stringAttr}", "B2", context));
        assertEquals("3", VariableResolver.resolve("${order.subEntityListAttr[0].intAttr}", "B2", context));
        assertEquals("3", VariableResolver.resolve("${order.subEntityListAttr[intAttr=3].intAttr}", "B2", context));
        assertEquals("3", VariableResolver.resolve("${" + TestModelSub.class.getName() + ".intAttr}", "B2", context));
    }

    @Test
    public void shouldResolveNullPropertiesWithoutCreatingThem() {
        final TestModelBase order = new TestModelBase();
        context.add("order", order);

        // a cell with only the variable has no value
        assertNull(VariableResolver.resolve("${order.subEntityAttr2.stringAttr}", "B2", context));
        assertEquals("name: ", VariableResolver.resolve("name: ${order.stringAttr}", "B2", context));
        // reading doesn't create the (sub-)entities
        assertNull(order.getSubEntityAttr2());
    }

    @Test
    public void shouldFailOnUnknownVariable() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> VariableResolver.resolve("id: ${orderId}", "C5", context));

        assertTrue(e.getMessage(), e.getMessage().contains("Unknown variable '${orderId}' in cell C5"));
    }

    @Test
    public void shouldFailOnUnclosedVariable() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> VariableResolver.resolve("id: ${orderId", "C5", context));

        assertTrue(e.getMessage(), e.getMessage().contains("Variable not closed in cell C5"));
    }

    @Test
    public void shouldFailOnUnreadableProperty() {
        context.add("order", new TestModelBase());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> VariableResolver.resolve("${order.subEntityListAttr[5].intAttr}", "C5", context));

        assertTrue(e.getMessage(), e.getMessage().contains("Variable '${order.subEntityListAttr[5].intAttr}' in cell "
                + "C5 cannot be read"));
    }

    @Test
    public void shouldOnlyAllowPropertyAccess() {
        context.add("order", new TestModelBase());

        assertThrows(IllegalArgumentException.class,
                () -> VariableResolver.resolve("${order.stringAttr2.toString()}", "C5", context));
    }

    @Test
    public void shouldResolveAllRowsOfTestCase() {
        TestContextManager.add("orderId", "4711");
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().put("order", new TestCell("B2", "${orderId}"));
        testCase.next();
        testCase.getValues().put("order", new TestCell("B3", "#${orderId}"));

        VariableResolver.resolve(testCase);

        assertEquals("4711", testCase.getMultilineValues().get(0).get("order").getValue());
        assertEquals("#4711", testCase.getMultilineValues().get(1).get("order").getValue());
    }

    @Test
    public void shouldNotResolveIfDisabled() {
        JExUnitConfig.setConfigProperty(ENABLED_KEY, "false");
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().put("order", new TestCell("B2", "${orderId}"));

        VariableResolver.resolve(testCase);

        assertEquals("${orderId}", testCase.getValues().get("order").getValue());
    }

}
