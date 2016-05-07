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

import org.exfio.csyncdroid.BuildConfig;
import org.exfio.csyncdroid.util.Log;
import org.exfio.weave.WeaveException;
import org.exfio.weave.account.WeaveAccount;
import org.exfio.weave.account.fxa.FxAccount;
import org.exfio.csyncdroid.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;

public class FxAccountAccountSettings extends AccountSettings {
	private final static String TAG = "weave.FxAccountAccountSettings";

	private static final int SETTINGS_VERSION = 1;

	public static final String DEFAULT_ACCOUNT_SERVER = BuildConfig.FXA_DEFAULT_ACCOUNT_SERVER;
	public static final String DEFAULT_TOKEN_SERVER   = BuildConfig.FXA_DEFAULT_TOKEN_SERVER;

	public final static String
	KEY_ACCOUNT_SERVER   = "accountserver",
	KEY_TOKEN_SERVER     = "tokenserver",
	KEY_USERNAME         = "username",
	KEY_PASSWORD         = "password",
	KEY_EMAIL            = "email",
	KEY_BROWSERIDCERT    = "browseridcert",
	KEY_KB               = "kb",
	KEY_SYNCTOKEN        = "synctoken";

	// Allow subclasses, i.e. CSyncAccountSettings, to override account properties
	protected String accountType() { return Constants.ACCOUNT_TYPE_FXACCOUNT; }
	protected int settingsVersion() { return SETTINGS_VERSION; }

	public FxAccountAccountSettings() {
		super();
		accountType     = accountType();
		settingsVersion = settingsVersion();
	}
	
	public FxAccountAccountSettings(Context context, Account account) {
		super(context, account);
		accountType     = accountType();
		settingsVersion = settingsVersion();
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

		String guid = WeaveAccount.generateAccountGuid(prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SERVER), prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_USERNAME));
		bundle.putString(KEY_GUID,             guid);
		bundle.putString(KEY_ACCOUNT_SERVER,   prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SERVER));
		bundle.putString(KEY_TOKEN_SERVER,     prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_TOKENSERVER));
		bundle.putString(KEY_USERNAME,         prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_USERNAME));
		bundle.putString(KEY_EMAIL,            prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_EMAIL));
		bundle.putString(KEY_BROWSERIDCERT,    prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_BROWSERIDCERT));
		bundle.putString(KEY_KB,               prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_KB));
		bundle.putString(KEY_SYNCTOKEN,        prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SYNCTOKEN));

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
		return prop.getProperty(KEY_PASSWORD);
	}

	@Override
	public Account createAccount(Context context, String accountName, Properties prop) throws WeaveException {
		Log.d(TAG, "createAccount()");
		Log.d(TAG, "accountType: " + accountType);

		accountManager = AccountManager.get(context);
		Account account = new Account(accountName, accountType);

		Bundle userData = createBundle(prop);

		accountManager.addAccountExplicitly(account, prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_PASSWORD), userData);

		return account;
	}

	@Override
	public void updateAccount(Properties prop) throws WeaveException {

		//There is currently no way for these to change
		//accountManager.setUserData(account, KEY_ACCOUNT_SERVER,   prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SERVER));
		//accountManager.setUserData(account, KEY_TOKEN_SERVER,     prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_TOKENSERVER));
		//accountManager.setUserData(account, KEY_USERNAME,         prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_USERNAME));

		accountManager.setUserData(account, KEY_EMAIL,            prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_EMAIL));
		accountManager.setUserData(account, KEY_BROWSERIDCERT,    prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_BROWSERIDCERT));
		accountManager.setUserData(account, KEY_KB,               prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_KB));
		accountManager.setUserData(account, KEY_SYNCTOKEN, prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SYNCTOKEN));

		//accountManager.setPassword(account, prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_PASSWORD));
	}

	public String getAccountServer() {
		return accountManager.getUserData(account, KEY_ACCOUNT_SERVER);
	}
	
	public String getTokenServer() {
		return accountManager.getUserData(account, KEY_TOKEN_SERVER);
	}

	public String getUserName() {
		return accountManager.getUserData(account, KEY_USERNAME);		
	}
	
	public String getPassword() {
		return accountManager.getPassword(account);
	}

	public String getEmail() {
		return accountManager.getUserData(account, KEY_EMAIL);
	}

	public String getBrowserIdCertificate() {
		return accountManager.getUserData(account, KEY_BROWSERIDCERT);
	}

	public String getKb() {
		return accountManager.getUserData(account, KEY_KB);
	}

	public String getSyncToken() {
		return accountManager.getUserData(account, KEY_SYNCTOKEN);
	}
}
