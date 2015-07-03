package org.exfio.weavedroid.syncadapter;

import java.io.IOException;
import java.util.Properties;

import org.exfio.weave.WeaveException;
import org.exfio.weave.account.fxa.FxAccount;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;

public class FxAccountAccountSettings extends AccountSettings {
	private final static String TAG = "weave.FxAccountAccountSettings";

	private static final int SETTINGS_VERSION = 1;

	public final static String
	KEY_ACCOUNT_SERVER   = "accountserver",
	KEY_TOKEN_SERVER     = "tokenserver",
	KEY_USERNAME         = "username",
	KEY_PASSWORD         = "password",
	KEY_WRAPKB           = "wrapkb",
	KEY_BROWSERIDCERT    = "browseridcert";
	
	public FxAccountAccountSettings() {
		super();
	}
	
	public FxAccountAccountSettings(Context context, Account account) {
		super(context, account);
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

		Bundle bundle = new Bundle();
		bundle.putString(KEY_SETTINGS_VERSION, String.valueOf(SETTINGS_VERSION));
		bundle.putString(KEY_GUID,             serverInfo.getGuid());

		bundle.putString(KEY_ACCOUNT_SERVER,   prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_SERVER));
		bundle.putString(KEY_TOKEN_SERVER,     prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_TOKENSERVER));
		bundle.putString(KEY_USERNAME,         prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_USERNAME));
		bundle.putString(KEY_WRAPKB,           prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_WRAPKB));
		bundle.putString(KEY_BROWSERIDCERT,    prop.getProperty(FxAccount.KEY_ACCOUNT_CONFIG_BROWSERIDCERT));
				
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
}
