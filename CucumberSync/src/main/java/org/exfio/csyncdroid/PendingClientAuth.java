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
		t.setText(String.format("Enter authcode '%s' on an authenticated device to approve acces to WeaveDroid account '%s'", authCode, accountName));

		// Remove notification now user has acknowledged it
		int notificationId = extras.getInt(KEY_EXTRA_NOTIFICATIONID);
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(notificationId);
	}
}
