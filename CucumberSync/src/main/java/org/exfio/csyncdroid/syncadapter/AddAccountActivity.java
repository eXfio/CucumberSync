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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.R;

import java.util.regex.Pattern;

public class AddAccountActivity extends Activity {


	private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._\\-]{3,15}$";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account);
		
		Intent intent = getIntent();
		String accountType = intent.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_TYPE);
		
		if (savedInstanceState == null) {	// first call
			Fragment fragment = null;
			String fragmentTag = null;
			if ( accountType.equals(Constants.ACCOUNT_TYPE_CSYNC) ) {
				fragment = new CSyncEnterCredentialsFragment();
				fragmentTag = "enter_credentials_csync";
			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_FXACCOUNT) ) {
				fragment = new FxAccountEnterCredentialsFragment();
				fragmentTag = "enter_credentials_fxaccount";
			} else if ( accountType.equals(Constants.ACCOUNT_TYPE_LEGACYV5) ) {
				fragment = new LegacyV5EnterCredentialsFragment();
				fragmentTag = "enter_credentials_legacyv5";
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

	public final static boolean isValidEmail(CharSequence target) {
		return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
	}

	public final static boolean isValidUsername(CharSequence target) {
		Pattern pattern = Pattern.compile(USERNAME_PATTERN);
		return !TextUtils.isEmpty(target) && pattern.matcher(target).matches();
	}

	public final static boolean isValidPassword(CharSequence target) {
		//FIXME - improve validation
		return !TextUtils.isEmpty(target) && target.length() >= 8;
	}
}
