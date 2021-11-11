package com.jexunit.core.data;

import com.jexunit.core.data.entity.TestEnum;
import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestObjectHelperTest {

    private static Map<String, TestCell> testValuesBase = new HashMap<>();
    private static Map<String, TestCell> testValuesSubElement = new HashMap<>();
    private static Map<String, TestCell> testValuesSubElement2 = new HashMap<>();
    private static Map<String, TestCell> testValuesList = new HashMap<>();
    private static Map<String, TestCell> testValuesList2 = new HashMap<>();

    @BeforeClass
    public static void prepare() {
        testValuesBase.put("intAttr", new TestCell("A", "5"));
        testValuesBase.put("doubleAttr", new TestCell("B", "3.21"));
        testValuesBase.put("stringAttr", new TestCell("C", "Test String"));
        testValuesBase.put("booleanAttr", new TestCell("D", "true"));
        testValuesBase.put("stringAttr2", new TestCell("E", "second test string"));
        testValuesBase.put("enumAttr", new TestCell("F", "TYPE_B"));

        testValuesSubElement.put("subEntityAttr.stringAttr", new TestCell("G", "sub entity test string"));
        testValuesSubElement.put("subEntityAttr.intAttr", new TestCell("H", "38"));
        testValuesSubElement.put("subEntityAttr.boolAttr", new TestCell("I", "true"));
        testValuesSubElement.put("subEntityAttr.enumAttr", new TestCell("J", "TYPE_C"));

        testValuesList.put("subEntityListAttr[0].intAttr", new TestCell("K", "1"));
        testValuesList.put("subEntityListAttr[1].intAttr", new TestCell("L", "2"));
        testValuesList.put("subEntityListAttr[0].boolAttr", new TestCell("M", "true"));

        testValuesSubElement2.put("subEntityAttr2.stringAttr", new TestCell("T", "sub entity test string"));
        testValuesSubElement2.put("subEntityAttr2.intAttr", new TestCell("U", "38"));
        testValuesSubElement2.put("subEntityAttr2.boolAttr", new TestCell("V", "true"));
        testValuesSubElement2.put("subEntityAttr2.enumAttr", new TestCell("W", "TYPE_C"));

        testValuesList2.put("subEntityListAttr2[0].intAttr", new TestCell("X", "1"));
        testValuesList2.put("subEntityListAttr2[1].intAttr", new TestCell("Y", "2"));
        testValuesList2.put("subEntityListAttr2[0].boolAttr", new TestCell("Z", "true"));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "base-entity".
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_baseValues() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesBase);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getStringAttr(), is(equalTo("Test String")));
        assertThat(actual.getIntAttr(), is(equalTo(5)));
        assertThat(actual.getDoubleAttr(), is(equalTo(3.21)));
        assertThat(actual.isBooleanAttr(), is(equalTo(true)));
        assertThat(actual.getStringAttr2(), is(equalTo("second test string")));
        assertThat(actual.getEnumAttr(), is(equalTo(TestEnum.TYPE_B)));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "sub-entity".
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_subElementValues() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesSubElement);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getSubEntityAttr().isBoolAttr(), is(true));
        assertThat(actual.getSubEntityAttr().getIntAttr(), is(38));
        assertThat(actual.getSubEntityAttr().getStringAttr(), is(equalTo("sub entity test string")));
        assertThat(actual.getSubEntityAttr().getEnumAttr(), is(equalTo(TestEnum.TYPE_C)));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "sub-entity" (this
     * is going to be created automatically!).
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_subElementValues_creatingNewSubElement() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesSubElement2);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getSubEntityAttr2(), is(notNullValue()));
        assertThat(actual.getSubEntityAttr2().isBoolAttr(), is(true));
        assertThat(actual.getSubEntityAttr2().getIntAttr(), is(38));
        assertThat(actual.getSubEntityAttr2().getStringAttr(), is(equalTo("sub entity test string")));
        assertThat(actual.getSubEntityAttr2().getEnumAttr(), is(equalTo(TestEnum.TYPE_C)));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "list-entity".
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_listValues() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesList);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getSubEntityListAttr(), is(notNullValue()));
        assertThat(actual.getSubEntityListAttr().size(), is(2));
        assertThat(actual.getSubEntityListAttr().get(0).getIntAttr(), is(1));
        assertThat(actual.getSubEntityListAttr().get(0).isBoolAttr(), is(true));
        assertThat(actual.getSubEntityListAttr().get(1).getIntAttr(), is(2));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "list-entity" (the
     * list is going to be created automatically!).
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_listValues_creatingNewList() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesList2);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getSubEntityListAttr2(), is(notNullValue()));
        assertThat(actual.getSubEntityListAttr2().size(), is(2));
        assertThat(actual.getSubEntityListAttr2().get(0).getIntAttr(), is(1));
        assertThat(actual.getSubEntityListAttr2().get(0).isBoolAttr(), is(true));
        assertThat(actual.getSubEntityListAttr2().get(1).getIntAttr(), is(2));
    }

    /**
     * Test to set only a subset of an already existing instance of the test-object.
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseT() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesBase);
        testCase.getValues().putAll(testValuesSubElement);
        testCase.getValues().putAll(testValuesList);
        testCase.getValues().remove("intAttr");

        final TestModelBase base = new TestModelBase();
        base.setIntAttr(-768);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, base);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getStringAttr(), is(equalTo("Test String")));
        assertThat(actual.getIntAttr(), is(equalTo(-768)));
        assertThat(actual.getDoubleAttr(), is(equalTo(3.21)));
        assertThat(actual.isBooleanAttr(), is(equalTo(true)));
        assertThat(actual.getStringAttr2(), is(equalTo("second test string")));
        assertThat(actual.getEnumAttr(), is(equalTo(TestEnum.TYPE_B)));
        assertThat(actual.getSubEntityAttr().isBoolAttr(), is(true));
        assertThat(actual.getSubEntityAttr().getIntAttr(), is(38));
        assertThat(actual.getSubEntityAttr().getStringAttr(), is(equalTo("sub entity test string")));
        assertThat(actual.getSubEntityAttr().getEnumAttr(), is(equalTo(TestEnum.TYPE_C)));

        assertThat(actual.getSubEntityListAttr(), is(notNullValue()));
        assertThat(actual.getSubEntityListAttr().size(), is(2));
        assertThat(actual.getSubEntityListAttr().get(0).getIntAttr(), is(1));
        assertThat(actual.getSubEntityListAttr().get(0).isBoolAttr(), is(true));
        assertThat(actual.getSubEntityListAttr().get(1).getIntAttr(), is(2));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "list-entity" (the
     * list is going to be created automatically!) and also of sub-lists, because the "list-entity" also contains a
     * list-attribute.
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_subListValues_creatingNewLists() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesList2);

        final Map<String, TestCell> testValuesSubList = new HashMap<>();
        testValuesSubList.put("subEntityListAttr2[0].subListAttr[0].intAttr", new TestCell("AD", "100"));
        testValuesSubList.put("subEntityListAttr2[1].subListAttr[0].intAttr", new TestCell("AE", "99"));
        testValuesSubList.put("subEntityListAttr2[0].subListAttr[1].boolAttr", new TestCell("AF", "true"));

        testCase.getValues().putAll(testValuesSubList);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getSubEntityListAttr2(), is(notNullValue()));
        assertThat(actual.getSubEntityListAttr2().size(), is(2));
        assertThat(actual.getSubEntityListAttr2().get(0).getIntAttr(), is(1));
        assertThat(actual.getSubEntityListAttr2().get(0).isBoolAttr(), is(true));
        assertThat(actual.getSubEntityListAttr2().get(1).getIntAttr(), is(2));

        // check the sub-lists
        assertThat(actual.getSubEntityListAttr2().get(0).getSubListAttr(), is(notNullValue()));
        assertThat(actual.getSubEntityListAttr2().get(0).getSubListAttr().size(), is(2));
        assertThat(actual.getSubEntityListAttr2().get(0).getSubListAttr().get(0).getIntAttr(), is(100));
        assertThat(actual.getSubEntityListAttr2().get(0).getSubListAttr().get(1).isBoolAttr(), is(true));
        assertThat(actual.getSubEntityListAttr2().get(1).getSubListAttr().size(), is(1));
        assertThat(actual.getSubEntityListAttr2().get(1).getSubListAttr().get(0).getIntAttr(), is(99));
    }

    /**
     * Test creating a new instance of the test-object. This will test setting the attributes of the "base-entity" and
     * setting attributes into a map.
     *
     * @throws Exception in case that something goes wrong
     */
    @Test
    public void testCreateObjectTestCaseClassOfT_baseValuesAndMap() throws Exception {
        // prepare
        final TestCase<?> testCase = new TestCase<>();
        testCase.getValues().putAll(testValuesBase);

        final Map<String, TestCell> testValuesMap = new HashMap<>();
        testValuesMap.put("mapAttr[\"myKey\"]", new TestCell("AD", "Hello"));
        testValuesMap.put("mapAttr['yourKey']", new TestCell("AE", "world"));
        testValuesMap.put("mapAttr[\"ourKey\"]", new TestCell("AF", "yeah!"));

        testCase.getValues().putAll(testValuesMap);

        // act
        final TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

        // assert
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getStringAttr(), is(equalTo("Test String")));
        assertThat(actual.getIntAttr(), is(equalTo(5)));
        assertThat(actual.getDoubleAttr(), is(equalTo(3.21)));
        assertThat(actual.isBooleanAttr(), is(equalTo(true)));
        assertThat(actual.getStringAttr2(), is(equalTo("second test string")));
        assertThat(actual.getEnumAttr(), is(equalTo(TestEnum.TYPE_B)));

        // check the map
        assertThat(actual.getMapAttr(), is(notNullValue()));
        assertThat(actual.getMapAttr().size(), is(3));
        assertThat(actual.getMapAttr().get("myKey"), is(equalTo("Hello")));
        assertThat(actual.getMapAttr().get("yourKey"), is(equalTo("world")));
        assertThat(actual.getMapAttr().get("ourKey"), is(equalTo("yeah!")));
    }

}
