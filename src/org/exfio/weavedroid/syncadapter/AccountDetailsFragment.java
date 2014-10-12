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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
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

import org.exfio.weavedroid.R;
import org.exfio.weavedroid.util.Log;

public class AccountDetailsFragment extends Fragment implements TextWatcher {
	
	ServerInfo serverInfo;
	
	EditText editAccountName;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);
		
		editAccountName = (EditText)v.findViewById(R.id.account_name);
		editAccountName.addTextChangedListener(this);

		//DEBUG only
		editAccountName.setText("Gerry Test");
		
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
		ServerInfo serverInfo = (ServerInfo)getArguments().getSerializable(AccountSettings.KEY_SERVER_INFO);
		try {
			String accountName = editAccountName.getText().toString();
			
			AccountManager accountManager = AccountManager.get(getActivity());
			Account account = new Account(accountName, org.exfio.weavedroid.Constants.ACCOUNT_TYPE);
			Bundle userData = AccountSettings.createBundle(serverInfo);
			
			if (serverInfo.getAddressBook().isEnabled()) {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			} else {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
			}

			//FIXME - make password/synckey concatenation more robust, i.e. encode as JSON
			if (accountManager.addAccountExplicitly(account, AccountSettings.encodePassword(serverInfo.getPassword(), serverInfo.getSyncKey()), userData)) {
				getActivity().finish();
			} else {
				Toast.makeText(getActivity(), "Couldn't create account (account with this name already existing?)", Toast.LENGTH_LONG).show();
			}
			
		} catch (Exception e) {
			Log.getInstance().error(String.format("Error creating account - %s", e.getMessage()));
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

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void afterTextChanged(Editable s) {
	}
}
