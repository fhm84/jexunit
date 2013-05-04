/**
 * 
 */
package com.jexunit.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
		readExcelFileNames();
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
	 * Get the excel-file-name(s) from the test-class. It should be read from a static field or a
	 * static method returning a string, array or list of strings, both annotated with
	 * {@code @ExcelFile}.
	 * 
	 * @throws Exception
	 */
	private void readExcelFileNames() throws Exception {
		List<FrameworkField> fields = getTestClass().getAnnotatedFields(ExcelFile.class);
		for (FrameworkField each : fields) {
			if (checkFieldForExcelFileAnnotation(each)) {
				return;
			}
		}

		// check, if there is a method annotated with @ExcelFile
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(ExcelFile.class);
		for (FrameworkMethod each : methods) {
			if (checkMethodForExcelFileAnnotation(each)) {
				return;
			}
		}

		throw new Exception(
				"No excel-file definition found (static string-field or public static method annotated with @ExcelFile) in class "
						+ getTestClass().getName());
	}

	/**
	 * Check the given FrameworkField if @ExcelFile-Annotation is present. If so, read the settings
	 * and excel files and return true.
	 * 
	 * @param frameworkField
	 *            the FrameworkField to check for the @ExcelFile-Annotation
	 * 
	 * @return true, if the Annotation is present and the settings and values could be read, else
	 *         false
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	private boolean checkFieldForExcelFileAnnotation(FrameworkField frameworkField)
			throws IllegalArgumentException, IllegalAccessException {
		if (frameworkField.isStatic()
				&& (frameworkField.getType() == String.class
						|| frameworkField.getType().isAssignableFrom(List.class) || (frameworkField
						.getType().isArray() && frameworkField.getType().getComponentType() == String.class))) {
			Field field = frameworkField.getField();
			Class<?> type = field.getType();

			ExcelFile annotation = field.getAnnotation(ExcelFile.class);
			worksheetAsTest = annotation.worksheetAsTest();

			boolean isFieldAccessible = field.isAccessible();
			if (!isFieldAccessible) {
				field.setAccessible(true);
			}

			if (type == String.class) {
				excelFileNames.add((String) field.get(getTestClass()));
			} else if (type.isArray() && type.getComponentType() == String.class) {
				excelFileNames.addAll(Arrays.asList((String[]) field.get(getTestClass())));
			} else if (type.isAssignableFrom(List.class)
					&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == String.class) {
				excelFileNames.addAll((List<String>) field.get(getTestClass()));
			} else {
				throw new IllegalArgumentException("The annotated static field '" + field.getName()
						+ "' in class '" + getTestClass().getName()
						+ "' as either to be of type String, String[] or List<String>!");
			}

			if (!isFieldAccessible) {
				field.setAccessible(false);
			}
			return true;
		}

		return false;
	}

	/**
	 * Check the given FrameworkMethod if @ExcelFile-Annotation is present. If so, read the settings
	 * and excel files and return true.
	 * 
	 * @param frameworkMethod
	 *            the FrameworkMethod to check for the @ExcelFile-Annotation
	 * 
	 * @return true, if the Annotation is present and the settings and values could be read, else
	 *         false
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	private boolean checkMethodForExcelFileAnnotation(FrameworkMethod frameworkMethod)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (frameworkMethod.isStatic()
				&& frameworkMethod.isPublic()
				&& (frameworkMethod.getReturnType() == String.class
						|| frameworkMethod.getReturnType().isAssignableFrom(List.class) || (frameworkMethod
						.getReturnType().isArray() && frameworkMethod.getReturnType()
						.getComponentType() == String.class))) {
			Method method = frameworkMethod.getMethod();
			Class<?> returnType = frameworkMethod.getReturnType();

			ExcelFile annotation = frameworkMethod.getAnnotation(ExcelFile.class);
			worksheetAsTest = annotation.worksheetAsTest();

			if (returnType == String.class) {
				excelFileNames.add((String) method.invoke(null));
			} else if (returnType.isArray() && returnType.getComponentType() == String.class) {
				excelFileNames.addAll(Arrays.asList((String[]) method.invoke(null)));
			} else if (returnType.isAssignableFrom(List.class)
					&& ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0] == String.class) {
				excelFileNames.addAll((List<String>) method.invoke(null));
			} else {
				throw new IllegalArgumentException("The annotated static field '"
						+ frameworkMethod.getName() + "' in class '" + getTestClass().getName()
						+ "' as either to be of type String, String[] or List<String>!");
			}
			return true;
		}

		return false;
	}
}
