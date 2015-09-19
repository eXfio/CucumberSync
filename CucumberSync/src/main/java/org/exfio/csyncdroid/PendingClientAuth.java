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
package org.exfio.csyncdroid;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;

public class PendingClientAuth extends Activity {
	
	private final static String TAG = "PendingClientAuth";
	
	public static final String KEY_EXTRA_NOTIFICATIONID = "notificationid";
	public static final String KEY_EXTRA_ACCOUNTNAME    = "accountname";
	public static final String KEY_EXTRA_AUTHCODE       = "authcode";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
				
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Log.e(TAG, "Error extracting notification data");
			return;
		}
				
		//Set notification text
		String accountName = extras.getString(KEY_EXTRA_ACCOUNTNAME);
		String authCode    = extras.getString(KEY_EXTRA_AUTHCODE);
		setContentView(R.layout.pending_client_auth);
		TextView t = (TextView)findViewById(R.id.pending_ca_text);		
		t.setText(String.format("Enter authcode '%s' on an authenticated device to approve access to CucumberSync account '%s'", authCode, accountName));

		// Remove notification now user has acknowledged it
		int notificationId = extras.getInt(KEY_EXTRA_NOTIFICATIONID);
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(notificationId);
	}
}
