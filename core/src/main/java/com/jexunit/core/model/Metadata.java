package com.jexunit.core.model;

/**
 * Container for the metadata of a {@linkplain TestCase}. This can also be provided by a dataprovider to put additional
 * information to the testcase.
 * 
 * @author Fabian
 *
 */
public class Metadata {

	private String testGroup;
	private String identifier;

	public String getTestGroup() {
		return testGroup;
	}

	public void setTestGroup(String testGroup) {
		this.testGroup = testGroup;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getDetailedIdentifier() {
		return String.format("identifier: %s", identifier);
	}
}
