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
import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.exfio.weave.WeaveException;
import org.exfio.csyncdroid.R;
import org.exfio.csyncdroid.resource.LocalCalendar;
import org.exfio.csyncdroid.util.Log;

import java.util.Properties;

public class AccountDetailsFragment extends Fragment implements TextWatcher {
	private static final String TAG = "exfio.AccountDetails";

	ServerInfo serverInfo;
	
	EditText editAccountName;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		serverInfo = (ServerInfo)getArguments().getSerializable(AccountSettings.KEY_SERVER_INFO);

		View v = inflater.inflate(R.layout.account_details, container, false);
		
		editAccountName = (EditText)v.findViewById(R.id.account_name);
		editAccountName.setText(serverInfo.getAccountName());
		editAccountName.addTextChangedListener(this);

		setHasOptionsMenu(true);
		return v;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.account_details, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_account:
			addAccount();
			break;
		default:
			return false;
		}
		return true;
	}


	// actions
	
	void addAccount() {
		Log.d(TAG, "addAccount()");

		try {
			String accountName = editAccountName.getText().toString();
			String accountType = serverInfo.getAccountType();
			Properties propSettings = serverInfo.getAccountParamsAsProperties();

			Log.d(TAG, "accountType: " + accountType);

			AccountSettings settings = null;

			if (accountType.equals(org.exfio.csyncdroid.Constants.ACCOUNT_TYPE_CSYNC)) {
				settings = new CSyncAccountSettings();
			} else if (accountType.equals(org.exfio.csyncdroid.Constants.ACCOUNT_TYPE_FXACCOUNT)) {
				settings = new FxAccountAccountSettings();
			} else if (accountType.equals(org.exfio.csyncdroid.Constants.ACCOUNT_TYPE_LEGACYV5)) {
				settings = new LegacyV5AccountSettings();
			} else if (accountType.equals(org.exfio.csyncdroid.Constants.ACCOUNT_TYPE_EXFIOPEER)) {
				settings = new ExfioPeerAccountSettings();

				//FIXME - Complete migration of this code from QueryServerDialogFrament

				/*
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
				*/
			} else {
				Toast.makeText(getActivity(), String.format("Couldn't create account. Account type '%s' not recognised", accountType), Toast.LENGTH_LONG).show();
				return;
			}

			Account account = null;
			try {
				account = settings.createAccount(getActivity(), accountName, propSettings);
			} catch (WeaveException e) {
				Toast.makeText(getActivity(), "Couldn't create account (account with this name already existing?)", Toast.LENGTH_LONG).show();
				return;
			}

			//Register addressbook
			if (serverInfo.getAddressBook().isEnabled()) {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			} else {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
			}

			//TODO - support multiple calendars

			//Register calendar
			if (serverInfo.getCalendar().isEnabled()) {
				LocalCalendar.create(account, getActivity().getContentResolver(), serverInfo.getCalendar());
				ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
			} else {
				ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 0);
			}

			getActivity().finish();

		} catch (Exception e) {
			Log.getInstance().error(String.format("Error creating account - %s", e.getMessage()));
			Toast.makeText(getActivity(), "Couldn't create account", Toast.LENGTH_LONG).show();
		}
	}

	
	// input validation
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ok = false;
		ok = editAccountName.getText().length() > 0;
		MenuItem item = menu.findItem(R.id.add_account);
		item.setEnabled(ok);
	}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		getActivity().invalidateOptionsMenu();
	}

	public void afterTextChanged(Editable s) {
	}
}
