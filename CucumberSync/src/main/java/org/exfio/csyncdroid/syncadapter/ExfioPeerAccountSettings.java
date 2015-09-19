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
import android.content.Context;

import org.exfio.weave.WeaveException;
import org.exfio.weave.account.legacy.LegacyV5Account;

import java.util.Properties;

public class ExfioPeerAccountSettings extends LegacyV5AccountSettings {
	private final static String TAG = "weave.ExfioPeerAccountSettings";	

	public ExfioPeerAccountSettings(Context context, Account account) {
		super(context, account);
	}

	public ExfioPeerAccountSettings() {
		super();
	}

	public Account createAccount(Properties prop) throws WeaveException {
		throw new WeaveException("AccountSettings.createAccount() not yet implemented");

		//Build unique account guid that is also valid filename
		//String guid = WeaveAccount.generateAccountGuid(propSettings.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SERVER), username);

	}

	@Override
	public void updateAccount(Properties prop) throws WeaveException {
		accountManager.setPassword(account, encodePassword(prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_PASSWORD), prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SYNCKEY)));
	}

}
