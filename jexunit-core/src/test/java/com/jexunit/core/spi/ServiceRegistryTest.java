package com.jexunit.core.spi;

import com.jexunit.core.dataprovider.ExcelDataProvider;
import com.jexunit.core.dataprovider.ExcelFile;
import com.jexunit.core.spi.data.DataProvider;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ServiceRegistryTest {

    @Test
    public void shouldCreateNewDataProviderPerTestClass() {
        final ServiceRegistry registry = ServiceRegistry.getInstance();

        final DataProvider first = registry.createDataProvider(ExcelTestClass.class);
        final DataProvider second = registry.createDataProvider(ExcelTestClass.class);

        assertThat(first, instanceOf(ExcelDataProvider.class));
        assertThat(second, not(sameInstance(first)));
        assertThat(first, not(sameInstance(registry.getServicesFor(DataProvider.class).get(0))));
    }

    @Test
    public void shouldReturnNullIfNoDataProviderCanProvide() {
        assertNull(ServiceRegistry.getInstance().createDataProvider(ServiceRegistryTest.class));
    }

    @Test
    public void shouldUseRegisteredInstanceIfItCannotBeInstantiated() {
        // anonymous classes have no accessible no-arg constructor
        final DataProvider registered = new DataProvider() {
            @Override
            public boolean canProvide(final Class<?> testClass) {
                return testClass == CustomTestClass.class;
            }

            @Override
            public void initialize(final Class<?> testClass) {
            }

            @Override
            public int numberOfTests() {
                return 0;
            }

            @Override
            public String getIdentifier(final int number) {
                return null;
            }

            @Override
            public Collection<Object[]> loadTestData(final int test) {
                return null;
            }
        };
        ServiceRegistry.getInstance().register(DataProvider.class, registered);

        assertSame(registered, ServiceRegistry.getInstance().createDataProvider(CustomTestClass.class));
    }

    public static class ExcelTestClass {

        @ExcelFile
        static String excelFile = "test.xlsx";
    }

    public static class CustomTestClass {
    }

}
