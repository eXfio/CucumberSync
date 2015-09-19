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

import java.util.Map;
import java.util.HashMap;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.resource.LocalAddressBook;
import org.exfio.csyncdroid.resource.LocalCollection;
import org.exfio.csyncdroid.resource.WeaveAddressBook;
import org.exfio.csyncdroid.resource.WeaveCollection;

public class ContactsSyncAdapterService extends Service {
	private static ContactsSyncAdapter syncAdapter;

	
	@Override
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new ContactsSyncAdapter(getApplicationContext());
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
	

	private static class ContactsSyncAdapter extends WeaveSyncAdapter {
		
		private ContactsSyncAdapter(Context context) {
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
			
			try {
				LocalCollection<?> database = new LocalAddressBook(account, provider, settings);
				WeaveCollection<?> weave = new WeaveAddressBook(weaveClient, Constants.ADDRESSBOOK_COLLECTION);
				
				Map<LocalCollection<?>, WeaveCollection<?>> map = new HashMap<LocalCollection<?>, WeaveCollection<?>>();
				map.put(database, weave);
				
				return map;
			} finally {
				//TODO - handle WeaveException here?
			}
		}
	}
}
