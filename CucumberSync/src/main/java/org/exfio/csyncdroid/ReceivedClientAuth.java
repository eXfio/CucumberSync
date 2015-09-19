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

import android.accounts.AccountManager;
import android.accounts.Account;
import android.os.Bundle;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.exfio.weave.WeaveException;
import org.exfio.weave.account.exfiopeer.AuthcodeVerificationFailedException;
import org.exfio.weave.account.exfiopeer.ExfioPeerV1;
import org.exfio.weave.account.legacy.LegacyV5AccountParams;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClientFactory;
import org.exfio.csyncdroid.syncadapter.ExfioPeerAccountSettings;
import org.exfio.csyncdroid.util.SystemUtils;

public class ReceivedClientAuth extends Activity implements LoaderCallbacks<CSyncReturnValue> {
	
	private final static String TAG = "ReceivedClientAuth";
	
	public static final String KEY_EXTRA_NOTIFICATIONID = "notificationid";
	public static final String KEY_EXTRA_ACCOUNTNAME    = "accountname";
	public static final String KEY_EXTRA_SESSIONID      = "sessionid";
	public static final String KEY_EXTRA_CLIENTNAME     = "clientname";
	
	public static final String KEY_LOADER_ACCOUNTNAME   = "accountname";
	public static final String KEY_LOADER_SESSIONID     = "sessionid";
	public static final String KEY_LOADER_APPROVED      = "approved";
	public static final String KEY_LOADER_AUTHCODE      = "authcode";
	
	EditText editAuthCode;
	TextView textError;

	String accountName;
	String sessionId;
	String clientName;

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		final String TAG = "csyncdroid.ReceivedClientAuth";
		
		// First check we have received extras from notification
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Log.e(TAG, "Error extracting notification data");
			return;
		}
		
		// Remove notification now user has acknowledged it
		int notificationId = extras.getInt(KEY_EXTRA_NOTIFICATIONID);
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(notificationId);

		// Render UI elements
		setContentView(R.layout.received_client_auth);

		editAuthCode = (EditText) findViewById(R.id.received_ca_authcode);
		textError    = (TextView) findViewById(R.id.received_ca_error);

		accountName = extras.getString(KEY_EXTRA_ACCOUNTNAME);
		sessionId   = extras.getString(KEY_EXTRA_SESSIONID);
		clientName  = extras.getString(KEY_EXTRA_CLIENTNAME);
		
		TextView t = (TextView)findViewById(R.id.received_ca_text);		
		t.setText(String.format("'%s' is requesting authentication for CucumberSync account '%s'", clientName, accountName));

		//We need to assign context to variable for use in closure
		final ReceivedClientAuth thisObject = this;

		//Run in background as inet access not allowed on main thread, i.e. extend AsyncTask or AsyncTaskLoader		
		
	    final OnClickListener btnApproveListener = new OnClickListener() {
	        public void onClick(View v) {
	    		Bundle arguments = new Bundle();
	    		arguments.putString(KEY_LOADER_ACCOUNTNAME, accountName);
	    		arguments.putString(KEY_LOADER_SESSIONID, sessionId);
	    		arguments.putBoolean(KEY_LOADER_APPROVED, true);
	    		arguments.putString(KEY_LOADER_AUTHCODE, editAuthCode.getText().toString());
	    		
	        	getLoaderManager().destroyLoader(0);	        	
	    		getLoaderManager().initLoader(0, arguments, thisObject).forceLoad();
	        }
	    };
		Button btnApprove = (Button) findViewById(R.id.received_ca_approve);
		btnApprove.setOnClickListener(btnApproveListener);

	    final OnClickListener btnRejectListener = new OnClickListener() {
	        public void onClick(View v) {
	        	Bundle arguments = new Bundle();
	        	arguments.putString(KEY_LOADER_ACCOUNTNAME, accountName);
	        	arguments.putString(KEY_LOADER_SESSIONID, sessionId);
	        	arguments.putBoolean(KEY_LOADER_APPROVED, false);
	        	arguments.putString(KEY_LOADER_AUTHCODE, null);
	        	
	        	getLoaderManager().destroyLoader(0);	        	
	        	getLoaderManager().initLoader(0, arguments, thisObject).forceLoad();
	        }
	    };
		Button btnReject = (Button) findViewById(R.id.received_ca_reject);
		btnReject.setOnClickListener(btnRejectListener);

	}

	public Loader<CSyncReturnValue> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader()");
		return new ClientAuthResponseLoader(this, args);
	}

	public void onLoadFinished(Loader<CSyncReturnValue> loader, CSyncReturnValue retval) {
		Log.d(TAG, "onLoadFinished()");

		textError.setText("");

		if (retval.getMessage() != null) {
			if ( retval.getException() instanceof AuthcodeVerificationFailedException ) {
				textError.setText(retval.getException().getMessage());
			} else {
				Toast.makeText(this, retval.getMessage(), Toast.LENGTH_LONG).show();
			}
		} else {
			//Close activity
			finish();
		}
	}

	public void onLoaderReset(Loader<CSyncReturnValue> loader) {
		Log.d(TAG, "onLoaderReset()");
	}

	static class ClientAuthResponseLoader extends AsyncTaskLoader<CSyncReturnValue> {
		private static final String TAG = "exfio.ClientAuthResponseLoader";
		Bundle args;
		
		public ClientAuthResponseLoader(Context context, Bundle args) {
			super(context);
			this.args = args;
		}

		@Override
		public CSyncReturnValue loadInBackground() {
			Log.d(TAG, "LoadInBackgroud()");
			
			//DEBUG only
			if ( SystemUtils.isDebuggable(getContext()) ) {
				org.exfio.csyncdroid.util.Log.init("debug");
			}
			
			CSyncReturnValue retval = new CSyncReturnValue();
		
			String accountName = args.getString(KEY_LOADER_ACCOUNTNAME);
			String sessionId   = args.getString(KEY_LOADER_SESSIONID);
			boolean approved   = args.getBoolean(KEY_LOADER_APPROVED);
			String authCode    = args.getString(KEY_LOADER_AUTHCODE);

			Log.d(TAG, String.format("Client Auth Response - account: %s, sessionid: %s, approved: %s, authcode: %s", accountName, sessionId, Boolean.toString(approved), authCode));

			try {
				ExfioPeerV1 auth = ReceivedClientAuth.getClientAuth(this.getContext(), accountName);
				
	    		if ( approved ) {
					auth.approveClientAuth(sessionId, authCode);
				} else {
	        		auth.rejectClientAuth(sessionId);				
				}

			} catch (Exception e) {
				retval.setException(e);
			}
			
			return retval;
		}
	}

	public static Account getAccount(Context context, String accountName) throws AccountNotFoundException {
		Account accountFound = null;
		
		AccountManager am = AccountManager.get(context);
		Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE_EXFIOPEER);

		for (Account account: accounts) {
			if ( account.name.equals(accountName) ) {
				accountFound = account;
				break;
			}
		}
		
		if (accountFound == null) {
			throw new AccountNotFoundException(String.format("Couldn't find account '%s' of type '%s'", accountName, Constants.ACCOUNT_TYPE_EXFIOPEER));
		}
		
		return accountFound;
	}
	
	public static ExfioPeerV1 getClientAuth(Context context, String accountName) throws WeaveException, AccountNotFoundException, InvalidAccountException {
		
		Account account = getAccount(context, accountName);
		ExfioPeerAccountSettings settings = new ExfioPeerAccountSettings(context, account);
		
		//DEBUG only
		if ( SystemUtils.isDebuggable(context) ) {
			org.exfio.csyncdroid.util.Log.init("debug");
		}

		Log.d(TAG, String.format("Weave credentials - username: %s, password: %s, synckey: %s", settings.getUserName(), settings.getPassword(), settings.getSyncKey()));
		
		//get weave account params
		LegacyV5AccountParams  params = new LegacyV5AccountParams();
		params.accountServer = settings.getBaseURL();
		params.user          = settings.getUserName();
		params.password      = settings.getPassword();
		params.syncKey       = settings.getSyncKey();
		
		//Initialise weave client from account params
		WeaveClient weaveClient = null;
		
		try {
			weaveClient = WeaveClientFactory.getInstance(params);
		} catch (WeaveException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		ExfioPeerV1 auth = null;
		
		try {	
			//Initialise sqldroid jdbc provider
			Class.forName("org.sqldroid.SQLDroidDriver");
            String databasePath = context.getDatabasePath(settings.getGuid()).getAbsolutePath();

			Log.d(TAG, String.format("Database path: %s", databasePath));

			auth = new ExfioPeerV1(weaveClient, databasePath);

			Log.d(TAG, String.format("Client auth before - authcode: %s, status: %s, auth by: %s", auth.getAuthCode(), auth.getAuthStatus(), auth.getAuthBy()));

		} catch (ClassNotFoundException e) {
			throw new WeaveException("Couldn't load SQL driver", e);
		}
		
		return auth;
	}

}
