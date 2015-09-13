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
	
	public Double getModifiedTime() {
		String modified = accountManager.getUserData(account, KEY_MODIFIED_TIME);
		if (modified == null || modified.equals("")) {
			return 0D;
		} else {
			return Double.parseDouble(modified);
		}
	}
	
	public void setModifiedTime(Double modified) {
		accountManager.setUserData(account, KEY_MODIFIED_TIME, modified.toString());
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
