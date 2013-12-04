package com.jexunit.examples.arithmeticaltests.model;

/**
 * This is a "test"-entity to show how to integrate the business entities with the model classes for
 * testing.
 * 
 * @author fabian
 * 
 */
public class CustomTestObject {

	private ArithmeticalEntity entity;
	private double result;

	public ArithmeticalEntity getEntity() {
		return entity;
	}

	public void setEntity(ArithmeticalEntity entity) {
		this.entity = entity;
	}

	public double getResult() {
		return result;
	}

	public void setResult(double result) {
		this.result = result;
	}

}
