package org.exfio.weavedroid;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;

public class ReceivedClientAuth extends Activity {
	
	private final static String TAG = "ReceivedClientAuth";
	
	public static final String KEY_EXTRA_NOTIFICATIONID = "notificationid";
	public static final String KEY_EXTRA_ACCOUNTNAME    = "accountname";
	public static final String KEY_EXTRA_CLIENTNAME     = "clientname";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
				
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			Log.e(TAG, "Error extracting notification data");
			return;
		}
				
		//Set notification text
		String clientName  = extras.getString(KEY_EXTRA_CLIENTNAME);
		String accountName = extras.getString(KEY_EXTRA_ACCOUNTNAME);
		setContentView(R.layout.approve_client_auth);
		TextView t = (TextView)findViewById(R.id.approve_ca_text);		
		t.setText(String.format("'%s' is requesting authentication for WeaveDroid account '%s'", clientName, accountName));
		
		// Remove notification now user has acknowledged it
		int notificationId = extras.getInt(KEY_EXTRA_NOTIFICATIONID);
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(notificationId);
	}
}
