package org.exfio.weavedroid.syncadapter;

import java.util.regex.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class AccountSettings {
	private final static String TAG = "weave.AccountSettings";
	
	public final static String KEY_SERVER_INFO = "server_info";
	
	private final static int CURRENT_VERSION = 1;
	private final static String
		KEY_SETTINGS_VERSION = "version",
		KEY_GUID             = "guid",
		KEY_USERNAME         = "username",
		KEY_AUTH_PREEMPTIVE  = "auth_preemptive",
		KEY_BASE_URL         = "base_url",
		KEY_MODIFIED_TIME    = "modified_time";
	
	private final static String ENCODE_PASSWORD_SEPARATOR = "SYNCKEY:";
	
	Context context;
	AccountManager accountManager;
	Account account;
	
	
	public AccountSettings(Context context, Account account) {
		this.context = context;
		this.account = account;
		
		accountManager = AccountManager.get(context);
		
		synchronized(AccountSettings.class) {
			int version = 0;
			try {
				version = Integer.parseInt(accountManager.getUserData(account, KEY_SETTINGS_VERSION));
			} catch(NumberFormatException e) {
			}
			if (version < CURRENT_VERSION)
				update(version);
		}
	}
	
	
	public static Bundle createBundle(ServerInfo serverInfo) {
		Bundle bundle = new Bundle();
		bundle.putString(KEY_SETTINGS_VERSION, String.valueOf(CURRENT_VERSION));
		bundle.putString(KEY_GUID,             serverInfo.getGuid());
		bundle.putString(KEY_BASE_URL,         serverInfo.getBaseURL());
		bundle.putString(KEY_USERNAME,         serverInfo.getUser());
		bundle.putString(KEY_AUTH_PREEMPTIVE,  Boolean.toString(serverInfo.isAuthPreemptive()));
		return bundle;
	}
	
	
	// Weave account settings
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

	public String getGuid() {
		return accountManager.getUserData(account, KEY_GUID);
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
	
	// Collection settings
	
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
		Log.i(TAG, "Account settings must be updated from v" + fromVersion + " to v" + CURRENT_VERSION);
		for (int toVersion = CURRENT_VERSION; toVersion > fromVersion; toVersion--)
			update(fromVersion, toVersion);
	}
	
	private void update(int fromVersion, int toVersion) {
		Log.wtf(TAG, "Don't know how to update settings from v" + fromVersion + " to v" + toVersion);
	}	
}
