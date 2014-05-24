package com.jexunit.core.data.entity;

import java.util.ArrayList;
import java.util.List;

public class TestModelSub {

	private String stringAttr;
	private int intAttr;
	private boolean boolAttr;
	private TestEnum enumAttr;
	private List<TestModelSub> subListAttr = new ArrayList<>();

	public String getStringAttr() {
		return stringAttr;
	}

	public void setStringAttr(String stringAttr) {
		this.stringAttr = stringAttr;
	}

	public int getIntAttr() {
		return intAttr;
	}

	public void setIntAttr(int intAttr) {
		this.intAttr = intAttr;
	}

	public boolean isBoolAttr() {
		return boolAttr;
	}

	public void setBoolAttr(boolean boolAttr) {
		this.boolAttr = boolAttr;
	}

	public TestEnum getEnumAttr() {
		return enumAttr;
	}

	public void setEnumAttr(TestEnum enumAttr) {
		this.enumAttr = enumAttr;
	}

	public List<TestModelSub> getSubListAttr() {
		return subListAttr;
	}

	public void setSubListAttr(List<TestModelSub> subListAttr) {
		this.subListAttr = subListAttr;
	}

}
