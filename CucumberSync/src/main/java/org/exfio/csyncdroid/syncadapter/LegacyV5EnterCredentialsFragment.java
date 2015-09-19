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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.exfio.weave.util.URIUtils;
import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.R;

public class LegacyV5EnterCredentialsFragment extends Fragment implements TextWatcher {
	String protocol;
	
	TextView textHttpWarning, labelSyncKey;
	EditText editBaseURL, editUserName, editPassword, editSyncKey;
	Spinner spnrProtocol;
	Button btnNext;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials_legacyv5, container, false);
		
		// protocol selection spinner
		textHttpWarning = (TextView) v.findViewById(R.id.http_warning);
		
		spnrProtocol = (Spinner) v.findViewById(R.id.legacyv5_account_server_protocol);
		spnrProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				protocol = parent.getAdapter().getItem(position).toString();
				textHttpWarning.setVisibility(protocol.equals("https://") ? View.GONE : View.VISIBLE);
			}

			public void onNothingSelected(AdapterView<?> parent) {
				protocol = null;
			}
		});
		
		spnrProtocol.setSelection(1);	// HTTPS

		// other input fields
		editBaseURL = (EditText) v.findViewById(R.id.legacyv5_account_server_url);
		editBaseURL.addTextChangedListener(this);
		
		editUserName = (EditText) v.findViewById(R.id.userName);
		editUserName.addTextChangedListener(this);
		
		editPassword = (EditText) v.findViewById(R.id.password);
		editPassword.addTextChangedListener(this);
		
		labelSyncKey = (TextView) v.findViewById(R.id.synckeylabel);
		editSyncKey = (EditText) v.findViewById(R.id.synckey);
		editSyncKey.addTextChangedListener(this);
		
		// hook into action bar
		setHasOptionsMenu(true);

		//ownCloud environment
		spnrProtocol.setSelection(1);
		editBaseURL.setText("cucumbersync.com/cloud/remote.php/mozilla_sync/");
        
		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.enter_credentials, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.next:
			queryServer();
			break;
		default:
			return false;
		}
		return true;
	}

	void queryServer() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		Bundle args = new Bundle();
		
		args.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE_LEGACYV5);
		args.putString(LegacyV5AccountSettings.KEY_BASE_URL, URIUtils.sanitize(protocol + editBaseURL.getText().toString()));
		args.putString(LegacyV5AccountSettings.KEY_USERNAME, editUserName.getText().toString());
		args.putString(LegacyV5AccountSettings.KEY_PASSWORD , editPassword.getText().toString());
		args.putString(LegacyV5AccountSettings.KEY_SYNCKEY, editSyncKey.getText().toString());
		args.putBoolean(LegacyV5AccountSettings.KEY_AUTH_PREEMPTIVE, true);

		
		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(args);
	    dialog.show(ft, QueryServerDialogFragment.class.getName());
	}

	
	// input validation
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ok =
			editUserName.getText().length() > 0 &&
			editPassword.getText().length() > 0;

		// check host name
		try {
			URI uri = new URI(URIUtils.sanitize(protocol + editBaseURL.getText().toString()));
			if (StringUtils.isBlank(uri.getHost()))
				ok = false;
		} catch (URISyntaxException e) {
			ok = false;
		}
			
		MenuItem item = menu.findItem(R.id.next);
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
