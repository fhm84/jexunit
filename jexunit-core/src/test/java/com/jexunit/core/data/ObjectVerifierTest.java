package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.data.entity.TestEnum;
import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.data.entity.TestModelSub;
import com.jexunit.core.model.TestCell;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ObjectVerifierTest {

    @After
    public void resetConfig() {
        JExUnitConfig.setConfigProperty(JExUnitConfig.ConfigKey.VERIFY_NULL.getKey(),
                JExUnitConfig.ConfigKey.VERIFY_NULL.getDefaultConfig());
    }

    @Test
    public void shouldMatchConvertedValues() {
        final TestModelBase model = model();

        final List<String> mismatches = ObjectVerifier.verify(model, expected(
                "stringAttr", "Miller",
                "intAttr", "3",
                "doubleAttr", "2.50",
                "booleanAttr", "TRUE",
                "enumAttr", "TYPE_B",
                "subEntityListAttr[0].intAttr", "7",
                "subEntityListAttr[intAttr=7].stringAttr", "first",
                "mapAttr.key", "value"));

        assertEquals(Collections.emptyList(), mismatches);
    }

    @Test
    public void shouldReportAllMismatchesWithCell() {
        final TestModelBase model = model();

        final List<String> mismatches = ObjectVerifier.verify(model, expected(
                "stringAttr", "Müller",
                "intAttr", "4",
                "booleanAttr", "yes",
                "enumAttr", "TYPE_X"));

        assertEquals(Arrays.asList(
                "stringAttr (B1): expected \"Müller\" but was \"Miller\"",
                "intAttr (B2): expected \"4\" but was \"3\"",
                "booleanAttr (B3): expected \"yes\" but was \"true\"",
                "enumAttr (B4): expected \"TYPE_X\" but was \"TYPE_B\""), mismatches);
    }

    @Test
    public void shouldSkipEmptyCells() {
        assertEquals(Collections.emptyList(), ObjectVerifier.verify(model(), expected("stringAttr", "")));
    }

    @Test
    public void shouldCheckNullValue() {
        final TestModelBase model = model();

        assertEquals(Collections.emptyList(), ObjectVerifier.verify(model, expected(
                "stringAttr2", "<null>",
                // a null in the middle of the path
                "subEntityAttr2.stringAttr", "<null>")));
        assertEquals(Arrays.asList(
                "stringAttr (B1): expected <null> but was \"Miller\"",
                "stringAttr2 (B2): expected \"x\" but was <null>"),
                ObjectVerifier.verify(model, expected("stringAttr", "<null>", "stringAttr2", "x")));
        // reading doesn't create the (sub-)entities
        assertNull(model.getSubEntityAttr2());
    }

    @Test
    public void shouldUseConfiguredNullValue() {
        JExUnitConfig.setConfigProperty(JExUnitConfig.ConfigKey.VERIFY_NULL.getKey(), "#null");

        assertEquals(Collections.emptyList(), ObjectVerifier.verify(model(), expected("stringAttr2", "#null")));
    }

    @Test
    public void shouldReportMissingCollectionElementsAsMismatch() {
        final TestModelBase model = model();

        final List<String> mismatches = ObjectVerifier.verify(model, expected(
                "subEntityListAttr[5].intAttr", "1",
                "subEntityListAttr[intAttr=8].stringAttr", "x"));

        assertEquals(Arrays.asList(
                "subEntityListAttr[5].intAttr (B1): expected \"1\" but there is no element subEntityListAttr[5]",
                "subEntityListAttr[intAttr=8].stringAttr (B2): expected \"x\" but there is no element "
                        + "subEntityListAttr[intAttr=8]"), mismatches);
        // the list is not extended
        assertEquals(1, model.getSubEntityListAttr().size());
    }

    @Test
    public void shouldFailOnUnreadableProperties() {
        final IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> ObjectVerifier.verify(model(), expected("unknownAttr", "1")));
        assertTrue(unknown.getMessage(), unknown.getMessage().startsWith(
                "The property unknownAttr (B1) of the object cannot be read"));

        final IllegalArgumentException method = assertThrows(IllegalArgumentException.class,
                () -> ObjectVerifier.verify(model(), expected("stringAttr.toString()", "Miller")));
        assertTrue(method.getMessage(), method.getMessage().contains("only property access is supported"));
    }

    private static TestModelBase model() {
        final TestModelBase model = new TestModelBase();
        model.setStringAttr("Miller");
        model.setIntAttr(3);
        model.setDoubleAttr(2.5);
        model.setBooleanAttr(true);
        model.setEnumAttr(TestEnum.TYPE_B);
        model.getMapAttr().put("key", "value");
        final TestModelSub sub = new TestModelSub();
        sub.setIntAttr(7);
        sub.setStringAttr("first");
        model.getSubEntityListAttr().add(sub);
        return model;
    }

    private static Map<String, TestCell> expected(final String... pathsAndValues) {
        final Map<String, TestCell> expected = new LinkedHashMap<>();
        for (int i = 0; i < pathsAndValues.length; i += 2) {
            expected.put(pathsAndValues[i], new TestCell("B" + (i / 2 + 1), pathsAndValues[i + 1]));
        }
        return expected;
    }

}
