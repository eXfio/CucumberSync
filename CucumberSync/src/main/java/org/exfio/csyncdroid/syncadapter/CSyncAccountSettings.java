/*
 * Copyright (C) 2016 Gerry Healy <nickel_chrome@exfio.org> and contributors
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
 * This program is derived from DAVdroid, Copyright (C) 2014 Richard Hirner, bitfire web engineering
 * DavDroid is distributed under the terms of the GNU Public License v3.0, https://github.com/bitfireAT/davdroid
 */

package org.exfio.csyncdroid.syncadapter;

import android.accounts.Account;
import android.content.Context;

import org.exfio.csyncdroid.Constants;

public class CSyncAccountSettings extends FxAccountAccountSettings {
    private final static String TAG = "weave.CsyncAccountSettings";

    private static final int SETTINGS_VERSION = 1;

    public static final String DEFAULT_ACCOUNT_SERVER = "https://api.accounts.cucumbersync.com/v1";
    public static final String DEFAULT_TOKEN_SERVER   = "https://cucumbersync.com/token/1.0/sync/1.5";

    // Override properties of superclass FxAccountAccountSettings
    //@Override
    protected static String accountType() { return Constants.ACCOUNT_TYPE_CSYNC; }
    //@Override
    protected static int settingsVersion() { return SETTINGS_VERSION; }

    public CSyncAccountSettings() {
        super();
    }

    public CSyncAccountSettings(Context context, Account account) {
        super(context, account);
    }

}
