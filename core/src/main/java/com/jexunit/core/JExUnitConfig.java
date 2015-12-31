package com.jexunit.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.jexunit.core.commands.DefaultCommands;

/**
 * JExUnit configuration. This will give access to the configuration running JExUnit. It's possible to override the
 * default configuration from a <i>"jexunit.properties"</i> file or via configuration set for example in the
 * <code>@BeforClass</code>-method of the test. The same way you can add your own/customized configuration to the
 * JExUnitConfig to get unified access to the whole test-configuration.<br>
 * For the internal representation <i>apache commons configuration</i> is used. So it should be easy to extend.
 * 
 * @author fabian
 *
 */
public class JExUnitConfig {

	private static final Logger LOG = Logger.getLogger(JExUnitConfig.class.getName());

	public static final String DATE_PATTERN = "jexunit.datePattern";

	/**
	 * keyword for identifying a command
	 */
	public static final String COMMAND_STATEMENT = "jexunit.command_statement";
	/**
	 * prefix for the default commands (i.e. Testcase.ignore instead of ignore)
	 */
	public static final String DEFAULTCOMMAND_PREFIX = "jexunit.defaultcommand_prefix";

	/**
	 * prefix for test-command-classes
	 */
	public static final String COMMAND_CLASS_PREFIX = "jexunit.command.class_prefix";
	/**
	 * postfix for test-command-classes
	 */
	public static final String COMMAND_CLASS_POSTFIX = "jexunit.command.class_postfix";

	/**
	 * prefix for test-command-methods
	 */
	public static final String COMMAND_METHOD_PREFIX = "jexunit.command.method_prefix";
	/**
	 * postfix for test-command-methods
	 */
	public static final String COMMAND_METHOD_POSTFIX = "jexunit.command.method_postfix";

	private static CompositeConfiguration config;

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
	private static Configuration getDefaultConfiguration() {
		Map<String, Object> config = new HashMap<>();

		config.put(DATE_PATTERN, "dd.MM.yyyy");
		config.put(COMMAND_STATEMENT, "command");
		config.put(DEFAULTCOMMAND_PREFIX, "");
		config.put(DefaultCommands.DISABLED.getConfigKey(), "disabled");
		config.put(DefaultCommands.REPORT.getConfigKey(), "report");
		config.put(DefaultCommands.EXCEPTION_EXCPECTED.getConfigKey(), "exception");
		config.put(DefaultCommands.BREAKPOINT.getConfigKey(), "breakpoint");
		config.put(DefaultCommands.COMMENT.getConfigKey(), "comment");

		return new MapConfiguration(config);
	}

	/**
	 * Initialize the configuration. This will be done only one times. If you initialize the configuration multiple
	 * times, only the first time, the configuration will be prepared, read, ...
	 */
	public static void init() {
		if (config == null) {
			config = new CompositeConfiguration(getDefaultConfiguration());
			config.setThrowExceptionOnMissing(false);

			URL jexunitProperties = ConfigurationUtils.locate("jexunit.properties");
			if (jexunitProperties != null) {
				// properties file is optional, so only load if exists!
				try {
					config.addConfiguration(new PropertiesConfiguration(jexunitProperties));
				} catch (ConfigurationException e) {
					LOG.log(Level.WARNING, "ConfigurationException loading the jexunit.properties file.", e);
				}
			}
		}
	}

	/**
	 * Register an additional Configuration implementation. This can be any kind of configuration. For more information
	 * see <i>apache commons configuration</i>.
	 * 
	 * @param cfg
	 *            the configuration to register/add
	 */
	public static synchronized void registerConfig(Configuration cfg) {
		config.addConfiguration(cfg);
	}

	/**
	 * Set the config property with given key.
	 * 
	 * @param key
	 *            config key
	 * @param value
	 *            new value
	 */
	public static synchronized void setConfigProperty(String key, Object value) {
		config.setProperty(key, value);
	}

	/**
	 * Get the configured property with the given key.
	 * 
	 * @param key
	 *            config key
	 * @return the configured property value
	 */
	public static String getStringProperty(String key) {
		return config.getString(key);
	}

	/**
	 * Get the configured property (DefaultCommand) with the given key add prepend the configured prefix for the default
	 * commands.
	 * 
	 * @param defaultCommand
	 *            the DefaultCommand to get the configured property for
	 * @return the configured property value with the default command prefix prepended
	 */
	public static String getDefaultCommandProperty(DefaultCommands defaultCommand) {
		String conf = config.getString(defaultCommand.getConfigKey());
		String defaultCommandPrefix = config.getString(DEFAULTCOMMAND_PREFIX);
		if (defaultCommandPrefix != null && !defaultCommandPrefix.trim().isEmpty()) {
			conf = defaultCommandPrefix + conf;
		}
		return conf;
	}

	/**
	 * Get the configured property with the given key.
	 * 
	 * @param key
	 *            config key
	 * @return the configured property value
	 */
	public static Object getProperty(String key) {
		return config.getProperty(key);
	}

}
