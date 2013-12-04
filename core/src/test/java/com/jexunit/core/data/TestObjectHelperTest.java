package com.jexunit.core.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jexunit.core.data.entity.TestEnum;
import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.model.TestCell;

public class TestObjectHelperTest {

	private static Map<String, TestCell> testValuesBase = new HashMap<>();
	private static Map<String, TestCell> testValuesSubElement = new HashMap<>();
	private static Map<String, TestCell> testValuesSubElement2 = new HashMap<>();
	private static Map<String, TestCell> testValuesList = new HashMap<>();
	private static Map<String, TestCell> testValuesList2 = new HashMap<>();

	@BeforeClass
	public static void prepare() {
		testValuesBase.put("intAttr", new TestCell(1, "5"));
		testValuesBase.put("doubleAttr", new TestCell(2, "3.21"));
		testValuesBase.put("stringAttr", new TestCell(3, "Test String"));
		testValuesBase.put("booleanAttr", new TestCell(4, "true"));
		testValuesBase.put("stringAttr2", new TestCell(5, "second test string"));
		testValuesBase.put("enumAttr", new TestCell(6, "TYPE_B"));

		testValuesSubElement.put("subEntityAttr.stringAttr", new TestCell(7,
				"sub entity test string"));
		testValuesSubElement.put("subEntityAttr.intAttr", new TestCell(8, "38"));
		testValuesSubElement.put("subEntityAttr.boolAttr", new TestCell(9, "true"));
		testValuesSubElement.put("subEntityAttr.enumAttr", new TestCell(10, "TYPE_C"));

		testValuesList.put("subEntityListAttr[0].intAttr", new TestCell(11, "1"));
		testValuesList.put("subEntityListAttr[1].intAttr", new TestCell(12, "2"));
		testValuesList.put("subEntityListAttr[0].boolAttr", new TestCell(13, "true"));

		testValuesSubElement2.put("subEntityAttr2.stringAttr", new TestCell(20,
				"sub entity test string"));
		testValuesSubElement2.put("subEntityAttr2.intAttr", new TestCell(21, "38"));
		testValuesSubElement2.put("subEntityAttr2.boolAttr", new TestCell(22, "true"));
		testValuesSubElement2.put("subEntityAttr2.enumAttr", new TestCell(23, "TYPE_C"));

		testValuesList2.put("subEntityListAttr2[0].intAttr", new TestCell(24, "1"));
		testValuesList2.put("subEntityListAttr2[1].intAttr", new TestCell(25, "2"));
		testValuesList2.put("subEntityListAttr2[0].boolAttr", new TestCell(26, "true"));
	}

	/**
	 * Test creating a new instance of the test-object. This will test setting the attributes of the
	 * "base-entity".
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectTestCaseClassOfT_baseValues() throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesBase);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

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
	 * Test creating a new instance of the test-object. This will test setting the attributes of the
	 * "sub-entity".
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectTestCaseClassOfT_subElementValues() throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesSubElement);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

		// assert
		assertThat(actual, is(notNullValue()));
		assertThat(actual.getSubEntityAttr().isBoolAttr(), is(true));
		assertThat(actual.getSubEntityAttr().getIntAttr(), is(38));
		assertThat(actual.getSubEntityAttr().getStringAttr(), is(equalTo("sub entity test string")));
		assertThat(actual.getSubEntityAttr().getEnumAttr(), is(equalTo(TestEnum.TYPE_C)));
	}

	/**
	 * Test creating a new instance of the test-object. This will test setting the attributes of the
	 * "sub-entity" (this is going to be created automatically!).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectTestCaseClassOfT_subElementValues_creatingNewSubElement()
			throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesSubElement2);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

		// assert
		assertThat(actual, is(notNullValue()));
		assertThat(actual.getSubEntityAttr2(), is(notNullValue()));
		assertThat(actual.getSubEntityAttr2().isBoolAttr(), is(true));
		assertThat(actual.getSubEntityAttr2().getIntAttr(), is(38));
		assertThat(actual.getSubEntityAttr2().getStringAttr(),
				is(equalTo("sub entity test string")));
		assertThat(actual.getSubEntityAttr2().getEnumAttr(), is(equalTo(TestEnum.TYPE_C)));
	}

	/**
	 * Test creating a new instance of the test-object. This will test setting the attributes of the
	 * "list-entity".
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectTestCaseClassOfT_listValues() throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesList);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

		// assert
		assertThat(actual, is(notNullValue()));
		assertThat(actual.getSubEntityListAttr(), is(notNullValue()));
		assertThat(actual.getSubEntityListAttr().size(), is(2));
		assertThat(actual.getSubEntityListAttr().get(0).getIntAttr(), is(1));
		assertThat(actual.getSubEntityListAttr().get(0).isBoolAttr(), is(true));
		assertThat(actual.getSubEntityListAttr().get(1).getIntAttr(), is(2));
	}

	/**
	 * Test creating a new instance of the test-object. This will test setting the attributes of the
	 * "list-entity" (the list is NOT going to be created automatically!).
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateObjectTestCaseClassOfT_listValues_notCreatingNewList() throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesList2);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, TestModelBase.class);

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
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectTestCaseT() throws Exception {
		// prepare
		TestCase testCase = new TestCase();
		testCase.getValues().putAll(testValuesBase);
		testCase.getValues().putAll(testValuesSubElement);
		testCase.getValues().putAll(testValuesList);
		testCase.getValues().remove("intAttr");

		TestModelBase base = new TestModelBase();
		base.setIntAttr(-768);

		// act
		TestModelBase actual = TestObjectHelper.createObject(testCase, base);

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

}
