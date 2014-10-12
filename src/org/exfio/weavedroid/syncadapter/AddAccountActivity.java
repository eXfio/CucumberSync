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
package org.exfio.weavedroid.syncadapter;

import android.app.Activity;
import android.os.Bundle;
import org.exfio.weavedroid.R;

public class AddAccountActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_account);
		
		if (savedInstanceState == null) {	// first call
			getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, new EnterCredentialsFragment(), "enter_credentials")
				.commit();
		}
	}
}
