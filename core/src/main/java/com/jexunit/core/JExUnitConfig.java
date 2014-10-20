package com.jexunit.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * JExUnit configuration. This will give access to the configuration running JExUnit.
 * 
 * @author fabian
 *
 */
public class JExUnitConfig {

	public static String DATE_PATTERN = "jexunit.datePattern";
	// TODO: add ability to override the default command-names?

	private static CompositeConfiguration config;

	/**
	 * Private constructor -> only static access.
	 */
	private JExUnitConfig() {
	}

	private static Configuration getDefaultConfiguration() {
		Map<String, Object> config = new HashMap<>();

		config.put(DATE_PATTERN, "dd.MM.yyyy");

		return new MapConfiguration(config);
	}

	public static void init() {
		// initialize the configuration.
		if (config == null) {
			config = new CompositeConfiguration(getDefaultConfiguration());
			try {
				config.addConfiguration(new PropertiesConfiguration("jexunit.properties"));
				config.setThrowExceptionOnMissing(false);
			} catch (ConfigurationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static synchronized void registerConfig(Configuration cfg) {
		config.addConfiguration(cfg);
	}

	public static synchronized void setConfigProperty(String key, Object value) {
		config.setProperty(key, value);
	}

	public static String getStringProperty(String key) {
		return config.getString(key);
	}

	public static Object getProperty(String key) {
		return config.getProperty(key);
	}
}
