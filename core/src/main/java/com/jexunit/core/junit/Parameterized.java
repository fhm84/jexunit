package com.jexunit.core.junit;

import com.jexunit.core.JExUnitBase;
import com.jexunit.core.commands.TestCommandScanner;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.spi.ServiceRegistry;
import com.jexunit.core.spi.data.DataProvider;
import eu.infomas.annotation.AnnotationDetector;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is an "extension" of the {@link org.junit.runners.Parameterized} JUnit-Runner to run each excel-worksheet as a
 * single unit-test. Additionally there is a new annotation ({@link ExcelFile} to define the name of the excel-file in
 * the test-class.
 *
 * @author fabian
 */
public class Parameterized extends Suite {

    /**
     * This is the same as the one in the {@code org.junit.runners.Parameterized}. It has to be copied because it's a
     * private class.
     *
     * @see org.junit.runners.Parameterized
     */
    private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {

        private final Object[] fParameters;
        private final String fName;
        private final Class<?> testType;

        TestClassRunnerForParameters(final Class<?> type, final Object[] parameters, final String name, final Class<?> testType)
                throws InitializationError {
            super(type);
            fParameters = parameters;
            fName = name;
            this.testType = testType;
        }

        @Override
        public Object createTest() throws Exception {
            if (fieldsAreAnnotated()) {
                return createTestUsingFieldInjection();
            } else {
                return createTestUsingConstructorInjection();
            }
        }

        private Object createTestUsingConstructorInjection() throws Exception {
            return getTestClass().getOnlyConstructor().newInstance(fParameters);
        }

        private Object createTestUsingFieldInjection() throws Exception {
            final List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
            if (annotatedFieldsByParameter.size() != fParameters.length) {
                throw new Exception(String.format(
                        "Wrong number of parameters and @Parameter fields. @Parameter fields counted: %s, available parameters: %s.",
                        annotatedFieldsByParameter.size(), fParameters.length));
            }
            final Object testClassInstance = getTestClass().getJavaClass().newInstance();
            if (getTestClass().getJavaClass() == JExUnitBase.class) {
                ((JExUnitBase) testClassInstance).setTestType(testType);
            }
            for (final FrameworkField each : annotatedFieldsByParameter) {
                final Field field = each.getField();
                final Parameter annotation = field.getAnnotation(Parameter.class);
                final int index = annotation.value();
                try {
                    if (field.isAccessible()) {
                        field.set(testClassInstance, fParameters[index]);
                    } else {
                        field.setAccessible(true);
                        field.set(testClassInstance, fParameters[index]);
                        field.setAccessible(false);
                    }
                } catch (final IllegalArgumentException iare) {
                    throw new Exception(
                            String.format(
                                    "%s: Trying to set %s with the value %s that is not the right type (%s instead of %s).",
                                    getTestClass().getName(), field.getName(), fParameters[index],
                                    fParameters[index].getClass().getSimpleName(), field.getType().getSimpleName()),
                            iare);
                }
            }
            return testClassInstance;
        }

        @Override
        protected String getName() {
            return fName;
        }

        @Override
        protected String testName(final FrameworkMethod method) {
            return method.getName() + getName();
        }

        @Override
        protected void validateConstructor(final List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
            if (fieldsAreAnnotated()) {
                validateZeroArgConstructor(errors);
            }
        }

        @Override
        protected void validateFields(final List<Throwable> errors) {
            super.validateFields(errors);
            if (fieldsAreAnnotated()) {
                final List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
                final int[] usedIndices = new int[annotatedFieldsByParameter.size()];
                for (final FrameworkField each : annotatedFieldsByParameter) {
                    final int index = each.getField().getAnnotation(Parameter.class).value();
                    if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
                        errors.add(new Exception(String.format(
                                "Invalid @Parameter value: %s. @Parameter fields counted: %s. Please use an index between 0 and %s.",
                                index, annotatedFieldsByParameter.size(), annotatedFieldsByParameter.size() - 1)));
                    } else {
                        usedIndices[index]++;
                    }
                }
                for (int index = 0; index < usedIndices.length; index++) {
                    final int numberOfUse = usedIndices[index];
                    if (numberOfUse == 0) {
                        errors.add(new Exception(String.format("@Parameter(%s) is never used.", index)));
                    } else if (numberOfUse > 1) {
                        errors.add(new Exception(
                                String.format("@Parameter(%s) is used more than once (%s).", index, numberOfUse)));
                    }
                }
            }
        }

        @Override
        protected Statement classBlock(final RunNotifier notifier) {
            return childrenInvoker(notifier);
        }

        @Override
        protected Annotation[] getRunnerAnnotations() {
            return new Annotation[0];
        }

    }

    private static final List<Runner> NO_RUNNERS = Collections.<Runner>emptyList();
    private final ArrayList<Runner> runners = new ArrayList<>();
    private Class<?> testType;
    private String identifier;
    
    static {
        // scan classes for test commands
        final AnnotationDetector detector = new AnnotationDetector(new TestCommandScanner());
        try {
            final String property = System.getProperty("jexunit.annotation-scan.package");
            if (property != null && !property.isEmpty()) {
                detector.detect(property);
            } else {
                detector.detect();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Only called reflectively. Do not use programmatically.
     *
     * @param clazz the root class
     * @throws Throwable in case that something goes wrong
     */
    public Parameterized(final Class<?> clazz) throws Throwable {
        super(clazz, NO_RUNNERS);

        ServiceRegistry.initialize();

        DataProvider dataprovider = null;

        final List<DataProvider> dataproviders = ServiceRegistry.getInstance().getServicesFor(DataProvider.class);
        if (dataproviders != null) {
            for (final DataProvider dp : dataproviders) {
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

        final Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
        createRunnersForParameters(allParameters(0), parameters.name());
    }

    public Parameterized(final Class<?> clazz, final Class<?> testType, final int testNumber, final String identifier) throws Throwable {
        super(clazz, NO_RUNNERS);
        this.testType = testType;
        this.identifier = identifier;

        final Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
        createRunnersForParameters(allParameters(testNumber), parameters.name());
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @Override
    protected String getName() {
        final StringBuilder sb = new StringBuilder();
        if (identifier != null) {
            sb.append(getSimpleExcelFileName());
        } else {
            sb.append(super.getName());
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Iterable<Object[]> allParameters(final int testNumber) throws Throwable {
        final Object parameters = getParametersMethod().invokeExplosively(null, testNumber);
        if (parameters instanceof Iterable) {
            return (Iterable<Object[]>) parameters;
        } else {
            throw parametersMethodReturnedWrongType();
        }
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        final List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Parameters.class);
        for (final FrameworkMethod each : methods) {
            if (each.isStatic() && each.isPublic()) {
                return each;
            }
        }

        throw new Exception("No public static parameters method on class " + getTestClass().getName());
    }

    private void createRunnersForParameters(final Iterable<Object[]> allParameters, final String namePattern)
            throws InitializationError, Exception {
        try {
            int i = 0;
            for (final Object[] parametersOfSingleTest : allParameters) {
                final String name = nameFor(namePattern, i, parametersOfSingleTest);
                final TestClassRunnerForParameters runner = new TestClassRunnerForParameters(getTestClass().getJavaClass(),
                        parametersOfSingleTest, name, testType);
                runners.add(runner);
                ++i;
            }
        } catch (final ClassCastException e) {
            throw parametersMethodReturnedWrongType();
        }
    }

    private String nameFor(final String namePattern, final int index, final Object[] parameters) {
        String finalPattern;
        if (getSimpleExcelFileName() != null) {
            // change the name of the test to be unique in case of excelFileName is set (i.e. for
            // mass tests)
            finalPattern = getSimpleExcelFileName() + " - " + namePattern;
        } else {
            finalPattern = namePattern;
        }

        final String name;
        String idx = Integer.toString(index);

        if (parameters != null && parameters.length > 0 && parameters[0] instanceof List
                && !((List<?>) parameters[0]).isEmpty() && ((List<?>) parameters[0]).get(0) instanceof TestCase) {
            final TestCase<?> tc = (TestCase<?>) ((List<?>) parameters[0]).get(0);
            if (tc.getMetadata() != null) {
                idx = tc.getMetadata().getIdentifier();
            }
            finalPattern = finalPattern.replaceAll("\\{index\\}", idx);
            name = MessageFormat.format(finalPattern, ((List<?>) parameters[0]).get(0));
        } else {
            finalPattern = finalPattern.replaceAll("\\{index\\}", idx);
            name = MessageFormat.format(finalPattern, parameters);
        }

        return "[" + name + "]";
    }

    /**
     * Get the simple filename of the excel file (without the path).
     *
     * @return the simple name of the excel file
     */
    private String getSimpleExcelFileName() {
        if (identifier != null) {
            return identifier.substring(identifier.lastIndexOf("/") + 1);
        } else {
            return null;
        }
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        final String className = getTestClass().getName();
        final String methodName = getParametersMethod().getName();
        final String message = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.", className, methodName);
        return new Exception(message);
    }

    private List<FrameworkField> getAnnotatedFieldsByParameter() {
        return getTestClass().getAnnotatedFields(Parameter.class);
    }

    private boolean fieldsAreAnnotated() {
        return !getAnnotatedFieldsByParameter().isEmpty();
    }

}
