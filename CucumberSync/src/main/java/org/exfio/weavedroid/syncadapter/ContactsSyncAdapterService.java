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
package org.exfio.weavedroid.syncadapter;

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
import org.exfio.weavedroid.Constants;
import org.exfio.weavedroid.resource.LocalAddressBook;
import org.exfio.weavedroid.resource.LocalCollection;
import org.exfio.weavedroid.resource.WeaveAddressBook;
import org.exfio.weavedroid.resource.WeaveCollection;

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
