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
package org.exfio.weavedroid;

public class Constants {
	public static final String APP_VERSION            = "1.1-alpha-2";
	public static final String ACCOUNT_TYPE_LEGACYV5  = "org.exfio.weavedroid.legacyv5";	
	public static final String ACCOUNT_TYPE_EXFIOPEER = "org.exfio.weavedroid.exfiopeer";	
	public static final String ACCOUNT_TYPE_FXACCOUNT = "org.exfio.weavedroid.fxaccount";	
	public static final String META_COLLECTION        = "meta";
	public static final String META_ID                = "exfio";
	public static final String ADDRESSBOOK_COLLECTION = "exfiocontacts";
	public static final String WEB_URL_HELP           = "http://cucumbersync.exfio.org";
	public static enum ResourceType {
		ADDRESS_BOOK,
		CALENDAR
	}
}
