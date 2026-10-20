package com.jexunit.core.commands;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.commands.annotation.TestCommand;
import com.jexunit.core.commands.validation.CommandValidator;
import com.jexunit.core.model.Metadata;
import com.jexunit.core.model.TestCase;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCommandScannerTest {

    private static final String PREFIX_KEY = JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getKey();
    private static final String REPORT_KEY = DefaultCommands.REPORT.getConfigKey();

    @After
    public void resetConfig() {
        JExUnitConfig.setConfigProperty(PREFIX_KEY, JExUnitConfig.ConfigKey.DEFAULTCOMMAND_PREFIX.getDefaultConfig());
        JExUnitConfig.setConfigProperty(REPORT_KEY, DefaultCommands.REPORT.getDefaultValue());
    }

    @Test
    public void shouldRegisterCommandMethodsOfAClassOnlyOnce() {
        final List<LogRecord> warnings = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getLevel() == Level.WARNING) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final Logger log = Logger.getLogger(TestCommandScanner.class.getName());
        log.addHandler(handler);
        try {
            // the scan reports each annotated method of the class
            final TestCommandScanner scanner = new TestCommandScanner();
            scanner.reportMethodAnnotation(TestCommand.class, CommandFixture.class.getName(), "first");
            scanner.reportMethodAnnotation(TestCommand.class, CommandFixture.class.getName(), "second");
        } finally {
            log.removeHandler(handler);
        }

        assertEquals(Collections.emptyList(), warnings);
        assertTrue(TestCommandScanner.isTestCommandValid("scannerfixturefirst"));
        assertTrue(TestCommandScanner.isTestCommandValid("scannerfixturesecond"));
    }

    @Test
    public void shouldLoadReportedClassesWithContextClassLoader() {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final RecordingClassLoader recordingClassLoader = new RecordingClassLoader(original);
        thread.setContextClassLoader(recordingClassLoader);
        try {
            new TestCommandScanner().reportTypeAnnotation(TestCommand.class, TypeCommandFixture.class.getName());
        } finally {
            thread.setContextClassLoader(original);
        }

        assertTrue(recordingClassLoader.loadedClasses.contains(TypeCommandFixture.class.getName()));
    }

    @Test
    public void shouldFindCommandsIndependentOfDefaultLocale() throws Exception {
        new TestCommandScanner().reportMethodAnnotation(TestCommand.class, CommandFixture.class.getName(), "first");
        final TestCase<Metadata> testCase = new TestCase<>(new Metadata());
        // upper case "I" is lower cased to the dotless "ı" in the turkish locale
        testCase.setTestCommand("SCANNERFIXTUREFIRST");
        final List<TestCase<?>> testCases = new ArrayList<>(Collections.singletonList(testCase));
        final AtomicBoolean notFound = new AtomicBoolean();
        final TestCommandHost host = new TestCommandHost() {
            @Override
            public Class<?> getTestType() {
                return null;
            }

            @Override
            public void runCommand(final TestCase<?> testCase) {
                notFound.set(true);
            }

            @Override
            public Object getTestClassInstance(final Class<?> declaringClass) {
                return null;
            }
        };

        final Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));
        try {
            CommandValidator.validateCommands(Collections.singletonList(new Object[]{testCases}));
            new TestCommandRunner(host).runTestCommand(testCase);
        } finally {
            Locale.setDefault(defaultLocale);
        }

        // the command is neither removed by the validation, nor missing when running it
        assertEquals(1, testCases.size());
        assertFalse(notFound.get());
    }

    @Test
    public void shouldAcceptBuiltInCommands() {
        assertTrue(TestCommandScanner.isTestCommandValid("disabled"));
        assertTrue(TestCommandScanner.isTestCommandValid("report"));
        assertFalse(TestCommandScanner.isTestCommandValid("unknown"));
    }

    @Test
    public void shouldAcceptBuiltInCommandsWithConfiguredPrefix() {
        JExUnitConfig.setConfigProperty(PREFIX_KEY, "jx.");

        assertTrue(TestCommandScanner.isTestCommandValid("jx.disabled"));
        assertTrue(TestCommandScanner.isTestCommandValid("jx.report"));
        assertFalse(TestCommandScanner.isTestCommandValid("disabled"));
    }

    @Test
    public void shouldAcceptRenamedBuiltInCommands() {
        JExUnitConfig.setConfigProperty(REPORT_KEY, "Log");

        // command names are compared in lower case
        assertTrue(TestCommandScanner.isTestCommandValid("log"));
        assertFalse(TestCommandScanner.isTestCommandValid("report"));
    }

    public static class CommandFixture {

        @TestCommand("scannerFixtureFirst")
        public void first() {
        }

        @TestCommand("scannerFixtureSecond")
        public void second() {
        }
    }

    @TestCommand("scannerTypeFixture")
    public static class TypeCommandFixture {

        public void run() {
        }
    }

    private static class RecordingClassLoader extends ClassLoader {

        private final List<String> loadedClasses = new ArrayList<>();

        RecordingClassLoader(final ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            loadedClasses.add(name);
            return super.loadClass(name, resolve);
        }
    }

}
