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

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import org.exfio.csyncdroid.Constants;
import org.exfio.csyncdroid.R;

public class CSyncEnterCredentialsSignUpFragment extends Fragment implements TextWatcher {
    private static final String TAG = "exfio.CSyncSignUp";

    EditText editUserName;
    EditText editPassword;
    EditText editConfirmPassword;
    EditText editEmail;
    Button   buttonSignUp;
    Button   buttonGotoSignIn;
    TextView labelLegalese;

    String validatedUsername;
    String validatedPassword;
    String validatedEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.enter_credentials_csync_signup, container, false);

        editUserName = (EditText) v.findViewById(R.id.userName);
        editUserName.addTextChangedListener(this);

        editPassword = (EditText) v.findViewById(R.id.password);
        editPassword.addTextChangedListener(this);

        editConfirmPassword = (EditText) v.findViewById(R.id.confirm_password);
        editEmail = (EditText) v.findViewById(R.id.email);

        buttonSignUp = (Button) v.findViewById(R.id.buttonSignUp);
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                signUp();
            }
        });

        buttonGotoSignIn = (Button) v.findViewById(R.id.buttonGotoSignIn);
        buttonGotoSignIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoSignIn();
            }
        });

        labelLegalese =(TextView)v.findViewById(R.id.labelLegaleseSignUp);
        labelLegalese.setClickable(true);
        labelLegalese.setMovementMethod(LinkMovementMethod.getInstance());
        String textLegalese = v.getResources().getString(R.string.credentials_label_legalese);
        labelLegalese.setText(Html.fromHtml(textLegalese));

        validatedUsername = null;

        return v;
    }

    void signUp() {
        validateUsername();
    }

    void validateUsername() {

        //For now support unverified accounts only as UX for email verification sux on mobile
        if ( AddAccountActivity.isValidUsername(editUserName.getText()) ) {
            //Append account server domain to username
            try {
                URI accountServerUri = new URI(CSyncAccountSettings.DEFAULT_ACCOUNT_SERVER);
                validatedUsername = editUserName.getText().toString().toLowerCase() + '@' + accountServerUri.getHost();
                validatePassword();
            } catch (URISyntaxException e) {
                Toast.makeText(getActivity(), "Default account server URI invalid", Toast.LENGTH_LONG);
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Create account")
                    .setMessage("Username is invalid. Please enter a valid username containing alphanumeric characters plus '.', '-' or '_'")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    void validatePassword() {

        //Validate password
        if ( !AddAccountActivity.isValidPassword(editPassword.getText()) ) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Create account")
                    .setMessage("Password is invalid. Please enter a minimum of 8 characters")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        if ( !editPassword.getText().toString().equals(editConfirmPassword.getText().toString()) ) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Create account")
                    .setMessage("Passwords do not match. Please re-enter your password and try again")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        //Passed validation
        validatedPassword = editPassword.getText().toString();
        validateEmail();
    }

    void validateEmail() {

        if ( AddAccountActivity.isValidEmail(editEmail.getText()) ) {
            validatedEmail = editEmail.getText().toString().toLowerCase();
            queryServer();
        } else if ( editEmail.getText().toString().isEmpty() ) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Create account")
                    .setMessage("An email address is required recover your account in the case of a lost password? Are you sure you want to create an unverified account?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            validatedEmail = null;
                            queryServer();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Create account")
                    .setMessage("Email is invalid. Please enter a valid email address or leave blank to create an unverified account")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    void queryServer() {

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        Bundle args = new Bundle();

        args.putString(android.accounts.AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE_CSYNC);
        args.putBoolean(CSyncAccountSettings.KEY_CREATE_ACCOUNT, true);

        //Although server settings are not presented to the user we may want to at some stage
        args.putString(FxAccountAccountSettings.KEY_ACCOUNT_SERVER, CSyncAccountSettings.DEFAULT_ACCOUNT_SERVER);
        args.putString(FxAccountAccountSettings.KEY_TOKEN_SERVER, CSyncAccountSettings.DEFAULT_TOKEN_SERVER);

        args.putString(FxAccountAccountSettings.KEY_USERNAME, validatedUsername);
        args.putString(FxAccountAccountSettings.KEY_PASSWORD, validatedPassword);

        args.putString(FxAccountAccountSettings.KEY_EMAIL, validatedEmail);

        DialogFragment dialog = new QueryServerDialogFragment();
        dialog.setArguments(args);
        dialog.show(ft, QueryServerDialogFragment.class.getName());
    }

    void gotoSignIn() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        // pass to "sign up" fragment
        CSyncEnterCredentialsFragment signInFragment = new CSyncEnterCredentialsFragment();

        ft.replace(R.id.fragment_container, signInFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //getActivity().invalidateOptionsMenu();

        //TODO - improve validation
        boolean ok = (
                editUserName.getText().length() >= 3
                &&
                editPassword.getText().length() >= 8
        );
        buttonSignUp.setEnabled(ok);
    }

    public void afterTextChanged(Editable s) {
    }

}
