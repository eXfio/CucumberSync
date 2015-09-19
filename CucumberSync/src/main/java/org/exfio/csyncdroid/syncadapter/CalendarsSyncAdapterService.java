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
package org.exfio.csyncdroid.syncadapter;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.resource.LocalCalendar;
import org.exfio.csyncdroid.resource.LocalCollection;
import org.exfio.csyncdroid.resource.WeaveCalendar;
import org.exfio.csyncdroid.resource.WeaveCollection;

import java.util.HashMap;
import java.util.Map;

public class CalendarsSyncAdapterService extends Service {
	private static CalendarSyncAdapter syncAdapter;

	
	@Override
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new CalendarSyncAdapter(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		syncAdapter.close();
		syncAdapter = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder();
	}
	

	private static class CalendarSyncAdapter extends WeaveSyncAdapter {
		
		private CalendarSyncAdapter(Context context) {
			super(context);
		}

		@Override
		protected Map<LocalCollection<?>, WeaveCollection<?>> getSyncPairs(Account account, ContentProviderClient provider, WeaveClient weaveClient) throws WeaveException{
			
			AccountSettings settings = null;

			if ( account.type.equals(Constants.ACCOUNT_TYPE_LEGACYV5) ) {
				settings = new LegacyV5AccountSettings(getContext(), account);
			} else if ( account.type.equals(Constants.ACCOUNT_TYPE_FXACCOUNT) ) {
				settings = new FxAccountAccountSettings(getContext(), account);
			} else {
				settings = new ExfioPeerAccountSettings(getContext(), account);
			}

			Map<LocalCollection<?>, WeaveCollection<?>> map = new HashMap<LocalCollection<?>, WeaveCollection<?>>();

			try {
				for (LocalCalendar calendar : LocalCalendar.findAll(account, provider, settings)) {
					WeaveCollection<?> weave = new WeaveCalendar(weaveClient, Constants.CALENDAR_COLLECTION);
					map.put(calendar, weave);
				}

				return map;

			} catch (RemoteException e) {
				throw new WeaveException("Error getting local calendars - " + e.getMessage());
			} finally {
				//TODO - handle WeaveExceptions here?
			}
		}
	}
}
