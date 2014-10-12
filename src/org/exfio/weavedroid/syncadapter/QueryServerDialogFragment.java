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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.sql.SQLException;

import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.bluetooth.BluetoothAdapter;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.exfio.weave.client.AccountParams;
import org.exfio.weave.InvalidStorageException;
import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClientCLI;
import org.exfio.weave.client.WeaveClientFactory;
import org.exfio.weave.client.WeaveClientV5Params;
import org.exfio.weave.client.WeaveCollectionInfo;
import org.exfio.weave.ext.clientauth.ClientAuth;
import org.exfio.weave.ext.comm.Comms;
import org.exfio.weave.util.Base64;
import org.exfio.weave.util.OSUtils;
import org.exfio.weavedroid.Constants;
import org.exfio.weavedroid.syncadapter.AccountDetailsFragment;
import org.exfio.weavedroid.R;

public class QueryServerDialogFragment extends DialogFragment implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "exfio.QueryServerDialogFragment";

	public static final String
		EXTRA_BASE_URL        = "base_uri",
		EXTRA_USER_NAME       = "user_name",
		EXTRA_PASSWORD        = "password",
		EXTRA_SYNC_KEY        = "sync_key",
		EXTRA_AUTH_PREEMPTIVE = "auth_preemptive";
	
	ProgressBar progressBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate()");
		
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		setCancelable(false);

		Loader<ServerInfo> loader = getLoaderManager().initLoader(0, getArguments(), this);
		if (savedInstanceState == null)		// http://code.google.com/p/android/issues/detail?id=14944
			loader.forceLoad();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.query_server, container, false);
		return v;
	}

	@Override
	public Loader<ServerInfo> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader()");
		return new ServerInfoLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<ServerInfo> loader, ServerInfo serverInfo) {
		Log.d(TAG, "onLoadFinished()");
		
		if (serverInfo.getErrorMessage() != null) {
			Toast.makeText(getActivity(), serverInfo.getErrorMessage(), Toast.LENGTH_LONG).show();
		} else {
			
			// pass to "account details" fragment
			AccountDetailsFragment accountDetails = new AccountDetailsFragment();
			Bundle arguments = new Bundle();
			arguments.putSerializable(AccountSettings.KEY_SERVER_INFO, serverInfo);
			accountDetails.setArguments(arguments);
			
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, accountDetails)
				.addToBackStack(null)
				.commitAllowingStateLoss();
		}

		getDialog().dismiss();
	}

	@Override
	public void onLoaderReset(Loader<ServerInfo> loader) {
		Log.d(TAG, "onLoaderReset()");
	}
	
	
	static class ServerInfoLoader extends AsyncTaskLoader<ServerInfo> {
		private static final String TAG = "exfio.ServerInfoLoader";
		Bundle args;
		
		public ServerInfoLoader(Context context, Bundle args) {
			super(context);
			this.args = args;
		}

		@Override
		public ServerInfo loadInBackground() {
			Log.d(TAG, "LoadInBackgroud()");
			
			//DEBUG only
			org.exfio.weavedroid.util.Log.init("debug");
			org.exfio.weavedroid.util.Log.getInstance().info("Initialised logger");
			org.exfio.weavedroid.util.Log.getInstance().debug("Debug message");
			
			//Build unique account guid that is also valid filename
			String guid = null;
			try {
				guid = Comms.generateAccountGuid(args.getString(EXTRA_BASE_URL), args.getString(EXTRA_USER_NAME));
			} catch (WeaveException e) {
				//Fail quietly - shouldn't happen
			}
			
			ServerInfo serverInfo = new ServerInfo(
				guid,
				args.getString(EXTRA_BASE_URL),
				args.getString(EXTRA_USER_NAME),
				args.getString(EXTRA_PASSWORD),
				args.getString(EXTRA_SYNC_KEY),
				args.getBoolean(EXTRA_AUTH_PREEMPTIVE)
			);
			
			try {
				// Query weave sync server
				
				// (1/4) Autodiscover weave sync storage version				
				AccountParams adParams = new AccountParams();
				adParams.baseURL  = serverInfo.getBaseURL();
				adParams.user     = serverInfo.getUser();
				adParams.password = serverInfo.getPassword();
				
				WeaveClientFactory.StorageVersion storageVersion = null;
				try {
					storageVersion = WeaveClientFactory.autoDiscoverStorageVersion(adParams);
				} catch (InvalidStorageException e) {
					//Storage not initialised use default
					storageVersion = WeaveClientFactory.getDefaultStorageVersion();
				}

				// (2/4) Initialise weave client
				AccountParams weaveParams = null;
				
				if ( storageVersion == WeaveClientFactory.StorageVersion.v5 ){
					//Only v5 is currently supported
					WeaveClientV5Params v5Params = new WeaveClientV5Params();
					v5Params.baseURL  = serverInfo.getBaseURL();
					v5Params.user     = serverInfo.getUser();
					v5Params.password = serverInfo.getPassword();
					v5Params.syncKey  = serverInfo.getSyncKey();
					weaveParams = v5Params;
				} else {
					throw new WeaveException(String.format("Storage version '%s' not supported", WeaveClientFactory.storageVersionToString(storageVersion))); 
				}
				
				WeaveClient weaveClient = WeaveClientFactory.getInstance(storageVersion);
				weaveClient.init(weaveParams);

				// (3/4) Initialise weave meta data
				if ( !weaveClient.isInitialised() ) {
					weaveClient.initServer();
					
					//Save synckey
					if ( weaveClient.getStorageVersion() == WeaveClientFactory.StorageVersion.v5 ){
						//Only v5 is currently supported
						
						serverInfo = new ServerInfo(
							serverInfo.getGuid(),
							serverInfo.getBaseURL(),
							serverInfo.getUser(),
							serverInfo.getPassword(),
							((WeaveClientV5Params)weaveClient.getClientParams()).syncKey,
							serverInfo.isAuthPreemptive()
						);
					} else {
						throw new WeaveException(String.format("Storage version '%s' not supported", weaveClient.getStorageVersion())); 
					}
				}
				
				// (4/5) Initialise eXfio meta data OR request authorisation for existing client

				//Initialise sqldroid jdbc provider
				Class.forName("org.sqldroid.SQLDroidDriver");
				
				String clientName = OSUtils.getPrettyName();
				
				//Create database file if it does not already exist
            	android.database.sqlite.SQLiteDatabase database = null;
                try {
                	database = getContext().openOrCreateDatabase(serverInfo.getGuid(), Context.MODE_PRIVATE, null);
                } catch(Exception e) {
                	Log.e(TAG, e.getMessage());
                } finally {
                	if ( database != null ) {
                		database.close();
                	}
                }
                String databasePath = getContext().getDatabasePath(serverInfo.getGuid()).getAbsolutePath();
                
				ClientAuth auth = new ClientAuth(weaveClient);

				if ( !auth.isInitialised() ) {
					
					Log.i(TAG, "Initialising eXfio meta data");
					auth.initClientAuth(clientName, databasePath);
				} else {
				
					Log.i(TAG, String.format("Requesting client auth for client '%s'", clientName));
					
					//FIXME - DEBUG only
					auth.setPbkdf2Iterations(1);
					auth.setPbkdf2Length(128);
					
					auth.requestClientAuth(clientName, serverInfo.getPassword(), databasePath);
					String authCode = auth.getAuthCode();
					
					Log.i(TAG, String.format("Client auth request pending with auth code '%s'", authCode));
				}
				
				// (5/5) Initialise exfiocontacts collection
				ServerInfo.ResourceInfo addressbook = new ServerInfo.ResourceInfo(
					Constants.ResourceType.ADDRESS_BOOK,
					false,
					Constants.ADDRESSBOOK_COLLECTION,
					"WeaveDroid Contacts",
					"WeaveDroid Contacts",
					null
				);
				addressbook.setEnabled(true);
				
				serverInfo.setAddressBook(addressbook);	
								
			} catch (Exception e) {
				Log.e(TAG, "Error while querying server info", e);
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_exfio, e.getLocalizedMessage()));
			}
			
			return serverInfo;
		}
	}
}
