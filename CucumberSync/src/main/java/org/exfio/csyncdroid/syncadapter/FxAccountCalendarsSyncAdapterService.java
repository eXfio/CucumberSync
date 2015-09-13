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
package org.exfio.csyncdroid.syncadapter;

import org.exfio.csyncdroid.Constants;

import lombok.Getter;

public class FxAccountCalendarsSyncAdapterService extends CalendarsSyncAdapterService {
	@Getter private static final String accountType = Constants.ACCOUNT_TYPE_FXACCOUNT;
}
