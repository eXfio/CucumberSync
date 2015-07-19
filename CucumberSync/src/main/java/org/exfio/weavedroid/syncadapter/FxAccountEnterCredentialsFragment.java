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

import org.exfio.weave.client.WeaveClientV1_5;
import org.exfio.weave.util.Log;
import org.exfio.weave.util.URIUtils;
import org.exfio.weavedroid.Constants;
import org.exfio.weavedroid.R;

public class FxAccountEnterCredentialsFragment extends Fragment implements TextWatcher {
	String accountServerProtocol = "";
	String tokenServerProtocol   = "";
	
	TextView textHttpWarning, labelSyncKey;
	EditText editAccountServerUrl, editTokenServerUrl, editUserName, editPassword;
	Spinner spnrAccountServerProtocol, spnrTokenServerProtocol;
	Button btnNext;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials_fxaccount, container, false);
		
		// Setup protocol selection spinners
		textHttpWarning = (TextView) v.findViewById(R.id.http_warning);
		
		spnrAccountServerProtocol = (Spinner) v.findViewById(R.id.fxaccount_account_server_protocol);
		spnrAccountServerProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				accountServerProtocol = parent.getAdapter().getItem(position).toString();
				if (
						accountServerProtocol.equals("https://")
								&&
								tokenServerProtocol.equals("https://")
						) {
					textHttpWarning.setVisibility(View.GONE);
				} else {
					textHttpWarning.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				accountServerProtocol = null;
			}
		});

		spnrAccountServerProtocol.setSelection(1);	// HTTPS

		spnrTokenServerProtocol = (Spinner) v.findViewById(R.id.fxaccount_token_server_protocol);
		spnrTokenServerProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				tokenServerProtocol = parent.getAdapter().getItem(position).toString();
				if (
						accountServerProtocol.equals("https://")
								&&
								tokenServerProtocol.equals("https://")
						) {
					textHttpWarning.setVisibility(View.GONE);
				} else {
					textHttpWarning.setVisibility(View.VISIBLE);
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				tokenServerProtocol = null;
			}
		});

		spnrTokenServerProtocol.setSelection(1);	// HTTPS

		// other input fields
		editAccountServerUrl = (EditText) v.findViewById(R.id.fxaccount_account_server_url);
		editAccountServerUrl.addTextChangedListener(this);
		
		editTokenServerUrl = (EditText) v.findViewById(R.id.fxaccount_token_server_url);
		editTokenServerUrl.addTextChangedListener(this);
		
		editUserName = (EditText) v.findViewById(R.id.userName);
		editUserName.addTextChangedListener(this);
		
		editPassword = (EditText) v.findViewById(R.id.password);
		editPassword.addTextChangedListener(this);
		
		// hook into action bar
		setHasOptionsMenu(true);


		//Defaults
		try {
			URI uriAccount = new URI(WeaveClientV1_5.DEFAULT_ACCOUNT_SERVER);
			URI uriToken = new URI(WeaveClientV1_5.DEFAULT_TOKEN_SERVER);
			String portAccount = "";
			String portToken   = "";

			//FIXME - Debug only
			//Dev environment
			uriToken = new URI("http://argent.local:5000/token/1.0/sync/1.5");
			editUserName.setText("exfiotest1@gmail.com");

			if ( uriAccount.getScheme().equalsIgnoreCase("https") ) {
				spnrAccountServerProtocol.setSelection(1);
				if ( uriAccount.getPort() > 0 && uriAccount.getPort() != 443 ) {
					portAccount = ":" + uriAccount.getPort();
				}
			} else {
				spnrAccountServerProtocol.setSelection(0);
				if ( uriAccount.getPort() > 0 && uriAccount.getPort() != 80 ) {
					portAccount = ":" + uriAccount.getPort();
				}
			}
			editAccountServerUrl.setText(uriAccount.getHost() + portAccount + uriAccount.getPath());

			if ( uriToken.getScheme().equalsIgnoreCase("https") ) {
				spnrTokenServerProtocol.setSelection(1);
				if ( uriToken.getPort() > 0 && uriToken.getPort() != 443 ) {
					portToken = ":" + uriToken.getPort();
				}
			} else {
				spnrTokenServerProtocol.setSelection(0);
				if ( uriToken.getPort() > 0 && uriToken.getPort() != 80 ) {
					portToken = ":" + uriToken.getPort();
				}
			}
			editTokenServerUrl.setText(uriToken.getHost() + portToken + uriToken.getPath());

		} catch(URISyntaxException e) {
			//Fail quietly
			Log.getInstance().error("Couldn't initialise default values - " + e.getMessage());
		}

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
		
		args.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE_FXACCOUNT);
		args.putString(FxAccountAccountSettings.KEY_ACCOUNT_SERVER, URIUtils.sanitize(accountServerProtocol + editAccountServerUrl.getText().toString()));
		args.putString(FxAccountAccountSettings.KEY_TOKEN_SERVER, URIUtils.sanitize(tokenServerProtocol + editTokenServerUrl.getText().toString()));
		args.putString(FxAccountAccountSettings.KEY_USERNAME, editUserName.getText().toString());
		args.putString(FxAccountAccountSettings.KEY_PASSWORD, editPassword.getText().toString());
		
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
			URI uri = new URI(URIUtils.sanitize(accountServerProtocol + editAccountServerUrl.getText().toString()));
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
