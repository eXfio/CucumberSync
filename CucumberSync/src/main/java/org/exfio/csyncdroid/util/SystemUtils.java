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

import android.content.Context;
import android.content.pm.ApplicationInfo;

public class SystemUtils {

	public static Boolean debuggable = null;
	
	public static boolean isDebuggable(Context context) {
		if ( debuggable == null ) {
			debuggable =  ( (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE );			
		}
		return debuggable.booleanValue();
	}
}
