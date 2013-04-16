/**
 * 
 */
package com.jexunit.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;

import com.jexunit.core.junit.Parameterized;
import com.jexunit.core.junit.Parameterized.ExcelFile;

/**
 * @author fabian
 * 
 */
public class GevoTester extends Suite {

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	public GevoTester(Class<?> klass) throws Throwable {
		super(klass, Collections.<Runner> emptyList());
		// add the Parameterized GevoTestBase, initialized with the ExcelFileName
		runners.add(new Parameterized(GevoTestBase.class, getExcelFileName(), klass));

		// if there are Test-methods defined in the test-class, this once will be execute too
		try {
			runners.add(new BlockJUnit4ClassRunner(klass));
		} catch (Exception e) {
			// ignore (if there is no method annotated with @Test in the class, an exception is
			// thrown -> so we can ignore this here)
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	/**
	 * Get the excel-file-name from the test-class. It should be read from a static field or a
	 * static method returning a string, both annotated with {@code @ExcelFile}.
	 * 
	 * @return the name of the excel-file to use for the test
	 * @throws Exception
	 */
	private String getExcelFileName() throws Exception {
		List<FrameworkField> fields = getTestClass().getAnnotatedFields(ExcelFile.class);
		for (FrameworkField each : fields) {
			if (each.isStatic() && each.getType() == String.class) {
				Field field = each.getField();
				if (field.isAccessible()) {
					return (String) field.get(getTestClass());
				} else {
					field.setAccessible(true);
					String retVal = (String) field.get(getTestClass());
					field.setAccessible(false);
					return retVal;
				}
			}
		}

		// check, if there is a method annotated with @ExcelFile
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(ExcelFile.class);
		for (FrameworkMethod each : methods) {
			if (each.isStatic() && each.isPublic() && each.getReturnType() == String.class) {
				return (String) each.getMethod().invoke(null);
			}
		}

		throw new Exception(
				"No excel-file definition found (static string-field or public static method annotated with @ExcelFile) in class "
						+ getTestClass().getName());
	}
}
