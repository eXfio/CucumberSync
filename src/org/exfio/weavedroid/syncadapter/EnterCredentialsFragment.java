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
import org.exfio.weavedroid.R;

public class EnterCredentialsFragment extends Fragment implements TextWatcher {
	String protocol;
	
	TextView textHttpWarning;
	EditText editBaseURL, editUserName, editPassword, editSyncKey;
	Spinner spnrProtocol;
	Button btnNext;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials, container, false);
		
		// protocol selection spinner
		textHttpWarning = (TextView) v.findViewById(R.id.http_warning);
		
		spnrProtocol = (Spinner) v.findViewById(R.id.select_protocol);
		spnrProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				protocol = parent.getAdapter().getItem(position).toString();
				textHttpWarning.setVisibility(protocol.equals("https://") ? View.GONE : View.VISIBLE);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				protocol = null;
			}
		});
		
		spnrProtocol.setSelection(1);	// HTTPS

		// other input fields
		editBaseURL = (EditText) v.findViewById(R.id.baseURL);
		editBaseURL.addTextChangedListener(this);
		
		editUserName = (EditText) v.findViewById(R.id.userName);
		editUserName.addTextChangedListener(this);
		
		editPassword = (EditText) v.findViewById(R.id.password);
		editPassword.addTextChangedListener(this);
		
		editSyncKey = (EditText) v.findViewById(R.id.synckey);
		editSyncKey.addTextChangedListener(this);
		
		// hook into action bar
		setHasOptionsMenu(true);

		//DEBUG For testing only
		spnrProtocol.setSelection(0);
        
		//Dev environment
		//editBaseURL.setText("argent.local:8081");
		//editUserName.setText("gerry");
		//editPassword.setText("test1234");
		//editSyncKey.setText("7-ch6pu-6teey-36ndf-gwsex-93uza");
        
		//ownCloud environment
		editBaseURL.setText("cloud.kaleido.com.au/remote.php/mozilla_sync/");
		editUserName.setText("gerry");
		editPassword.setText("test1234");
		//editSyncKey.setText("E7NEP9G26C7EF33KUFDZENHW94");
		
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
		
		String host_path = editBaseURL.getText().toString();
		args.putString(QueryServerDialogFragment.EXTRA_BASE_URL, URIUtils.sanitize(protocol + host_path));
		args.putString(QueryServerDialogFragment.EXTRA_USER_NAME, editUserName.getText().toString());
		args.putString(QueryServerDialogFragment.EXTRA_PASSWORD, editPassword.getText().toString());
		args.putString(QueryServerDialogFragment.EXTRA_SYNC_KEY, editSyncKey.getText().toString());
		args.putBoolean(QueryServerDialogFragment.EXTRA_AUTH_PREEMPTIVE, true);
		
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
