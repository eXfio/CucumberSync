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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.exfio.csyncdroid.util.SystemUtils;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//DEBUG only
		if ( SystemUtils.isDebuggable(this) ) {
			org.exfio.csyncdroid.util.Log.init("debug");
		}
		
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

	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	public void addAccount(MenuItem item) {
		Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
		
		//EXTRA_ACCOUNT_TYPES supported in JBMR2 and above
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			intent.putExtra(
				Settings.EXTRA_ACCOUNT_TYPES,
				new String[] {
					Constants.ACCOUNT_TYPE_LEGACYV5
					,Constants.ACCOUNT_TYPE_EXFIOPEER
					,Constants.ACCOUNT_TYPE_FXACCOUNT
				}
			);
	    }
		
		startActivity(intent);
	}

	public void showSyncSettings(MenuItem item) {
		Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		startActivity(intent);
	}

	private boolean fromPlayStore() {
		try {
			return "com.android.vending".equals(getPackageManager().getInstallerPackageName("org.exfio.csyncdroid"));
		} catch(IllegalArgumentException e) {
		}
		return false;
	}
}
