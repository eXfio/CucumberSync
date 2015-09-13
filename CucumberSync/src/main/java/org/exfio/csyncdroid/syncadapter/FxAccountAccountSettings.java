package org.exfio.csyncdroid.syncadapter;

import java.io.IOException;
import java.util.Properties;

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

	public static final String DEFAULT_ACCOUNT_SERVER = "https://api.accounts.firefox.com/v1";
	public static final String DEFAULT_TOKEN_SERVER   = "https://cucumbersync.com/syncserver/token/1.0/sync/1.5";

	public final static String
	KEY_ACCOUNT_SERVER   = "accountserver",
	KEY_TOKEN_SERVER     = "tokenserver",
	KEY_USERNAME         = "username",
	KEY_PASSWORD         = "password",
	KEY_EMAIL            = "email",
	KEY_BROWSERIDCERT    = "browseridcert",
	KEY_KB               = "kb",
	KEY_SYNCTOKEN        = "synctoken";

	public FxAccountAccountSettings() {
		super();
		accountType     = Constants.ACCOUNT_TYPE_FXACCOUNT;
		settingsVersion = SETTINGS_VERSION;
	}
	
	public FxAccountAccountSettings(Context context, Account account) {
		super(context, account);
		accountType     = Constants.ACCOUNT_TYPE_FXACCOUNT;
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
