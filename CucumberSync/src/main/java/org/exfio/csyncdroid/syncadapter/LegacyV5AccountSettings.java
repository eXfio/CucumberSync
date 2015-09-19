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

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exfio.weave.WeaveException;
import org.exfio.weave.account.WeaveAccount;
import org.exfio.weave.account.legacy.LegacyV5Account;
import org.exfio.csyncdroid.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;

public class LegacyV5AccountSettings extends AccountSettings {
	private final static String TAG = "weave.LegacyV5AccountSettings";

	private final static int SETTINGS_VERSION = 1;

	private final static String ENCODE_PASSWORD_SEPARATOR = "SYNCKEY:";

	public final static String
		KEY_BASE_URL         = "base_url",
		KEY_USERNAME         = "username",
		KEY_PASSWORD         = "password",
		KEY_SYNCKEY          = "synckey",
		KEY_AUTH_PREEMPTIVE  = "auth_preemptive";
	
	public LegacyV5AccountSettings() {
		super();
		accountType     = Constants.ACCOUNT_TYPE_LEGACYV5;
		settingsVersion = SETTINGS_VERSION;
	}
	
	public LegacyV5AccountSettings(Context context, Account account) {
		super(context, account);
		accountType     = Constants.ACCOUNT_TYPE_LEGACYV5;
		settingsVersion = SETTINGS_VERSION;
		checkVersion();
	}

	@Override
	public Bundle createBundle(ServerInfo serverInfo) throws WeaveException {
		Properties prop = null;
		try {
			prop = serverInfo.getAccountParamsAsProperties();
		} catch (IOException e) {
			throw new WeaveException(e);
		}

		return createBundle(prop);
	}

	public Bundle createBundle(Properties prop) throws WeaveException {

		Bundle bundle = new Bundle();
		bundle.putString(KEY_SETTINGS_VERSION, String.valueOf(SETTINGS_VERSION));

		String guid = WeaveAccount.generateAccountGuid(prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SERVER), prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_USERNAME));
		bundle.putString(KEY_GUID,     guid);
		bundle.putString(KEY_BASE_URL, prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SERVER));
		bundle.putString(KEY_USERNAME, prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_USERNAME));
		
		return bundle;
	}
	
	@Override
	public String getPassword(ServerInfo serverInfo) throws WeaveException {
		Properties prop = null;
		try {
			prop = serverInfo.getAccountParamsAsProperties();
		} catch (IOException e) {
			throw new WeaveException(e);
		}
		return encodePassword(prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_PASSWORD), prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SYNCKEY));
	}

	@Override
	public Account createAccount(Context context, String accountName, Properties prop) throws WeaveException {
		accountManager = AccountManager.get(context);
		Account account = new Account(accountName, accountType);

		Bundle userData = createBundle(prop);

		String password = encodePassword(prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_PASSWORD), prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SYNCKEY));

		accountManager.addAccountExplicitly(account, password, userData);

		return account;
	}

	@Override
	public void updateAccount(Properties prop) throws WeaveException {

		//Nothing should change...
		//accountManager.setUserData(account, KEY_BASE_URL, prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SERVER));
		//accountManager.setUserData(account, KEY_USERNAME, prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_USERNAME));

		//accountManager.setPassword(account, encodePassword(prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_PASSWORD), prop.getProperty(LegacyV5Account.KEY_ACCOUNT_CONFIG_SYNCKEY));
	}

	// Weave account settings	
	// FIXME - make password/synckey concatenation more robust, i.e. encode as JSON
	public static String encodePassword(String password, String synckey) {
		return password + ENCODE_PASSWORD_SEPARATOR + synckey;
	}

	public static String decodePassword(String password) {
		Pattern pattern = Pattern.compile("^(.+)" + ENCODE_PASSWORD_SEPARATOR + "(.*)$");
		Matcher matcher = pattern.matcher(password);
		if (matcher.find()) {
		    return matcher.group(1);
		}
		return null;
	}

	public static String decodeSyncKey(String password) {
		Pattern pattern = Pattern.compile("^(.+)" + ENCODE_PASSWORD_SEPARATOR + "(.*)$");
		Matcher matcher = pattern.matcher(password);
		if (matcher.find()) {
		    return matcher.group(2);
		}
		return null;
	}

	public String getBaseURL() {
		return accountManager.getUserData(account, KEY_BASE_URL);
	}
	
	public String getUserName() {
		return accountManager.getUserData(account, KEY_USERNAME);		
	}
	
	public String getPassword() {
		return decodePassword(accountManager.getPassword(account));
	}
	
	public String getSyncKey() {
		return decodeSyncKey(accountManager.getPassword(account));
	}

	public boolean getPreemptiveAuth() {
		return Boolean.parseBoolean(accountManager.getUserData(account, KEY_AUTH_PREEMPTIVE));
	}
}
