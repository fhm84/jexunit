package com.jexunit.core.data.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class of the entity-object-tree for testing the ObjectHelper setting the
 * entities-attributes.
 * 
 * @author fabian
 * 
 */
public class TestModelBase {

	private int intAttr;
	private Double doubleAttr;
	private String stringAttr;
	private TestModelSub subEntityAttr = new TestModelSub();
	private boolean booleanAttr;
	private String stringAttr2;
	private List<TestModelSub> subEntityListAttr = new ArrayList<>();
	private TestEnum enumAttr;
	private TestModelSub subEntityAttr2;
	private List<TestModelSub> subEntityListAttr2;
	private Map<String, String> mapAttr = new HashMap<>();

	public int getIntAttr() {
		return intAttr;
	}

	public void setIntAttr(int intAttr) {
		this.intAttr = intAttr;
	}

	public Double getDoubleAttr() {
		return doubleAttr;
	}

	public void setDoubleAttr(Double doubleAttr) {
		this.doubleAttr = doubleAttr;
	}

	public String getStringAttr() {
		return stringAttr;
	}

	public void setStringAttr(String stringAttr) {
		this.stringAttr = stringAttr;
	}

	public TestModelSub getSubEntityAttr() {
		return subEntityAttr;
	}

	public void setSubEntityAttr(TestModelSub subEntityAttr) {
		this.subEntityAttr = subEntityAttr;
	}

	public boolean isBooleanAttr() {
		return booleanAttr;
	}

	public void setBooleanAttr(boolean booleanAttr) {
		this.booleanAttr = booleanAttr;
	}

	public String getStringAttr2() {
		return stringAttr2;
	}

	public void setStringAttr2(String stringAttr2) {
		this.stringAttr2 = stringAttr2;
	}

	public List<TestModelSub> getSubEntityListAttr() {
		return subEntityListAttr;
	}

	public void setSubEntityListAttr(List<TestModelSub> subEntityListAttr) {
		this.subEntityListAttr = subEntityListAttr;
	}

	public TestEnum getEnumAttr() {
		return enumAttr;
	}

	public void setEnumAttr(TestEnum enumAttr) {
		this.enumAttr = enumAttr;
	}

	public TestModelSub getSubEntityAttr2() {
		return subEntityAttr2;
	}

	public void setSubEntityAttr2(TestModelSub subEntityAttr2) {
		this.subEntityAttr2 = subEntityAttr2;
	}

	public List<TestModelSub> getSubEntityListAttr2() {
		return subEntityListAttr2;
	}

	public void setSubEntityListAttr2(List<TestModelSub> subEntityListAttr2) {
		this.subEntityListAttr2 = subEntityListAttr2;
	}

	public Map<String, String> getMapAttr() {
		return mapAttr;
	}

	public void setMapAttr(Map<String, String> mapAttr) {
		this.mapAttr = mapAttr;
	}

}
