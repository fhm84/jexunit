package com.jexunit.core.jupiter;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.TestCommandScanner;
import com.jexunit.core.commands.validation.CommandValidator;
import com.jexunit.core.context.TestContextManager;
import com.jexunit.core.model.TestCase;
import com.jexunit.core.spi.AfterSheet;
import com.jexunit.core.spi.BeforeSheet;
import com.jexunit.core.spi.ServiceRegistry;
import com.jexunit.core.spi.data.DataProvider;
import org.junit.jupiter.api.extension.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class JExUnitExtension implements TestTemplateInvocationContextProvider, BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        TestCommandScanner.ensureScanned();

        final String beforeClass = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.BEFORE_EXCEL);
        if (beforeClass != null && !beforeClass.isEmpty()) {
            final Class<?> cls = Class.forName(beforeClass);
            if (BeforeSheet.class.isAssignableFrom(cls)) {
                ((BeforeSheet) cls.getDeclaredConstructor().newInstance()).run();
            }
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        final String afterClass = JExUnitConfig.getStringProperty(JExUnitConfig.ConfigKey.AFTER_EXCEL);
        if (afterClass != null && !afterClass.isEmpty()) {
            final Class<?> cls = Class.forName(afterClass);
            if (AfterSheet.class.isAssignableFrom(cls)) {
                ((AfterSheet) cls.getDeclaredConstructor().newInstance()).run();
            }
        }
    }

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return context.getTestMethod()
                .map(m -> m.isAnnotationPresent(ExcelTest.class))
                .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();

        ServiceRegistry.initialize();

        DataProvider dataProvider = null;
        final List<DataProvider> providers = ServiceRegistry.getInstance().getServicesFor(DataProvider.class);
        if (providers != null) {
            for (final DataProvider dp : providers) {
                if (dp.canProvide(testClass)) {
                    dataProvider = dp;
                }
            }
        }
        if (dataProvider == null) {
            throw new IllegalStateException("No DataProvider found for: " + testClass.getName());
        }

        try {
            TestContextManager.add(DataProvider.class, dataProvider);
            dataProvider.initialize(testClass);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to initialize DataProvider", e);
        }

        final List<TestTemplateInvocationContext> invocations = new ArrayList<>();
        final int sheetCount = dataProvider.numberOfTests();

        for (int i = 0; i < sheetCount; i++) {
            try {
                final Collection<Object[]> testData = dataProvider.loadTestData(i);
                CommandValidator.validateCommands(testData);
                final String identifier = dataProvider.getIdentifier(i);

                for (final Object[] row : testData) {
                    @SuppressWarnings("unchecked")
                    final List<TestCase<?>> testCases = (List<TestCase<?>>) row[0];
                    invocations.add(new JExUnitInvocationContext(testCases, identifier));
                }
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to load test data for sheet " + i, e);
            }
        }

        return invocations.stream();
    }
}
