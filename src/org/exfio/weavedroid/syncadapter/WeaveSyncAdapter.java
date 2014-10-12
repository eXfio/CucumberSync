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
package org.exfio.weavedroid.syncadapter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

//import org.apache.http.HttpStatus;

import lombok.Getter;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveStorageV5Params;

import org.exfio.weavedroid.resource.LocalCollection;
import org.exfio.weavedroid.resource.LocalStorageException;
import org.exfio.weavedroid.resource.WeaveCollection;


public abstract class WeaveSyncAdapter extends AbstractThreadedSyncAdapter implements Closeable {
	private final static String TAG = "exfio.WeaveSyncAdapter";
	
	@Getter private static String androidID;
	
	protected AccountManager accountManager;
	
	private WeaveClient weaveClient = null;
	
	public WeaveSyncAdapter(Context context) {
		super(context, true);
		
		synchronized(this) {
			if (androidID == null)
				androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		}
		
		accountManager = AccountManager.get(context);
	}
	
	@Override
	public void close() {
		// apparently may be called from a GUI thread
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				closeWeaveClient();
				return null;
			}
		}.execute();
	}
	
	private void closeWeaveClient() {
		if (weaveClient == null) {
			Log.w(TAG, "Couldn't close WeaveClient, not instansiated");
			return;
		}

		try {
			weaveClient.close();
			weaveClient = null;
		} catch (IOException e) {
			Log.w(TAG, "Couldn't close WeaveClient", e);
		}		
	}
	
	protected abstract Map<LocalCollection<?>, WeaveCollection<?>> getSyncPairs(Account account, ContentProviderClient provider, WeaveClient weaveClient) throws WeaveException;
	
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,	ContentProviderClient provider, SyncResult syncResult) {
		Log.i(TAG, "Performing sync for authority " + authority);
		
		// set class loader for iCal4j ResourceLoader
		Thread.currentThread().setContextClassLoader(getContext().getClassLoader());
		
		AccountSettings settings = new AccountSettings(getContext(), account);

		//TODO - handle user defined Weave Storage version
		
		//DEBUG only
		org.exfio.weavedroid.Log.init("debug");
		org.exfio.weavedroid.Log.getInstance().info("Initialised logger");
		org.exfio.weavedroid.Log.getInstance().debug("Debug message");
		
		//get weave account params
		WeaveStorageV5Params params = new WeaveStorageV5Params();
		params.baseURL  = settings.getBaseURL();
		params.user     = settings.getUserName();
		params.password = settings.getPassword();
		params.syncKey  = settings.getSyncKey();
		
		//TODO - validate weave account settings
		if (params.baseURL == null) {
			Log.e(TAG, "Weave account settings invalid");
			return;
		}
		
		//Initialise weave client
		try {
			weaveClient = WeaveClient.getInstance(org.exfio.weave.client.WeaveClient.StorageVersion.v5);
			weaveClient.init(params);
		} catch (WeaveException e) {
			Log.e(TAG, e.getMessage());
			closeWeaveClient();
			return;
		}
		
		//getSyncPairs() overridden by implementing classes, i.e. ContactsSyncAdapter 
		Map<LocalCollection<?>, WeaveCollection<?>> syncCollections = null;		
		try {
			syncCollections = getSyncPairs(account, provider, weaveClient);
		} catch (WeaveException e) {
			Log.e(TAG, e.getMessage());
			closeWeaveClient();
			return;
		}
		
		if (syncCollections == null)
			Log.i(TAG, "Nothing to synchronize");
		else {
			try {
				// prevent weave http client shutdown until we're ready
				weaveClient.lock();
				
				for (Map.Entry<LocalCollection<?>, WeaveCollection<?>> entry : syncCollections.entrySet())
					new SyncManager(entry.getKey(), entry.getValue()).synchronize(extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL), syncResult);
				
			} catch (WeaveException ex) {
				syncResult.stats.numParseExceptions++;
				Log.e(TAG, "Invalid Weave response", ex);
				
			//TODO - log sync status
			
//			} catch (HttpException ex) {
//				if (ex.getCode() == HttpStatus.SC_UNAUTHORIZED) {
//					Log.e(TAG, "HTTP Unauthorized " + ex.getCode(), ex);
//					syncResult.stats.numAuthExceptions++;
//				} else if (ex.isClientError()) {
//					Log.e(TAG, "Hard HTTP error " + ex.getCode(), ex);
//					syncResult.stats.numParseExceptions++;
//				} else {
//					Log.w(TAG, "Soft HTTP error " + ex.getCode() + " (Android will try again later)", ex);
//					syncResult.stats.numIoExceptions++;
//				}
				
			} catch (LocalStorageException ex) {
				syncResult.databaseError = true;
				Log.e(TAG, "Local storage (content provider) exception", ex);
//			} catch (IOException ex) {
//				syncResult.stats.numIoExceptions++;
//				Log.e(TAG, "I/O error (Android will try again later)", ex);
			} finally {
				// close weave http client
				weaveClient.unlock();
				closeWeaveClient();
			}
		}
	}
}
