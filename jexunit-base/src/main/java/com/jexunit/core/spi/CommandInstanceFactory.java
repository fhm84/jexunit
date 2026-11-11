package com.jexunit.core.spi;

import java.util.Optional;

/**
 * Creates the instances of the command classes ({@code @TestCommand} on a class) and of the classes declaring
 * {@code @TestCommand} methods outside of the test class (command providers). Implement it to create them with a
 * dependency injection container (Spring, CDI, Guice, ...), so the commands get services or repositories injected.
 * <p>
 * Configure the implementation (with a public no-arg constructor) in <code>jexunit.properties</code>:
 * <pre>
 * jexunit.command.instance_factory=com.example.tests.SpringCommandInstanceFactory
 * </pre>
 * Without configuration, the {@link DefaultCommandInstanceFactory} creates the instances via their no-arg constructor.
 * <p>
 * A command class is created for each invocation, a command provider once per test group (worksheet). Commands
 * declared in the test class itself are invoked on the test instance (created by the test framework), so they don't
 * use the factory.
 * <p>
 * Example for Spring with JUnit 5 (the test class uses the <code>SpringExtension</code>):
 * <pre>
 * public class SpringCommandInstanceFactory implements CommandInstanceFactory {
 *     &#64;Override
 *     public &lt;T&gt; T create(Class&lt;T&gt; type, Context context) {
 *         ExtensionContext extensionContext = context.unwrap(ExtensionContext.class).orElseThrow(IllegalStateException::new);
 *         return SpringExtension.getApplicationContext(extensionContext).getAutowireCapableBeanFactory().createBean(type);
 *     }
 * }
 * </pre>
 *
 * @author fabian
 */
public interface CommandInstanceFactory {

    /**
     * Create an instance of the given type.
     *
     * @param type    the command class, or the class declaring command methods
     * @param context the running test
     * @param <T>     generic type
     * @return the new instance (not <code>null</code>)
     * @throws Exception if the instance cannot be created; the command fails then
     */
    <T> T create(Class<T> type, Context context) throws Exception;

    /**
     * The running test, for creating the instances.
     */
    interface Context {

        /**
         * Get the instance of the running test class.
         *
         * @return the test instance (JUnit 5), the instance of the test class extending <code>JExUnitBase</code> (JUnit
         * 4), or <code>null</code> (JUnit 4 with <code>@RunWith(JExUnit.class)</code>)
         */
        Object getTestInstance();

        /**
         * Get the context of the test framework, if it is of the given type: the <code>ExtensionContext</code> of the
         * test with JUnit 5. There is none with JUnit 4.
         *
         * @param type the expected type, e.g. <code>org.junit.jupiter.api.extension.ExtensionContext</code>
         * @param <T>  generic type
         * @return the context of the test framework, or empty if there is none of this type
         */
        <T> Optional<T> unwrap(Class<T> type);
    }

}
