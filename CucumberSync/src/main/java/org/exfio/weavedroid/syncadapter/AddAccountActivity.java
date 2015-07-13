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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import org.exfio.weavedroid.Constants;
import org.exfio.weavedroid.R;

public class AddAccountActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account);
		
		Intent intent = getIntent();
		String accountType = intent.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
		
		if (savedInstanceState == null) {	// first call
			Fragment fragment = null;
			String fragmentTag = null;
			if ( accountType.equals(Constants.ACCOUNT_TYPE_LEGACYV5) ) {
				fragment = new LegacyV5EnterCredentialsFragment();
				fragmentTag = "enter_credentials_legacyv5";
			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_FXACCOUNT) ) {
				fragment = new FxAccountEnterCredentialsFragment();
				fragmentTag = "enter_credentials_fxaccount";
			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_EXFIOPEER) ) {
				fragment = new ExfioPeerEnterCredentialsFragment();
				fragmentTag = "enter_credentials_exfiopeer";
			} else {
				throw new AssertionError(String.format("Account type '%s' not supported", accountType));
			}
			
			getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, fragment, fragmentTag)
				.commit();
		}
	}
}
