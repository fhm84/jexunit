package com.jexunit.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;

import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.junit.Parameterized;
import com.jexunit.core.spi.ServiceRegistry;
import com.jexunit.core.spi.data.DataProvider;

/**
 * JUnit-Suite for running the tests with the <code>@RunWith</code>-Annotation.
 * 
 * @author fabian
 * 
 */
public class JExUnit extends Suite {

	private static final Logger LOG = Logger.getLogger(JExUnit.class.getName());

	private final ArrayList<Runner> runners = new ArrayList<>();

	public JExUnit(Class<?> clazz) throws Throwable {
		super(clazz, Collections.<Runner>emptyList());

		ServiceRegistry.initialize();

		DataProvider dataprovider = null;

		List<DataProvider> dataproviders = ServiceRegistry.getInstance().getServicesFor(DataProvider.class);
		if (dataproviders != null) {
			for (DataProvider dp : dataproviders) {
				if (dp.canProvide(clazz)) {
					dataprovider = dp;
				}
			}
		}

		if (dataprovider == null) {
			throw new IllegalArgumentException();
		}

		TestContextManager.add(DataProvider.class, dataprovider);
		dataprovider.initialize(clazz);

		// add the Parameterized JExUnitBase, initialized with the ExcelFileName
		for (int i = 0; i < dataprovider.numberOfTests(); i++) {
			runners.add(new Parameterized(JExUnitBase.class, clazz, i, dataprovider.getIdentifier(i)));
		}

		// if there are Test-methods defined in the test-class, this once will be execute too
		try {
			runners.add(new BlockJUnit4ClassRunner(clazz));
		} catch (Exception e) {
			// ignore (if there is no method annotated with @Test in the class, an exception is
			// thrown -> so we can ignore this here)
			LOG.finer("No method found annotated with @Test; this will be ignored!");
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

}
