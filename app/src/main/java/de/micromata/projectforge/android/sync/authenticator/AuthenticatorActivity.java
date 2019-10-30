/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.micromata.projectforge.android.sync.authenticator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import de.micromata.projectforge.android.sync.Constants;
import de.micromata.projectforge.android.sync.R;
import de.micromata.projectforge.android.sync.client.NetworkUtilities;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_CALENDAR;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_CALENDAR;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity
    implements OnClickListener
{
  /**
   * The Intent flag to confirm credentials.
   */
  public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

  /**
   * The Intent extra to store password.
   */
  public static final String PARAM_PASSWORD = "password";

  /**
   * The Intent extra to store username.
   */
  public static final String PARAM_USERNAME = "username";

  /**
   * The Intent extra to store username.
   */
  public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

  /**
   * The tag used to log to adb console.
   */
  private static final String TAG = "AuthenticatorActivity";

  private AccountManager mAccountManager;

  /**
   * Keep track of the login task so can cancel it if requested
   */
  private UserLoginTask mAuthTask = null;

  /**
   * Keep track of the progress dialog so we can dismiss it
   */
  private ProgressDialog mProgressDialog = null;

  /**
   * If set we are just checking that the user knows their credentials; this doesn't cause the
   * user's password or authToken to be changed on the device.
   */
  private Boolean mConfirmCredentials = false;

  /**
   * for posting authentication attempts back to UI thread
   */
  private final Handler mHandler = new Handler();

  //private TextView mMessage;

  private String mPassword;

  private EditText mPasswordEdit;

  /**
   * Was the original caller asking for an entirely new account?
   */
  protected boolean mRequestNewAccount = false;

  private String mUsername;

  private EditText mUsernameEdit;

  private String mUrl;

  private EditText mUrlEdit;

  private Button mButton;

  private static final ArrayList<String> DANGER = new ArrayList<>();

  static {
    DANGER.add(READ_CONTACTS);
    DANGER.add(WRITE_CONTACTS);
  }

  private static final int MY_PERMISSIONS_REQUEST = 13;

  @Override
  public void onRequestPermissionsResult(int requestCode,
      String permissions[], int[] grantResults)
  {
    switch (requestCode) {
      case MY_PERMISSIONS_REQUEST: {
        // If request is cancelled, the result arrays are empty.
        for (int gr : grantResults) {
          if (gr != PERMISSION_GRANTED) {
            say(this, R.string.permission_rationale, Toast.LENGTH_LONG);
            finish();
            return;
          }
        }
        onCreateExec();
      }
    }
  }


  private void onCreateExec()
  {
    mAccountManager = AccountManager.get(this);
    Log.i(TAG, "loading data from Intent");
    final Intent intent = getIntent();
    mUsername = intent.getStringExtra(PARAM_USERNAME);
    mRequestNewAccount = mUsername == null;
    mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS,
        false);
    Log.i(TAG, "    request new: " + mRequestNewAccount);
    requestWindowFeature(Window.FEATURE_LEFT_ICON);
    setContentView(R.layout.login_activity);
    getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
        android.R.drawable.ic_dialog_alert);

    mButton = (Button) findViewById(R.id.ok_button);

    mButton.setOnClickListener(this);

    //mMessage = (TextView) findViewById(R.id.message);
    mUsernameEdit = findViewById(R.id.username_edit);
    mPasswordEdit = (EditText) findViewById(R.id.password_edit);
    mUrlEdit = (EditText) findViewById(R.id.url_edit);

    mUrlEdit.addTextChangedListener(new TextWatcher()
    {

      @Override
      public void onTextChanged(CharSequence s, int start, int before,
          int count)
      {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start,
          int comunt, int after)
      {
      }

      @Override
      public void afterTextChanged(Editable s)
      {
        if (TextUtils.isEmpty(s.toString()) == false) {
          mButton.setEnabled(true);
        } else {
          mButton.setEnabled(false);
        }

      }
    });
    mButton.setEnabled(mUrlEdit.getText().toString().isEmpty() == false);
    if (!TextUtils.isEmpty(mUsername)) {
      mUsernameEdit.setText(mUsername);
    }
  }


  public static void say(final Context ctx, final int msg, final int duration)
  {
    final Toast t = Toast.makeText(ctx, msg, duration);
    t.setGravity(Gravity.CENTER, 0, 0);
    t.show();
  }

  private void checkAndRequestPermission()
  {
    boolean granted = true;
    for (String p : DANGER) {
      if (granted == false) {
        break;
      }
      granted = checkSelfPermission(p) == PERMISSION_GRANTED;
    }

    if (granted == false) {
      boolean shouldShowRequestPermissionRationale = false;
      for (String p : DANGER) {
        if (shouldShowRequestPermissionRationale) {
          break;
        }
        shouldShowRequestPermissionRationale =
            shouldShowRequestPermissionRationale(p);
      }
      if (shouldShowRequestPermissionRationale) {
        say(this, R.string.permission_rationale, Toast.LENGTH_LONG);
      }
      requestPermissions(DANGER.toArray(new String[] {}),
          MY_PERMISSIONS_REQUEST);
    } else {
      onCreateExec();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(Bundle icicle)
  {
    super.onCreate(icicle);

    checkAndRequestPermission();

  }

  /*
   * {@inheritDoc}
   */
  //	@Override
  //	protected Dialog onCreateDialog(int id, Bundle args) {
  protected void createDialog()
  {

    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setMessage(getText(R.string.ui_activity_authenticating));
    dialog.setIndeterminate(true);
    dialog.setCancelable(true);
    dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
    {
      public void onCancel(DialogInterface dialog)
      {
        Log.i(TAG, "user cancelling authentication");
        if (mAuthTask != null) {
          mAuthTask.cancel(true);
        }
      }
    });
    // We save off the progress dialog in a field so that we can dismiss
    // it later. We can't just call dismissDialog(0) because the system
    // can lose track of our dialog if there's an orientation change.
    mProgressDialog = dialog;
  }

  /**
   * Handles onClick event on the Submit button. Sends username/password to the server for
   * authentication.
   *
   * @param view The Submit button for which this method is invoked
   */
  public void handleLogin(View view)
  {
    if (mRequestNewAccount) {
      mUsername = mUsernameEdit.getText().toString();
    }

    mPassword = mPasswordEdit.getText().toString();

    mUrl = mUrlEdit.getText().toString();

    if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)
        || TextUtils.isEmpty(mUrl)) {
      say(getMessage());
    } else {
      // Show a progress dialog, and kick off a background task to perform
      // the user login attempt.
      mAuthTask = new UserLoginTask();
      mAuthTask.execute();
    }
  }

  /**
   * Called when response is received from the server for confirm credentials request. See
   * onAuthenticationResult(). Sets the AccountAuthenticatorResult which is sent back to the
   * caller.
   *
   * @param result the confirmCredentials result.
   */
  private void finishConfirmCredentials(boolean result)
  {
    Log.i(TAG, "finishConfirmCredentials()");
    final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
    // mAccountManager.setPassword(account, mPassword);
    mAccountManager.setPassword(account, null);
    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
  }

  /**
   * Called when response is received from the server for authentication request. See
   * onAuthenticationResult(). Sets the AccountAuthenticatorResult which is sent back to the caller.
   * We store the authToken that's returned from the server as the 'password' for this account - so
   * we're never storing the user's actual password locally.
   *
   * @param result the confirmCredentials result.
   * @throws MalformedURLException
   */
  private void finishLogin(Pair<String, String> authToken)
  {

    Log.i(TAG, "finishLogin()");
    String accountName = mUsername;// + "@"
    // + getHost(mUrl);
    final Account account = new Account(accountName, Constants.ACCOUNT_TYPE);
    if (mRequestNewAccount) {
      mAccountManager.addAccountExplicitly(account, null, null);
      mAccountManager.setAuthToken(account, Constants.ACCOUNT_TYPE,
          authToken.second);
      // mAccountManager.addAccountExplicitly(account, null, null);
      // Set contacts sync for this account.
      ContentResolver.setSyncAutomatically(account,
          ContactsContract.AUTHORITY, true);
    } else {
      // mAccountManager.setPassword(account, mPassword);
      mAccountManager.setAuthToken(account, Constants.ACCOUNT_TYPE,
          authToken.second);
      // mAccountManager.setPassword(account, null);
    }

    mAccountManager.setUserData(account, "url", mUrl);
    mAccountManager.setUserData(account, "id", authToken.first);

    final Intent intent = new Intent();
    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
  }

  public String getHost(String url)
  {
    try {
      return new URL(url).getHost();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }

  }

  /**
   * Called when the authentication process completes (see attemptLogin()).
   *
   * @param authToken the authentication token returned by the server, or NULL if authentication
   *                  failed.
   */
  public void onAuthenticationResult(Pair<String, String> authToken)
  {

    boolean success = ((authToken != null) && (authToken.second.length() > 0));
    Log.i(TAG, "onAuthenticationResult(" + success + ")");

    // Our task is complete, so clear it out
    mAuthTask = null;

    // Hide the progress dialog
    hideProgress();

    if (success) {
      if (!mConfirmCredentials) {
        finishLogin(authToken);
      } else {
        finishConfirmCredentials(success);
      }
    } else {
      Log.e(TAG, "onAuthenticationResult: failed to authenticate");

      if (mRequestNewAccount) {
        // "Please enter a valid username/password.
        say(getText(R.string.login_activity_loginfail_text_both));
      } else {
        // "Please enter a valid password." (Used when the
        // account is already in the database but the password
        // doesn't work.)
        say(getText(R.string.login_activity_loginfail_text_pwonly));
      }
    }
  }

  private void say(CharSequence msg)
  {
    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();

  }

  public void onAuthenticationCancel()
  {
    Log.i(TAG, "onAuthenticationCancel()");

    // Our task is complete, so clear it out
    mAuthTask = null;

    // Hide the progress dialog
    hideProgress();
  }

  /**
   * Returns the message to be displayed at the top of the login dialog box.
   */
  private CharSequence getMessage()
  {
    //getString(R.string.label);
    if (TextUtils.isEmpty(mUsername)) {
      // If no username, then we ask the user to log in using an
      // appropriate service.
      final CharSequence msg = getText(R.string.login_activity_loginfail_text_usermissing);
      return msg;
    }
    if (TextUtils.isEmpty(mPassword)) {
      // We have an account but no password
      return getText(R.string.login_activity_loginfail_text_pwmissing);
    }

    if (TextUtils.isEmpty(mUrl)) {
      // We have no url
      return getText(R.string.login_activity_url_missing);
    }

    return "Erfolg ist programmierbar.";
  }

  /**
   * Shows the progress UI for a lengthy operation.
   */
  private void showProgress()
  {
    createDialog();
    mProgressDialog.show();
  }

  /**
   * Hides the progress UI for a lengthy operation.
   */
  private void hideProgress()
  {
    //dismissDialog(0);
    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
      mProgressDialog = null;
    }
  }

  /**
   * Represents an asynchronous task used to authenticate a user against the ProjectForgeSyncAdapter
   * Service
   */
  public class UserLoginTask extends
      AsyncTask<Void, Void, Pair<String, String>>
  {

    @Override
    protected Pair<String, String> doInBackground(Void... params)
    {
      // We do the actual work of authenticating the user
      // in the NetworkUtilities class.
      try {
        return NetworkUtilities
            .authenticate(mUrl, mUsername, mPassword);
      } catch (Exception ex) {
        Log.e(TAG,
            "UserLoginTask.doInBackground: failed to authenticate");
        Log.i(TAG, ex.toString());
        return null;
      }
    }

    @Override
    protected void onPreExecute()
    {
      showProgress();
    }

    @Override
    protected void onPostExecute(final Pair<String, String> authToken)
    {
      // On a successful authentication, call back into the Activity to
      // communicate the authToken (or null for an error).
      onAuthenticationResult(authToken);
    }

    @Override
    protected void onCancelled()
    {
      // If the action was canceled (by the user clicking the cancel
      // button in the progress dialog), then call back into the
      // activity to let it know.
      onAuthenticationCancel();
    }
  }

  @Override
  public void onClick(View v)
  {
    if (v.getId() == R.id.ok_button) {
      handleLogin(v);
    }
  }
}
