package com.jexunit.core.data;

import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.data.entity.TestModelChild;
import com.jexunit.core.data.entity.TestModelSub;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for filling lists of the entities out of the excel file (column headers like <code>items[0].name</code>).
 */
public class CollectionMappingTest {

    @Test
    public void shouldAddElementsToInheritedList() throws Exception {
        final TestModelChild actual = createObject(TestModelChild.class, "subEntityListAttr[0].intAttr", "1");

        assertEquals(1, actual.getSubEntityListAttr().size());
        assertEquals(1, actual.getSubEntityListAttr().get(0).getIntAttr());
    }

    @Test
    public void shouldCreateListOfSubEntity() throws Exception {
        final TestModelBase actual = createObject(TestModelBase.class, "subEntityAttr.lazyListAttr[1].stringAttr", "b");

        final List<TestModelSub> list = actual.getSubEntityAttr().getLazyListAttr();
        assertEquals(2, list.size());
        assertNotNull(list.get(0));
        assertEquals("b", list.get(1).getStringAttr());
    }

    @Test
    public void shouldSetSimpleListValues() throws Exception {
        final TestModelBase actual = createObject(TestModelBase.class,
                "stringListAttr[0]", "x", "stringListAttr[2]", "z");

        assertEquals(Arrays.asList("x", null, "z"), actual.getStringListAttr());
    }

    @Test
    public void shouldConvertSimpleListValuesToElementType() throws Exception {
        final TestModelBase actual = createObject(TestModelBase.class,
                "integerListAttr[0]", "5", "integerListAttr[1]", "7");

        assertEquals(Arrays.asList(5, 7), actual.getIntegerListAttr());
    }

    @Test
    public void shouldAddElementsToListOfElementFoundByCondition() throws Exception {
        final TestModelBase actual = createObject(TestModelBase.class,
                "subEntityListAttr[0].stringAttr", "a",
                "subEntityListAttr[stringAttr=a].subListAttr[0].intAttr", "3");

        assertEquals(3, actual.getSubEntityListAttr().get(0).getSubListAttr().get(0).getIntAttr());
    }

    @Test
    public void shouldQuoteEachConditionOfExpression() {
        assertEquals("items['name=a'].sub['id=1'].qty", OgnlUtils.prepareExpression("items[name=a].sub[id=1].qty"));
        assertEquals("items['name=a'].sub[0].qty", OgnlUtils.prepareExpression("items[name=a].sub[0].qty"));
        assertEquals("items['name=a'].qty", OgnlUtils.prepareExpression("items['name=a'].qty"));
        assertEquals("items[0].qty", OgnlUtils.prepareExpression("items[0].qty"));
    }

    @Test
    public void shouldSetPropertyOfSetElementFoundByCondition() throws Exception {
        final TestModelBase tmb = new TestModelBase();
        final TestModelSub element = new TestModelSub();
        element.setStringAttr("a");
        tmb.setSubEntitySetAttr(new HashSet<>(Collections.singletonList(element)));

        OgnlUtils.setPropertyToObject(tmb, "subEntitySetAttr[stringAttr=a].intAttr", "4");

        assertEquals(4, element.getIntAttr());
    }

    private static <T> T createObject(final Class<T> type, final String... keyValues) throws Exception {
        final TestCase<?> testCase = new TestCase<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            testCase.getValues().put(keyValues[i], new TestCell("A" + i, keyValues[i + 1]));
        }
        return TestObjectHelper.createObject(testCase, type);
    }

}
