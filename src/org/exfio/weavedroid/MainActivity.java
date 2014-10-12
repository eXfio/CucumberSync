/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package org.exfio.weavedroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.exfio.weavedroid.R;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		TextView tvWorkaround = (TextView)findViewById(R.id.text_workaround);
		if (fromPlayStore()) {
			tvWorkaround.setVisibility(View.VISIBLE);
			tvWorkaround.setText(Html.fromHtml(getString(R.string.html_main_workaround)));
		    tvWorkaround.setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		TextView tvInfo = (TextView)findViewById(R.id.text_info);
		tvInfo.setText(Html.fromHtml(getString(R.string.html_main_info, Constants.APP_VERSION)));
		tvInfo.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity, menu);
	    return true;
	}

	
	public void addAccount(MenuItem item) {
		Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
		startActivity(intent);
	}

	public void showSyncSettings(MenuItem item) {
		Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		startActivity(intent);
	}

	public void showWebsite(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Constants.WEB_URL_HELP + "&pk_kwd=main-activity"));
		startActivity(intent);
	}
	
	
	private boolean fromPlayStore() {
		try {
			return "com.android.vending".equals(getPackageManager().getInstallerPackageName("org.exfio.weavedroid"));
		} catch(IllegalArgumentException e) {
		}
		return false;
	}
}
