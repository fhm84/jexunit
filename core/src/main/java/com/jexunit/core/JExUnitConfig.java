package com.jexunit.core;

import com.jexunit.core.commands.DefaultCommands;
import com.jexunit.core.commands.validation.ValidationType;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JExUnit configuration. This will give access to the configuration running JExUnit. It's possible to override the
 * default configuration from a <i>"jexunit.properties"</i> file or via configuration set for example in the
 * <code>@BeforClass</code>-method of the test. The same way you can add your own/customized configuration to the
 * JExUnitConfig to get unified access to the whole test-configuration.<br>
 * For the internal representation <i>apache commons configuration</i> is used. So it should be easy to extend.
 *
 * @author fabian
 */
public class JExUnitConfig {

    private static final Logger LOG = Logger.getLogger(JExUnitConfig.class.getName());

    @Getter
    public enum ConfigKey {

        DATE_PATTERN("jexunit.datePattern", "dd.MM.yyyy"),

        BEFORE_EXCEL("jexunit.sheet.before", ""),

        AFTER_EXCEL("jexunit.sheet.after", ""),

        DATETIME_PATTERN("jexunit.dateTimePattern", "dd.MM.yyyy hh:mm:ss"),
        /**
         * keyword for identifying a command
         */
        COMMAND_STATEMENT("jexunit.command_statement", "command"),
        /**
         * prefix for the default commands (i.e. Testcase.ignore instead of ignore)
         */
        DEFAULTCOMMAND_PREFIX("jexunit.defaultcommand_prefix", ""),
        /**
         * prefix for test-command-classes
         */
        COMMAND_CLASS_PREFIX("jexunit.command.class_prefix", ""),
        /**
         * postfix for test-command-classes
         */
        COMMAND_CLASS_POSTFIX("jexunit.command.class_postfix", ""),

        /**
         * prefix for test-command-methods
         */
        COMMAND_METHOD_PREFIX("jexunit.command.method_prefix", ""),
        /**
         * postfix for test-command-methods
         */
        COMMAND_METHOD_POSTFIX("jexunit.command.method_postfix", ""),

        /**
         * Validation type (no validation, warn and remove invalid test commands from execution list or
         * (fast) fail on missing command implementation).
         */
        COMMAND_VALIDATION_TYPE("jexunit.command.validation.type", ValidationType.WARN.name());

        private final String key;
        private final String defaultConfig;

        ConfigKey(final String key, final String defaultConfig) {
            this.key = key;
            this.defaultConfig = defaultConfig;
        }

    }

    private static Map<String, Object> config;

    /**
     * Private constructor -> only static access.
     */
    private JExUnitConfig() {
    }

    static {
        // initialize the configuration on first access!
        init();
    }

    /**
     * Prepare and get the default configuration.
     *
     * @return default configuration
     */
    private static Map<String, Object> getDefaultConfiguration() {
        final Map<String, Object> config = new HashMap<>();

        for (final ConfigKey ck : ConfigKey.values()) {
            config.put(ck.getKey(), ck.getDefaultConfig());
        }
        for (final DefaultCommands dc : DefaultCommands.values()) {
            config.put(dc.getConfigKey(), dc.getDefaultValue());
        }

        return config;
    }

    /**
     * Initialize the configuration. This will be done only one times. If you initialize the configuration multiple
     * times, only the first time, the configuration will be prepared, read, ...
     */
    public static synchronized void init() {
        if (config == null) {
            config = new HashMap<>();
            InputStream in = JExUnitConfig.class.getClassLoader().getResourceAsStream("jexunit.properties");
            Properties prop = new Properties();
            if (in != null) {
                try {
                    prop.load(in);
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            config.putAll(getDefaultConfiguration());

            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                config.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
    }

    /**
     * Set the config property with given key.
     *
     * @param key   config key
     * @param value new value
     */
    public static synchronized void setConfigProperty(final String key, final Object value) {
        config.put(key, value);
    }

    /**
     * Get the configured property with the given key.
     *
     * @param key config key
     * @return the configured property value
     */
    public static String getStringProperty(final String key) {
        return String.valueOf(config.get(key));
    }

    /**
     * Get the configured property with the given ConfigKey.
     *
     * @param key ConfigKey
     * @return the configured propert value
     */
    public static String getStringProperty(final ConfigKey key) {
        return getStringProperty(key.getKey());
    }

    /**
     * Get the configured property (DefaultCommand) with the given key add prepend the configured prefix for the default
     * commands.
     *
     * @param defaultCommand the DefaultCommand to get the configured property for
     * @return the configured property value with the default command prefix prepended
     */
    public static String getDefaultCommandProperty(final DefaultCommands defaultCommand) {
        String conf = getStringProperty(defaultCommand.getConfigKey());
        final String defaultCommandPrefix = getStringProperty(ConfigKey.DEFAULTCOMMAND_PREFIX);
        if (defaultCommandPrefix != null && !defaultCommandPrefix.trim().isEmpty()) {
            conf = defaultCommandPrefix + conf;
        }
        return conf;
    }

}
