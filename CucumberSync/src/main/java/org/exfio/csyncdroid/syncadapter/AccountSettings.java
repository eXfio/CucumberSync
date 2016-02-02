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

import org.exfio.weave.WeaveException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.Properties;

public abstract class AccountSettings {
	private final static String TAG = "weave.AccountSettings";
	
	public static final String KEY_SERVER_INFO      = "server_info";
	public static final String KEY_SETTINGS_VERSION = "version";
	public static final String KEY_GUID             = "guid";
	public static final String KEY_MODIFIED_TIME    = "modified_time";

	protected Context context;
	protected AccountManager accountManager;
	protected Account account;
	protected String accountType;
	
	int settingsVersion;

	public AccountSettings() {
	}

	public AccountSettings(Context context, Account account) {
		this.context = context;
		this.account = account;
		this.accountManager = AccountManager.get(context);
	}
	
	public void checkVersion() {
		synchronized(AccountSettings.class) {
			int version = 0;
			try {
				version = Integer.parseInt(accountManager.getUserData(account, KEY_SETTINGS_VERSION));
			} catch(NumberFormatException e) {
			}
			if (version < settingsVersion)
				update(version);
		}
	}
	
	public abstract Bundle createBundle(ServerInfo serverInfo) throws WeaveException;

	public abstract String getPassword(ServerInfo serverInfo) throws WeaveException;

	public abstract Account createAccount(Context context, String accountName, Properties prop) throws WeaveException;

	public abstract void updateAccount(Properties prop) throws WeaveException;

	public String getGuid() {
		return accountManager.getUserData(account, KEY_GUID);
	}

	
	// Remote collection settings
	
	public Double getModifiedTime(String collection) {
		String modified = accountManager.getUserData(account, collection + "_" + KEY_MODIFIED_TIME);
		if (modified == null || modified.equals("")) {
			return 0D;
		} else {
			return Double.parseDouble(modified);
		}
	}
	
	public void setModifiedTime(String collection, Double modified) {
		accountManager.setUserData(account, collection + "_" + KEY_MODIFIED_TIME, modified.toString());
	}
	
	
	// update from previous account settings
	// NOTE: update not yet required
	
	private void update(int fromVersion) {
		Log.i(TAG, "Account settings must be updated from v" + fromVersion + " to v" + settingsVersion);
		for (int toVersion = settingsVersion; toVersion > fromVersion; toVersion--)
			update(fromVersion, toVersion);
	}
	
	private void update(int fromVersion, int toVersion) {
		Log.wtf(TAG, "Don't know how to update settings from v" + fromVersion + " to v" + toVersion);
	}	
}
