package com.jexunit.core.dataprovider;

import com.jexunit.core.spi.data.DataProvider;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * DataProvider implementation for reading the test data out of excel files.
 *
 * @author fabian
 */
public class ExcelDataProvider implements DataProvider {

    private Class<?> testClass;

    // hold the information for multiple excel-files
    private List<String> excelFileNames;

    private boolean worksheetAsTest;

    @Override
    public boolean canProvide(final Class<?> testClass) {
        int annotatedFields = 0;
        final Field[] fields = testClass.getDeclaredFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(ExcelFile.class) && isAcceptable(field)) {
                annotatedFields++;
            }
        }

        int annotatedMethods = 0;
        // check, if there is a method annotated with @ExcelFile
        final Method[] methods = testClass.getMethods();
        for (final Method method : methods) {
            if (method.isAnnotationPresent(ExcelFile.class) && isAcceptable(method)) {
                annotatedMethods++;
            }
        }

        if (annotatedFields == 0 && annotatedMethods == 0) {
            // nothing found
            return false;
        }
        // check for unique @ExcelFile-Annotation?
        if (annotatedFields > 1 || annotatedMethods > 1 || (annotatedFields + annotatedMethods) > 1) {
            // multiple annotations found -> which one to choose?
            return false;
        }
        return true;
    }

    @Override
    public void initialize(final Class<?> testClass) throws Exception {
        this.testClass = testClass;

        this.excelFileNames = new ArrayList<>();
        this.worksheetAsTest = true;

        readExcelFileNames();
    }

    @Override
    public int numberOfTests() {
        if (excelFileNames != null) {
            return excelFileNames.size();
        }

        throw new IllegalArgumentException("Sorry, but the ExcelDataProvider seems not to be initialized yet!");
    }

    @Override
    public String getIdentifier(final int number) {
        if (excelFileNames == null || number >= excelFileNames.size() || number < 0) {
            throw new IllegalArgumentException("The ExcelDataProvider cannot provide test data for test number "
                    + number + "!");
        }
        return excelFileNames.get(number);
    }

    @Override
    public Collection<Object[]> loadTestData(final int test) throws Exception {
        if (excelFileNames == null || test >= excelFileNames.size() || test < 0) {
            throw new IllegalArgumentException("The ExcelDataProvider cannot provide test data for test number " + test
                    + "!");
        }
        return ExcelLoader.loadTestData(excelFileNames.get(test), worksheetAsTest);
    }

    /**
     * Check if the field is acceptable to provide the excel filename(s).
     *
     * @param field the field to check for the "correct" modifiers and type
     * @return <code>true</code> if the given field is acceptable to provide the excel filename(s), else <code>false</code>
     */
    private boolean isAcceptable(final Field field) {
        return Modifier.isStatic(field.getModifiers())
                && (field.getType() == String.class || field.getType().isAssignableFrom(List.class) || field.getType()
                .isArray() && field.getType().getComponentType() == String.class);
    }

    /**
     * Check if the method is acceptable to provide the excel filename(s).
     *
     * @param method the method to check for the "correct" modifiers and return type
     * @return <code>true</code> if the given method is acceptable to provide the excel filename(s), else <code>false</code>
     */
    private boolean isAcceptable(final Method method) {
        return Modifier.isStatic(method.getModifiers())
                && Modifier.isPublic(method.getModifiers())
                && (method.getReturnType() == String.class || method.getReturnType().isAssignableFrom(List.class) || method
                .getReturnType().isArray() && method.getReturnType().getComponentType() == String.class);
    }

    /**
     * Get the excel-file-name(s) from the test-class. It should be read from a static field or a static method
     * returning a string, array or list of strings, both annotated with {@code @ExcelFile}.
     *
     * @throws Exception
     */
    private void readExcelFileNames() throws Exception {
        final Field[] fields = testClass.getDeclaredFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(ExcelFile.class)) {
                if (checkFieldForExcelFileAnnotation(field)) {
                    return;
                }
            }
        }

        // check, if there is a method annotated with @ExcelFile
        final Method[] methods = testClass.getMethods();
        for (final Method method : methods) {
            if (method.isAnnotationPresent(ExcelFile.class)) {
                if (checkMethodForExcelFileAnnotation(method)) {
                    return;
                }
            }
        }

        throw new IllegalArgumentException(
                "No excel-file definition found (static string-field or public static method annotated with @ExcelFile) in class "
                        + testClass.getName());
    }

    /**
     * Check the given FrameworkField if @ExcelFile-Annotation is present. If so, read the settings and excel files and
     * return true.
     *
     * @param field the FrameworkField to check for the @ExcelFile-Annotation
     * @return true, if the Annotation is present and the settings and values could be read, else false
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    private boolean checkFieldForExcelFileAnnotation(final Field field) throws IllegalArgumentException,
            IllegalAccessException {
        if (isAcceptable(field)) {
            final Class<?> type = field.getType();

            final ExcelFile annotation = field.getAnnotation(ExcelFile.class);
            worksheetAsTest = annotation.worksheetAsTest();

            final boolean isFieldAccessible = field.isAccessible();
            if (!isFieldAccessible) {
                field.setAccessible(true);
            }

            if (type == String.class) {
                excelFileNames.add((String) field.get(testClass));
            } else if (type.isArray() && type.getComponentType() == String.class) {
                excelFileNames.addAll(Arrays.asList((String[]) field.get(testClass)));
            } else if (type.isAssignableFrom(List.class)
                    && ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == String.class) {
                excelFileNames.addAll((List<String>) field.get(testClass));
            } else {
                throw new IllegalArgumentException("The annotated static field '" + field.getName() + "' in class '"
                        + testClass.getName() + "' as either to be of type String, String[] or List<String>!");
            }

            if (!isFieldAccessible) {
                field.setAccessible(false);
            }
            return true;
        }

        return false;
    }

    /**
     * Check the given FrameworkMethod if @ExcelFile-Annotation is present. If so, read the settings and excel files and
     * return true.
     *
     * @param method the FrameworkMethod to check for the @ExcelFile-Annotation
     * @return true, if the Annotation is present and the settings and values could be read, else false
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    private boolean checkMethodForExcelFileAnnotation(final Method method) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (isAcceptable(method)) {
            final Class<?> returnType = method.getReturnType();

            final ExcelFile annotation = method.getAnnotation(ExcelFile.class);
            worksheetAsTest = annotation.worksheetAsTest();

            if (returnType == String.class) {
                excelFileNames.add((String) method.invoke(null));
            } else if (returnType.isArray() && returnType.getComponentType() == String.class) {
                excelFileNames.addAll(Arrays.asList((String[]) method.invoke(null)));
            } else if (returnType.isAssignableFrom(List.class)
                    && ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0] == String.class) {
                excelFileNames.addAll((List<String>) method.invoke(null));
            } else {
                throw new IllegalArgumentException("The annotated static field '" + method.getName() + "' in class '"
                        + testClass.getName() + "' as either to be of type String, String[] or List<String>!");
            }
            return true;
        }

        return false;
    }

}
