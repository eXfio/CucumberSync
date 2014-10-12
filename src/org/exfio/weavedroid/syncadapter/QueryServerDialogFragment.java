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

import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
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

import org.exfio.weave.WeaveException;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClient.StorageVersion;
import org.exfio.weave.client.WeaveClientParams;
import org.exfio.weave.client.WeaveAutoDiscoverParams;
import org.exfio.weave.client.WeaveStorageV5Params;
import org.exfio.weave.client.WeaveCollectionInfo;

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
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		setCancelable(false);

		Loader<ServerInfo> loader = getLoaderManager().initLoader(0, getArguments(), this);
		if (savedInstanceState == null)		// http://code.google.com/p/android/issues/detail?id=14944
			loader.forceLoad();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
	public void onLoaderReset(Loader<ServerInfo> arg0) {
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
			org.exfio.weavedroid.Log.init("debug");
			org.exfio.weavedroid.Log.getInstance().info("Initialised logger");
			org.exfio.weavedroid.Log.getInstance().debug("Debug message");
			
			ServerInfo serverInfo = new ServerInfo(
				args.getString(EXTRA_BASE_URL),
				args.getString(EXTRA_USER_NAME),
				args.getString(EXTRA_PASSWORD),
				args.getString(EXTRA_SYNC_KEY),
				args.getBoolean(EXTRA_AUTH_PREEMPTIVE)
			);
			
			try {
				// Query weave sync server
				
				// (1/3) Autodiscover weave sync storage version				
				WeaveClient weaveClient = null;
				WeaveAutoDiscoverParams adParams = new WeaveAutoDiscoverParams();
				adParams.baseURL  = serverInfo.getBaseURL();
				adParams.user     = serverInfo.getUser();
				adParams.password = serverInfo.getPassword();
				
				weaveClient = WeaveClient.getInstance(adParams);

				// (2/3) Initialise weave client
				WeaveClientParams weaveParams = null;
				
				if ( weaveClient.getStorageVersion() == StorageVersion.v5 ){
					//Only v5 is currently supported
					WeaveStorageV5Params v5Params = new WeaveStorageV5Params();
					v5Params.baseURL  = serverInfo.getBaseURL();
					v5Params.user     = serverInfo.getUser();
					v5Params.password = serverInfo.getPassword();
					v5Params.syncKey  = serverInfo.getSyncKey();
					weaveParams = v5Params;
				} else {
					throw new WeaveException(String.format("Storage version '%s' not supported", weaveClient.getStorageVersion())); 
				}
				
				weaveClient.init(weaveParams);	

				// (3/3) check for exfiocontacts collection
				try {
					WeaveCollectionInfo colinfo = weaveClient.getCollectionInfo(Constants.ADDRESSBOOK_COLLECTION);
					
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
					
				} catch (WeaveException e) {
					//FIXME create new collection?
					throw new WeaveException(e);
				}
								
			} catch (Exception e) {
				Log.e(TAG, "Error while querying server info", e);
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_exfio, e.getLocalizedMessage()));
			}
			
			return serverInfo;
		}
	}
}
