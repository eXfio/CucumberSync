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
package org.exfio.csyncdroid;

public class Constants {
	public static final String APP_VERSION            = "1.3-beta-1";
	public static final String ACCOUNT_TYPE_LEGACYV5  = "org.exfio.csyncdroid.legacyv5";
	public static final String ACCOUNT_TYPE_EXFIOPEER = "org.exfio.csyncdroid.exfiopeer";
	public static final String ACCOUNT_TYPE_FXACCOUNT = "org.exfio.csyncdroid.fxaccount";
	public static final String META_COLLECTION        = "meta";
	public static final String META_ID                = "exfio";
	public static final String ADDRESSBOOK_COLLECTION = "exfiocontacts";
	public static final String CALENDAR_COLLECTION    = "exfiocalendar";
	public static final String WEB_URL_HELP           = "http://cucumbersync.exfio.org";
	public static enum ResourceType {
		ADDRESS_BOOK,
		CALENDAR
	}
}
