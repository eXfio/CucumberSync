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


import java.io.IOException;
import java.net.URI;

import android.app.DialogFragment;
import android.app.LoaderManager;
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
import org.exfio.weave.account.WeaveAccount;
import org.exfio.weave.account.exfiopeer.ExfioPeerV1;
import org.exfio.weave.account.fxa.FxAccount;
import org.exfio.weave.account.fxa.FxAccountParams;
import org.exfio.weave.account.legacy.LegacyV5AccountParams;
import org.exfio.weave.client.WeaveClient;
import org.exfio.weave.client.WeaveClientFactory;
import org.exfio.weave.util.OSUtils;
import org.exfio.weavedroid.Constants;
import org.exfio.weavedroid.util.SystemUtils;
import org.exfio.weavedroid.R;

public class QueryServerDialogFragment extends DialogFragment implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "exfio.QueryServerDialog";

	ProgressBar progressBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate()");
		
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		setCancelable(false);

		LoaderManager.enableDebugLogging(true);

		// https://groups.google.com/forum/#!topic/android-developers/DbKL6PVyhLI
		//Loader<ServerInfo> loader = getLoaderManager().initLoader(0, getArguments(), this);

		Loader<ServerInfo> loader = getLoaderManager().getLoader(0);
		if ( loader != null && loader.isReset() ) {
			loader = getLoaderManager().restartLoader(0, getArguments(), this);
		} else {
			loader = getLoaderManager().initLoader(0, getArguments(), this);
		}

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
	public void onStart() {
		Log.d(TAG, "onStart()");
		super.onStart();

		Loader<ServerInfo> loader = getLoaderManager().getLoader(0);
		if ( loader == null || loader.isReset() || loader.isAbandoned() ) {
			Log.d(TAG, "Couldn't reconnect to loader");
			Toast.makeText(getActivity(), "Couldn't reconnect to loader", Toast.LENGTH_LONG).show();
			getDialog().dismiss();
		}
	}

	public Loader<ServerInfo> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "onCreateLoader()");
		return new ServerInfoLoader(getActivity(), args);
	}

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

	public void onLoaderReset(Loader<ServerInfo> loader) {
		Log.d(TAG, "onLoaderReset()");
	}
	
	
	static class ServerInfoLoader extends AsyncTaskLoader<ServerInfo> {
		private static final String TAG = "exfio.ServerInfoLoader";
		Bundle args;

		private ServerInfo serverInfo;

		public ServerInfoLoader(Context context, Bundle args) {
			super(context);
			this.args = args;
			serverInfo = null;
		}

		@Override
		protected void onStartLoading() {
			Log.d(TAG, "onStartLoading()");

			// deliverResult() not automatically triggered after screen lock or returning from other activity
			// http://stackoverflow.com/questions/7474756/onloadfinished-not-called-after-coming-back-from-a-home-button-press
			if (serverInfo != null) {
				deliverResult(serverInfo);
			}
		}

		@Override
		public ServerInfo loadInBackground() {
			Log.d(TAG, "LoadInBackgroud()");
			
			//DEBUG only
			if ( SystemUtils.isDebuggable(getContext()) ) {
				org.exfio.weavedroid.util.Log.init("debug");
				//org.exfio.weave.util.Log.init("debug");
				//org.mozilla.gecko.background.common.log.Logger.init("debug");
				//org.mozilla.gecko.background.common.log.Logger.setLogLevel("exfio.fxaclient", "debug");
			}
			
			String accountType = args.getString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
			
			String guid = null;
			WeaveAccount account = null;
			String errorMessage = null;

			if ( accountType.equals(Constants.ACCOUNT_TYPE_FXACCOUNT) ) {
				//Initialise FxA account

				//TODO - Support account creation
				
				//Get account params
				String accountServer = args.getString(FxAccountAccountSettings.KEY_ACCOUNT_SERVER);
				String tokenServer   = args.getString(FxAccountAccountSettings.KEY_TOKEN_SERVER);
				String username      = args.getString(FxAccountAccountSettings.KEY_USERNAME);
				String password      = args.getString(FxAccountAccountSettings.KEY_PASSWORD);
									
				try {
				
					//Validate account params
					if (
						(accountServer == null || accountServer.isEmpty())
						||
						(tokenServer == null || tokenServer.isEmpty())
						||
						(username == null || username.isEmpty())
						||
						(password == null || password.isEmpty())
					) {
						throw new WeaveException("account-server, token-server, username and password are required parameters for account registration");
					}
		
					//Validate URI syntax
					try {
						URI.create(accountServer);
						URI.create(tokenServer);
					} catch (IllegalArgumentException e) {
						throw new WeaveException(String.format("'%s' is not a valid URI, i.e. should be http(s)://example.com\n", accountServer));
					}

					//Initialise account
					FxAccountParams fxaParams = new FxAccountParams();
					fxaParams.accountServer  = accountServer;
					fxaParams.tokenServer    = tokenServer;
					fxaParams.user           = username;
					fxaParams.password       = password;
					
					account = new FxAccount();
					account.init(fxaParams);
					
				} catch (Exception e) {
					Log.e(TAG, "Error while querying server info", e);
					errorMessage = getContext().getString(R.string.exception_weavedroid, e.getLocalizedMessage());
				}

			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_LEGACYV5) ) {
				//Initialise LegacyV5 account

				//TODO - Support account creation

				//Get account params
				String accountServer = args.getString(LegacyV5AccountSettings.KEY_BASE_URL);
				String username      = args.getString(LegacyV5AccountSettings.KEY_USERNAME);
				String password      = args.getString(LegacyV5AccountSettings.KEY_PASSWORD);
				String synckey       = args.getString(LegacyV5AccountSettings.KEY_SYNCKEY);				
									
				try {					
				
					//Validate account params
					if (
						(accountServer == null || accountServer.isEmpty())
						||
						(username == null || username.isEmpty())
						||
						(password == null || password.isEmpty())
						||
						(synckey == null || synckey.isEmpty())
					) {
						throw new WeaveException("account-server, username, password and synckey are required parameters for account registration");
					}
									
					LegacyV5AccountParams adParams = new LegacyV5AccountParams();
					adParams.accountServer = accountServer;
					adParams.user          = username;
					adParams.password      = password;				
					adParams.syncKey       = synckey;
									
					WeaveClient weaveClient = WeaveClientFactory.getInstance(adParams);

					// Check weave account is initialised
					if ( !weaveClient.isInitialised() ) {
						throw new WeaveException(String.format("Weave account '%s@%s' not initialised.", adParams.user, adParams.accountServer)); 
					}
					
				} catch (Exception e) {
					Log.e(TAG, "Error while querying server info", e);
					errorMessage = getContext().getString(R.string.exception_weavedroid, e.getLocalizedMessage());
				}

			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_EXFIOPEER) ) {
				//Initialise eXfio Peer account

				//TODO - Support account creation

				//Get account params
				String accountServer = args.getString(LegacyV5AccountSettings.KEY_BASE_URL);
				String username      = args.getString(LegacyV5AccountSettings.KEY_USERNAME);
				String password      = args.getString(LegacyV5AccountSettings.KEY_PASSWORD);
									
				try {					
				
					//Validate account params
					if (
						(accountServer == null || accountServer.isEmpty())
						||
						(username == null || username.isEmpty())
						||
						(password == null || password.isEmpty())
					) {
						throw new WeaveException("account-server, username and password are required parameters for account registration");
					}
									
					LegacyV5AccountParams adParams = new LegacyV5AccountParams();
					adParams.accountServer = accountServer;
					adParams.user          = username;
					adParams.password      = password;				
									
					WeaveClient weaveClient = WeaveClientFactory.getInstance(adParams);
					
					// Check underlying weave account is initialised
					if ( !weaveClient.isInitialised() ) {
						throw new WeaveException(String.format("Weave account '%s@%s' not initialised.", adParams.user, adParams.accountServer)); 
					}

					ExfioPeerV1 auth = new ExfioPeerV1(weaveClient);

					// Check exfio peer account is initialised
					if ( !auth.isInitialised() ) {
						throw new WeaveException(String.format("eXfio Peer account '%s@%s' not initialised.", adParams.user, adParams.accountServer)); 
					}

					//Initialise sqldroid jdbc provider
					Class.forName("org.sqldroid.SQLDroidDriver");
					
					String clientName = OSUtils.getPrettyName();
			
					//Build unique account guid that is also valid filename
					guid = WeaveAccount.generateAccountGuid(accountServer, username);
					
					//Create database file if it does not already exist
					
	            	android.database.sqlite.SQLiteDatabase database = null;
	                try {
	                	database = getContext().openOrCreateDatabase(guid, Context.MODE_PRIVATE, null);
	                } catch(Exception e) {
	                	Log.e(TAG, e.getMessage());
	                } finally {
	                	if ( database != null ) {
	                		database.close();
	                	}
	                }
	                String databasePath = getContext().getDatabasePath(guid).getAbsolutePath();
	                
					// Request authorisation from existing client
					Log.i(TAG, String.format("Requesting client auth for client '%s'", clientName));
						
					auth.requestClientAuth(clientName, password, databasePath);
					String authCode = auth.getAuthCode();
						
					Log.i(TAG, String.format("Client auth request pending with auth code '%s'", authCode));

				} catch (Exception e) {
					Log.e(TAG, "Error while querying server info", e);
					errorMessage = getContext().getString(R.string.exception_weavedroid, e.getLocalizedMessage());
				}
				
			} else {
				Log.e(TAG, String.format("Account type '%s' not supported", accountType));
				return null;
			}

			serverInfo = new ServerInfo();
			
			if ( errorMessage == null ) {
				
				try {
					
					serverInfo.setAccountType(accountType);
					serverInfo.setGuid(guid);
					serverInfo.setAccountParams(account.accountParamsToProperties());

					// Initialise exfiocontacts collection
					ServerInfo.ResourceInfo addressbook = new ServerInfo.ResourceInfo(
						Constants.ResourceType.ADDRESS_BOOK,
						false,
						Constants.ADDRESSBOOK_COLLECTION,
						"CucumberSync Contacts",
						"CucumberSync Contacts",
						null
					);
					addressbook.setEnabled(true);
					
					serverInfo.setAddressBook(addressbook);	

				} catch (IOException e) {
					serverInfo.setErrorMessage(e.getMessage());					
				}
				
			} else {
				serverInfo.setErrorMessage(errorMessage);
			}

			Log.d(TAG, "Returning from LoadInBackgroud()");

			return serverInfo;
		}
	}
}
