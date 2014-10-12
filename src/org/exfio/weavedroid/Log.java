package org.exfio.weavedroid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class Log {
	private static Logger LOGGER = null;

	public static Logger getInstance() {
		if ( LOGGER == null ) {
			LOGGER = LoggerFactory.getLogger("org.exfio.weave");
		}
		return LOGGER;
	}

	public static void setLogLevel(String level) {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
		//TODO - Set log level for Android
	}

}
