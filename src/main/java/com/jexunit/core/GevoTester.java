/**
 * 
 */
package com.jexunit.core;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
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
 * JUnit-Suite for running the tests with the <code>@RunWith</code>-Annotation.
 * 
 * @author fabian
 * 
 */
public class GevoTester extends Suite {

	private final ArrayList<Runner> runners = new ArrayList<Runner>();
	// hold the information for multiple excel-files
	private List<String> excelFileNames = new ArrayList<>();
	private boolean worksheetAsTest = true;

	public GevoTester(Class<?> klass) throws Throwable {
		super(klass, Collections.<Runner> emptyList());
		readExcelFileName();
		// add the Parameterized GevoTestBase, initialized with the ExcelFileName
		for (String excelFileName : excelFileNames) {
			runners.add(new Parameterized(GevoTestBase.class, excelFileName, klass, worksheetAsTest));
		}

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
	@SuppressWarnings("unchecked")
	private void readExcelFileName() throws Exception {
		List<FrameworkField> fields = getTestClass().getAnnotatedFields(ExcelFile.class);
		for (FrameworkField each : fields) {
			if (each.isStatic()
					&& (each.getType() == String.class
							|| each.getType().isAssignableFrom(List.class) || (each.getType()
							.isArray() && each.getType().getComponentType() == String.class))) {
				Field field = each.getField();
				ExcelFile annotation = field.getAnnotation(ExcelFile.class);
				worksheetAsTest = annotation.worksheetAsTest();
				boolean isFieldAccessible = field.isAccessible();
				if (!isFieldAccessible) {
					field.setAccessible(true);
				}

				if (each.getType() == String.class) {
					excelFileNames.add((String) field.get(getTestClass()));
				} else if (each.getType().isArray()
						&& each.getType().getComponentType() == String.class) {
					excelFileNames.addAll(Arrays.asList((String[]) field.get(getTestClass())));
				} else if (each.getType().isAssignableFrom(List.class)
						&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == String.class) {
					excelFileNames.addAll((List<String>) field.get(getTestClass()));
				} else {
					throw new IllegalArgumentException("The annotated static field '"
							+ field.getName() + "' in class '" + getTestClass().getName()
							+ "' as either to be of type String, String[] or List<String>!");
				}

				if (!isFieldAccessible) {
					field.setAccessible(false);
				}
				return;
			}
		}

		// check, if there is a method annotated with @ExcelFile
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(ExcelFile.class);
		for (FrameworkMethod each : methods) {
			if (each.isStatic()
					&& each.isPublic()
					&& (each.getReturnType() == String.class
							|| each.getReturnType().isAssignableFrom(List.class) || (each
							.getReturnType().isArray() && each.getReturnType().getComponentType() == String.class))) {
				if (each.getReturnType() == String.class) {
					excelFileNames.add((String) each.getMethod().invoke(null));
				} else if (each.getReturnType().isArray()
						&& each.getReturnType().getComponentType() == String.class) {
					excelFileNames.addAll(Arrays.asList((String[]) each.getMethod().invoke(null)));
				} else if (each.getReturnType().isAssignableFrom(List.class)
						&& ((ParameterizedType) each.getMethod().getGenericReturnType())
								.getActualTypeArguments()[0] == String.class) {
					excelFileNames.addAll((List<String>) each.getMethod().invoke(null));
				} else {
					throw new IllegalArgumentException("The annotated static field '"
							+ each.getName() + "' in class '" + getTestClass().getName()
							+ "' as either to be of type String, String[] or List<String>!");
				}
				return;
			}
		}

		throw new Exception(
				"No excel-file definition found (static string-field or public static method annotated with @ExcelFile) in class "
						+ getTestClass().getName());
	}
}
