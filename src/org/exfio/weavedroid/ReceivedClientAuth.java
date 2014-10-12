package org.exfio.weavedroid;

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

import org.exfio.weave.InvalidStorageException;
import org.exfio.weave.WeaveException;
import org.exfio.weave.client.AccountParams;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClientFactory;
import org.exfio.weave.client.WeaveClientV5Params;
import org.exfio.weave.ext.clientauth.AuthcodeVerificationFailedException;
import org.exfio.weave.ext.clientauth.ClientAuth;
import org.exfio.weavedroid.syncadapter.AccountSettings;
import org.exfio.weavedroid.util.SystemUtils;

public class ReceivedClientAuth extends Activity implements LoaderCallbacks<WeaveDroidReturnValue> {
	
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
		
		final String TAG = "weavedroid.ReceivedClientAuth";
		
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
		t.setText(String.format("'%s' is requesting authentication for WeaveDroid account '%s'", clientName, accountName));

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

	@Override
	public Loader<WeaveDroidReturnValue> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader()");
		return new ClientAuthResponseLoader(this, args);
	}

	@Override
	public void onLoadFinished(Loader<WeaveDroidReturnValue> loader, WeaveDroidReturnValue retval) {
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

	@Override
	public void onLoaderReset(Loader<WeaveDroidReturnValue> loader) {
		Log.d(TAG, "onLoaderReset()");
	}

	static class ClientAuthResponseLoader extends AsyncTaskLoader<WeaveDroidReturnValue> {
		private static final String TAG = "exfio.ClientAuthResponseLoader";
		Bundle args;
		
		public ClientAuthResponseLoader(Context context, Bundle args) {
			super(context);
			this.args = args;
		}

		@Override
		public WeaveDroidReturnValue loadInBackground() {
			Log.d(TAG, "LoadInBackgroud()");
			
			//DEBUG only
			if ( SystemUtils.isDebuggable(getContext()) ) {
				org.exfio.weavedroid.util.Log.init("debug");
			}
			
			WeaveDroidReturnValue retval = new WeaveDroidReturnValue();
		
			String accountName = args.getString(KEY_LOADER_ACCOUNTNAME);
			String sessionId   = args.getString(KEY_LOADER_SESSIONID);
			boolean approved   = args.getBoolean(KEY_LOADER_APPROVED);
			String authCode    = args.getString(KEY_LOADER_AUTHCODE);

			Log.d(TAG, String.format("Client Auth Response - account: %s, sessionid: %s, approved: %s, authcode: %s", accountName, sessionId, Boolean.toString(approved), authCode));

			try {
				ClientAuth auth = ReceivedClientAuth.getClientAuth(this.getContext(), accountName);
				
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
		Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);

		for (Account account: accounts) {
			if ( account.name.equals(accountName) ) {
				accountFound = account;
				break;
			}
		}
		
		if (accountFound == null) {
			throw new AccountNotFoundException(String.format("Couldn't find account '%s' of type '%s'", accountName, Constants.ACCOUNT_TYPE));
		}
		
		return accountFound;
	}
	
	public static ClientAuth getClientAuth(Context context, String accountName) throws WeaveException, AccountNotFoundException, InvalidAccountException {
		
		Account account = getAccount(context, accountName);
		AccountSettings settings = new AccountSettings(context, account);
		
		//DEBUG only
		if ( SystemUtils.isDebuggable(context) ) {
			org.exfio.weavedroid.util.Log.init("debug");
		}

		Log.d(TAG, String.format("Weave credentials - username: %s, password: %s, synckey: %s", settings.getUserName(), settings.getPassword(), settings.getSyncKey()));
		
		//get weave account params		
		AccountParams params = new AccountParams();
		params.baseURL  = settings.getBaseURL();
		params.user     = settings.getUserName();
		params.password = settings.getPassword();
		
		WeaveClientFactory.StorageVersion storageVersion = null;
		try {
			storageVersion = WeaveClientFactory.autoDiscoverStorageVersion(params);
		} catch (InvalidStorageException e) {
			//Storage not initialised use default
			storageVersion = WeaveClientFactory.getDefaultStorageVersion();
		}

		// (2/4) Initialise weave client
		AccountParams weaveParams = null;
		
		if ( storageVersion == WeaveClientFactory.StorageVersion.v5 ){
			//Only v5 is currently supported
			WeaveClientV5Params v5Params = new WeaveClientV5Params();
			v5Params.baseURL  = settings.getBaseURL();
			v5Params.user     = settings.getUserName();
			v5Params.password = settings.getPassword();
			v5Params.syncKey  = settings.getSyncKey();
			weaveParams = v5Params;
		} else {
			throw new WeaveException(String.format("Storage version '%s' not supported", WeaveClientFactory.storageVersionToString(storageVersion))); 
		}
		
		WeaveClient weaveClient = WeaveClientFactory.getInstance(storageVersion);
		weaveClient.init(weaveParams);
		
		
		ClientAuth auth = null;
		
		try {	
			//Initialise sqldroid jdbc provider
			Class.forName("org.sqldroid.SQLDroidDriver");
            String databasePath = context.getDatabasePath(settings.getGuid()).getAbsolutePath();

			Log.d(TAG, String.format("Database path: %s", databasePath));

			auth = new ClientAuth(weaveClient, databasePath);

			Log.d(TAG, String.format("Client auth before - authcode: %s, status: %s, auth by: %s", auth.getAuthCode(), auth.getAuthStatus(), auth.getAuthBy()));

		} catch (ClassNotFoundException e) {
			throw new WeaveException("Couldn't load SQL driver", e);
		}
		
		//FIXME - DEBUG only
		auth.setPbkdf2Iterations(1000);

		return auth;
	}

}
