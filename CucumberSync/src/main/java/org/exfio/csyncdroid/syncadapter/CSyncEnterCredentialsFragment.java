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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.R;

public class CSyncEnterCredentialsFragment extends Fragment implements TextWatcher {

    EditText editUserName;
    EditText editPassword;
    Button   buttonSignIn;
    Button   buttonGotoSignUp;
    TextView labelLegalese;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.enter_credentials_csync, container, false);

        editUserName = (EditText) v.findViewById(R.id.userName);
        editUserName.addTextChangedListener(this);

        editPassword = (EditText) v.findViewById(R.id.password);
        editPassword.addTextChangedListener(this);

        buttonSignIn = (Button) v.findViewById(R.id.buttonSignIn);
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                queryServer();
            }
        });

        buttonGotoSignUp = (Button) v.findViewById(R.id.buttonGotoSignUp);
        buttonGotoSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoSignUp();
            }
        });

        labelLegalese =(TextView)v.findViewById(R.id.labelLegalese);
        labelLegalese.setClickable(true);
        labelLegalese.setMovementMethod(LinkMovementMethod.getInstance());
        String textLegalese = v.getResources().getString(R.string.credentials_label_legalese);
        labelLegalese.setText(Html.fromHtml(textLegalese));

        return v;
    }

    void queryServer() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        Bundle args = new Bundle();

        args.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE_CSYNC);

        //Although server settings are not presented to the user we may want to at some stage
        args.putString(FxAccountAccountSettings.KEY_ACCOUNT_SERVER, CSyncAccountSettings.DEFAULT_ACCOUNT_SERVER);
        args.putString(FxAccountAccountSettings.KEY_TOKEN_SERVER, CSyncAccountSettings.DEFAULT_TOKEN_SERVER);

        args.putString(FxAccountAccountSettings.KEY_USERNAME, editUserName.getText().toString());
        args.putString(FxAccountAccountSettings.KEY_PASSWORD, editPassword.getText().toString());

        DialogFragment dialog = new QueryServerDialogFragment();
        dialog.setArguments(args);
        dialog.show(ft, QueryServerDialogFragment.class.getName());
    }

    void gotoSignUp() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        // pass to "sign up" fragment
        CSyncEnterCredentialsSignUpFragment signUpFragment = new CSyncEnterCredentialsSignUpFragment();

        ft.replace(R.id.fragment_container, signUpFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }


    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //TODO - improve validation
        boolean ok = (
                editUserName.getText().length() > 0
                &&
                editPassword.getText().length() > 0
        );
        buttonSignIn.setEnabled(ok);
    }

    public void afterTextChanged(Editable s) {
    }

}
