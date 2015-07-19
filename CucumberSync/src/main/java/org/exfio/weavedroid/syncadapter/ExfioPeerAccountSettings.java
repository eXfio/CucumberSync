package org.exfio.weavedroid.syncadapter;

import android.accounts.Account;
import android.content.Context;

import org.exfio.weave.WeaveException;
import org.exfio.weave.account.WeaveAccount;
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
