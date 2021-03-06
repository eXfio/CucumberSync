/*
 * Copyright (C) 2015 Gerry Healy <nickel_chrome@exfio.org> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This program is derived from DavDroid, Copyright (C) 2014 Richard Hirner, bitfire web engineering
 * DavDroid is distributed under the terms of the GNU Public License v3.0, https://github.com/bitfireAT/davdroid
 */
package org.exfio.csyncdroid.util;

import org.exfio.csyncdroid.BuildConfig;

import java.lang.IllegalArgumentException;
import java.util.logging.Level;

public class Log extends org.exfio.weave.util.Log {

	static {
		init(BuildConfig.LOG_LEVEL);
	}

	public static Level toJavaUtilLevel(String level) throws IllegalArgumentException {
		if ( level.toLowerCase().equals("error") ) {
			return java.util.logging.Level.SEVERE;
		} else if ( level.toLowerCase().equals("warn") ) {
			return java.util.logging.Level.WARNING;		
		} else if ( level.toLowerCase().equals("info") ) {
			return java.util.logging.Level.INFO;
		} else if ( level.toLowerCase().equals("debug") ) {
			return java.util.logging.Level.FINE;
		} else if ( level.toLowerCase().equals("trace") ) {
			return java.util.logging.Level.FINEST;
		} else {
			throw new IllegalArgumentException(String.format("Log level '%s' not recognised", level));
		}	
	}

	private static void setWeaveLogLevel(String level) {

		//Enable http logging if level is debug or trace
		//if ( level.toLowerCase().matches("debug|trace") ) {

			//Apache HttpClient officially ported to Android as of v4.3 uses android logging directly hence need to use adb only
			//$ adb shell setprop log.tag.HttpClient DEBUG

			//Android vX fork of Apache HttpClient
			java.util.logging.Logger.getLogger("httpclient.wire").setLevel(toJavaUtilLevel(level));
			java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(toJavaUtilLevel(level));
			java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(toJavaUtilLevel(level));
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", level);
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.content", level);
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", level);

			//Log level not honoured by logcat hence we also need to set log level via adb
			//$ adb shell setprop log.tag.httpclient.wire.content DEBUG
			//$ adb shell setprop log.tag.httpclient.wire.header DEBUG

			//Android vY fork of Apache HttpClient
			java.util.logging.Logger.getLogger("org.apache.http").setLevel(toJavaUtilLevel(level));
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(toJavaUtilLevel(level));
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(toJavaUtilLevel(level));
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", level);
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", level);
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", level);

			//Log level not honoured by logcat hence we also need to set log level via adb
			//$ adb shell setprop log.tag.org.apache.http.wire DEBUG
			//$ adb shell setprop log.tag.org.apache.http.headers DEBUG

			//fxaclient
			//org.mozilla.gecko.background.common.log.Logger.init(level);
			//org.mozilla.gecko.background.common.log.Logger.setLogLevel("exfio.fxaclient", level);
			java.util.logging.Logger.getLogger("exfio").setLevel(toJavaUtilLevel(level));
			java.util.logging.Logger.getLogger("exfio.fxaclient").setLevel(toJavaUtilLevel(level));
			System.setProperty("org.apache.commons.logging.simplelog.log.exfio", level);
			System.setProperty("org.apache.commons.logging.simplelog.log.exfio.fxaclient", level);

		//}

	}

	public static void init(String level) {
		org.exfio.weave.util.Log.init(level);
		
		//Explicitly set level for default logger
		
		//Android logging has some peculiarities
		//1) Apache commons logging defaults to java.util.logging on Android hence we need to set this level
		java.util.logging.Logger.getLogger(logtag).setLevel(toJavaUtilLevel(level));

		//2) Log level not honoured by logcat hence we also need to set log level via adb
		//$ adb shell setprop log.tag.LOGGER LEVEL

		//3) Set weave log levels
		setWeaveLogLevel(level);
	}
	
	public static void setLogLevel(String logger, String level) {
		org.exfio.weave.util.Log.setLogLevel(logger, level);

		//Android logging has some peculiarities
		//1) Apache commons logging defaults to java.util.logging on Android hence we need to set this level
		java.util.logging.Logger.getLogger(logger).setLevel(toJavaUtilLevel(level));

		//2) Log level not honoured by logcat hence we also need to set log level via adb
		//$ adb shell setprop log.tag.LOGGER LEVEL

		//3) Set weave log levels
		setWeaveLogLevel(level);
	}

}
