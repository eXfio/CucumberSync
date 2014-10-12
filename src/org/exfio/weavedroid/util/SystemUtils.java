/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package org.exfio.weavedroid.util;

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
