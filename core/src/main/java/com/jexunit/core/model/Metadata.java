package com.jexunit.core.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Container for the metadata of a {@linkplain TestCase}. This can also be provided by a data provider to put additional
 * information to the test case.
 * 
 * @author fabian
 *
 */
@Getter
@Setter
public class Metadata {

	/**
	 * Test group for the test case. Here you can group the test cases to "bigger" tests.
	 */
	private String testGroup;
	/**
	 * Identifier for the test case.
	 */
	private String identifier;

	public String getDetailedIdentifier() {
		return String.format("identifier: %s", identifier);
	}

}
