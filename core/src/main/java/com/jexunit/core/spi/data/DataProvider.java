package com.jexunit.core.spi.data;

import java.util.Collection;

import com.jexunit.core.model.TestCase;

/**
 * DataProvider to read the data from file or anywhere and transform it to the internal representation.
 * 
 * @author fabian
 *
 */
public interface DataProvider {

	/**
	 * Check the test to identify whether the data provider is able to load the test data with the given information.
	 * Place to check the preconditions for the data provider.
	 * 
	 * @param testClass
	 *            (test)class to check for field, annotations or something else to check, if the provider can load the
	 *            test data for
	 * @return true, if the data provider could get enough information to load the test data, else false
	 */
	boolean canProvide(Class<?> testClass);

	/**
	 * Initialize the DataProvider for a test. This method should be used to load enough data to calculate the number of
	 * tests that will be provided.
	 * 
	 * @param testClass
	 *            the (test)class to initialize the dataprovider for
	 * @throws Exception
	 *             in case something went wrong
	 */
	void initialize(Class<?> testClass) throws Exception;

	/**
	 * It could be possible a test defines multiple files providing some test data. This method returns the number of
	 * tests (files) found.
	 * 
	 * @return the number of test (files) found
	 */
	int numberOfTests();

	/**
	 * Get the identifier (i.e. the filename) of the test with the given number.
	 * 
	 * @param number
	 *            number of the test
	 * @return the identifier for the test file with the given number (i.e. the filename)
	 */
	String getIdentifier(int number);

	/**
	 * Load the test data and transform it into JExUnits internal representation.<br>
	 * Each object has to be a list of type {@link TestCase}.
	 * 
	 * @param test
	 *            the number of the test to load the data for
	 * @return the list of TestCases
	 * @throws Exception
	 *             if something went wrong
	 */
	Collection<Object[]> loadTestData(int test) throws Exception;
}
