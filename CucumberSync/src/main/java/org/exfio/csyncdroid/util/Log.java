/*******************************************************************************
 * Copyright (c) 2014 Gerry Healy <nickel_chrome@mac.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Based on DavDroid:
 *     Richard Hirner (bitfire web engineering)
 * 
 * Contributors:
 *     Gerry Healy <nickel_chrome@mac.com> - Initial implementation
 ******************************************************************************/
package org.exfio.csyncdroid.util;

import java.lang.IllegalArgumentException;
import java.util.logging.Level;

public class Log extends org.exfio.weave.util.Log {

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
			return java.util.logging.Level.FINER;
		} else {
			throw new IllegalArgumentException(String.format("Log level '%s' not recognised", level));
		}	
	}
	
	public static void init(String level) {
		org.exfio.weave.util.Log.init(level);
		
		//Explicitly set level for default logger
		
		//Android logging has some peculiarities
		//1) Apache commons logging defaults to java.util.logging on Android hence we need to set this level
		java.util.logging.Logger.getLogger(logtag).setLevel(toJavaUtilLevel(level));
		//java.util.logging.Logger.getLogger(logtag).setLevel(java.util.logging.Level.FINEST);
		
		//2) Log level not honoured by logcat hence we also need to set log level via adb
		//$ adb shell setprop log.tag.LOGGER LEVEL
		
		//Enable http logging if level is debug or trace
		if ( level.toLowerCase().matches("debug|trace") ) {
			
			//Apache HttpClient officially ported to Android as of v4.3 uses android logging directly hence need to use adb only
			//$ adb shell setprop log.tag.HttpClient DEBUG			
			
			//Android vX fork of Apache HttpClient
			java.util.logging.Logger.getLogger("httpclient.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(java.util.logging.Level.FINEST);
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.content", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

			//Log level not honoured by logcat hence we also need to set log level via adb
			//$ adb shell setprop log.tag.httpclient.wire.content DEBUG
			//$ adb shell setprop log.tag.httpclient.wire.header DEBUG

			//Android vY fork of Apache HttpClient
			java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);			 
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");

			//Log level not honoured by logcat hence we also need to set log level via adb
			//$ adb shell setprop log.tag.org.apache.http.wire DEBUG
			//$ adb shell setprop log.tag.org.apache.http.headers DEBUG

			//fxaclient
			java.util.logging.Logger.getLogger("exfio").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("exfio.fxaclient").setLevel(java.util.logging.Level.FINEST);
			System.setProperty("org.apache.commons.logging.simplelog.log.exfio", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.exfio.fxaclient", "debug");
		}
	}
	
	public static void setLogLevel(String logger, String level) {
		org.exfio.weave.util.Log.setLogLevel(logger, level);

		//Android logging has some peculiarities
		//1) Apache commons logging defaults to java.util.logging on Android hence we need to set this level
		java.util.logging.Logger.getLogger(logger).setLevel(toJavaUtilLevel(level));
		//java.util.logging.Logger.getLogger(logger).setLevel(java.util.logging.Level.FINEST);
		
		//2) Log level not honoured by logcat hence we also need to set log level via adb
		//$ adb shell setprop log.tag.LOGGER LEVEL
	}

}
