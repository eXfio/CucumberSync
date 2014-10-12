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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import lombok.Getter;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClientFactory;
import org.exfio.weave.client.WeaveClientV5Params;
import org.exfio.weave.ext.clientauth.ClientAuth;
import org.exfio.weave.ext.clientauth.ClientAuthRequestMessage;
import org.exfio.weave.ext.comm.Message;
import org.exfio.weavedroid.ReceivedClientAuth;
import org.exfio.weavedroid.MainActivity;
import org.exfio.weavedroid.PendingClientAuth;
import org.exfio.weavedroid.R;
import org.exfio.weavedroid.resource.LocalCollection;
import org.exfio.weavedroid.resource.LocalStorageException;
import org.exfio.weavedroid.resource.WeaveCollection;
import org.exfio.weavedroid.util.SystemUtils;


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
		if ( SystemUtils.isDebuggable(getContext()) ) {
			org.exfio.weavedroid.util.Log.init("debug");
		}
		
		Log.d(TAG, String.format("Weave credentials - username: %s, password: %s, synckey: %s", settings.getUserName(), settings.getPassword(), settings.getSyncKey()));
		
		//get weave account params
		WeaveClientV5Params params = new WeaveClientV5Params();
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
			weaveClient = WeaveClientFactory.getInstance(params);
		} catch (WeaveException e) {
			Log.e(TAG, e.getMessage());
			closeWeaveClient();
			return;
		}
		
		Log.i(TAG, String.format("Checking messages"));

		Message[] messages = null;
		
		try {
			
			//Initialise sqldroid jdbc provider
			Class.forName("org.sqldroid.SQLDroidDriver");
            String databasePath = getContext().getDatabasePath(settings.getGuid()).getAbsolutePath();

			Log.d(TAG, String.format("Database path: %s", databasePath));

			ClientAuth auth = new ClientAuth(weaveClient, databasePath);

			String curStatus = auth.getAuthStatus();
			messages = auth.processClientAuthMessages();
			String newStatus = auth.getAuthStatus();

			if ( curStatus != null && curStatus.equals("pending") ) {
				
				//If client has been authorised update configuration
				if ( newStatus.equals("authorised") ) {
					//Update synckey, re-initialise weave client and notify user clientauth request approved

					Log.i(TAG, String.format("Client auth request approved by '%s'", auth.getAuthBy()));

					accountManager.setPassword(account, AccountSettings.encodePassword(settings.getPassword(), auth.getSyncKey()));
					
					params.syncKey = auth.getSyncKey();
					weaveClient.init(params);

					//Notify user that clientauth request has been approved
					displayNotificationApprovedClientAuth(account.name, auth.getAuthBy());
					
				} else if ( newStatus.equals("pending") ) {
					//Client not yet authenticated
					
					Log.i(TAG, String.format("Client auth request pending with authcode '%s'", auth.getAuthCode()));
					
					//Notify user of the authcode to be entered in authorising device
					displayNotificationPendingClientAuth(account.name, auth.getAuthCode());
					
					return;
				}
			}
			
		} catch(Exception e) {
			Log.e(TAG, String.format("%s - %s", e.getClass().getName() , e.getMessage()));
			return;
		}
		
		Log.d(TAG, String.format("Processing %d pending client auth request messages", messages.length));
		
		for (Message msg: messages) {
			ClientAuthRequestMessage caMsg = (ClientAuthRequestMessage)msg;

			Log.i(TAG, String.format("Client auth request pending approval for client '%s'", caMsg.getClientName()));

			//Notify user that a clientauth request is waiting for approval
			displayNotificationReceivedClientAuth(account.name, caMsg);
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
				
			//FIXME - log sync status
			
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
	
	protected void displayNotificationReceivedClientAuth(String accountName, ClientAuthRequestMessage msg) {
		Log.d(TAG, "displayNotificationApproveClientAuth()");
		
		int notificationId = 111;
		
		// Invoking the default notification service
		NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this.getContext());	

		mBuilder.setContentTitle("Authentication request received");
		mBuilder.setContentText(String.format("'%s' is requesting authentication for WeaveDroid account '%s'", msg.getClientName(), accountName));
		mBuilder.setTicker("Authentication request received");
		mBuilder.setSmallIcon(R.drawable.ic_launcher);

		// Increase notification number every time a new notification arrives 
		mBuilder.setNumber(1);

		// Creates an explicit intent for an Activity in your app 
		Intent resultIntent = new Intent(this.getContext(), ReceivedClientAuth.class);
		resultIntent.putExtra(ReceivedClientAuth.KEY_EXTRA_NOTIFICATIONID, notificationId);
		resultIntent.putExtra(ReceivedClientAuth.KEY_EXTRA_ACCOUNTNAME, accountName);
		resultIntent.putExtra(ReceivedClientAuth.KEY_EXTRA_SESSIONID, msg.getMessageSessionId());
		resultIntent.putExtra(ReceivedClientAuth.KEY_EXTRA_CLIENTNAME, msg.getClientName());

		//This ensures that navigating backward from the Activity leads out of the app to Home page
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getContext());

		// Adds the back stack for the Intent
		stackBuilder.addParentStack(ReceivedClientAuth.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
			0,
			PendingIntent.FLAG_ONE_SHOT //can only be used once
		);
		
		// start the activity when the user clicks the notification text
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager nm = (NotificationManager) this.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

		// pass the Notification object to the system 
		nm.notify(notificationId, mBuilder.build());
	}

	protected void displayNotificationPendingClientAuth(String accountName, String authCode) {
		Log.d(TAG, "displayNotificationPendingClientAuth()");
		
		int notificationId = 112;
		
		// Invoking the default notification service
		NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this.getContext());	

		mBuilder.setContentTitle("Pending authentication request");
		mBuilder.setContentText(String.format("Enter authcode '%s' on an authenticated device to approve acces to WeaveDroid account '%s'", authCode, accountName));
		mBuilder.setTicker("Pending authentication request");
		mBuilder.setSmallIcon(R.drawable.ic_launcher);

		// Increase notification number every time a new notification arrives 
		mBuilder.setNumber(1);

		// Creates an explicit intent for an Activity in your app 
		Intent resultIntent = new Intent(this.getContext(), PendingClientAuth.class);
		resultIntent.putExtra(PendingClientAuth.KEY_EXTRA_NOTIFICATIONID, notificationId);
		resultIntent.putExtra(PendingClientAuth.KEY_EXTRA_ACCOUNTNAME, accountName);
		resultIntent.putExtra(PendingClientAuth.KEY_EXTRA_AUTHCODE, authCode);

		//This ensures that navigating backward from the Activity leads out of the app to Home page
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getContext());

		// Adds the back stack for the Intent
		stackBuilder.addParentStack(PendingClientAuth.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
			0,
			PendingIntent.FLAG_ONE_SHOT //can only be used once
		);
		
		// start the activity when the user clicks the notification text
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager nm = (NotificationManager) this.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

		// pass the Notification object to the system 
		nm.notify(notificationId, mBuilder.build());
	}

	protected void displayNotificationApprovedClientAuth(String accountName, String authBy) {
		Log.d(TAG, "displayNotificationApprovedClientAuth()");
		
		int notificationId = 113;
		
		// Invoking the default notification service
		NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this.getContext());	

		mBuilder.setContentTitle("Authentication request approved");
		mBuilder.setContentText(String.format("'%s' has approved authentication request for WeaveDroid account '%s'", authBy, accountName));
		mBuilder.setTicker("Authentication request approved");
		mBuilder.setSmallIcon(R.drawable.ic_launcher);

		// Increase notification number every time a new notification arrives 
		mBuilder.setNumber(1);
		
		// No activity when user selects notification
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this.getContext(), 0, new Intent(), 0);
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager nm = (NotificationManager) this.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

		// pass the Notification object to the system 
		nm.notify(notificationId, mBuilder.build());
	}

}
