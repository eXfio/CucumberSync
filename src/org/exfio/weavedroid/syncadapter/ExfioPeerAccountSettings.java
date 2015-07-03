package org.exfio.weavedroid.syncadapter;

import android.accounts.Account;
import android.content.Context;

public class ExfioPeerAccountSettings extends LegacyV5AccountSettings {
	private final static String TAG = "weave.ExfioPeerAccountSettings";	

	public ExfioPeerAccountSettings(Context context, Account account) {
		super(context, account);
	}

	public ExfioPeerAccountSettings() {
		super();
	}
}
