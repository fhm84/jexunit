package com.jexunit.examples.businesstests.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This entity represents a "very complex" business entity ;).
 * 
 * @author fabian
 * 
 */
public class MyComplexBusinessEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;

	private String name;
	private String city;
	private Date birthday;

	private int rate;
	private int count;
	private double percentage;

	private BigDecimal calcField1;
	private BigDecimal calcField2;
	private BigDecimal calcField3;

	private Map<String, String> additionalSettings = new HashMap<>();

	private List<MyComplexBusinessEntity> list = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public double getPercentage() {
		return percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}

	public BigDecimal getCalcField1() {
		return calcField1;
	}

	public void setCalcField1(BigDecimal calcField1) {
		this.calcField1 = calcField1;
	}

	public BigDecimal getCalcField2() {
		return calcField2;
	}

	public void setCalcField2(BigDecimal calcField2) {
		this.calcField2 = calcField2;
	}

	public BigDecimal getCalcField3() {
		return calcField3;
	}

	public void setCalcField3(BigDecimal calcField3) {
		this.calcField3 = calcField3;
	}

	public Map<String, String> getAdditionalSettings() {
		return additionalSettings;
	}

	public void setAdditionalSettings(Map<String, String> additionalSettings) {
		this.additionalSettings = additionalSettings;
	}

	/**
	 * @return the list
	 */
	public List<MyComplexBusinessEntity> getList() {
		return list;
	}

	/**
	 * @param list the list to set
	 */
	public void setList(List<MyComplexBusinessEntity> list) {
		this.list = list;
	}

}
