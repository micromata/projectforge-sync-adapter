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

package de.micromata.projectforge.android.sync.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

/**
 * Provides utility methods for communicating with the server.
 */
final public class NetworkUtilities
{
  /** The tag used to log to adb console. */
  private static final String TAG = "NetworkUtilities";

  /** POST parameter name for the user's account name */
  public static final String PARAM_USERNAME = "authenticationUsername";

  /** POST parameter name for the user's password */
  public static final String PARAM_PASSWORD = "authenticationPassword";

  /** POST parameter name for the user's authentication token */
  public static final String PARAM_AUTH_TOKEN = "authenticationToken";

  /** parameter name for the user's id */
  public static final String PARAM_USER_ID = "authenticationUserId";

  /**
   * Last last time
   */
  public static final String PARAM_MODIFIED_SINCE = "modifiedSince";

  /** POST parameter name for the client's last-known sync state */
  public static final String PARAM_SYNC_STATE = "syncstate";

  /** POST parameter name for the sending client-edited contact info */
  public static final String PARAM_CONTACTS_DATA = "contacts";

  /** Timeout (in ms) we specify for each http request */
  public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;
  /** Base URL for the v2 Sample Sync Service */
  // public static final String BASE_URL =
  // "https://projectforge.micromata.de/rest";

  /** URI for authentication service */
  public static final String AUTH_URI_PATH = "/rest/authenticate/getToken";
  /** URI for initializing authentication service */
  // public static final String INIT_URI_PATH =
  // "/rest/authenticate/initialContact";

  /** URI for sync service */
  public static final String SYNC_CONTACTS_URI = "/rest/address/list";

  private NetworkUtilities()
  {
  }

  /**
   * Configures the httpClient to connect to the URL provided.
   *
   * @return the http client
   */
  public static HttpClient getHttpClient()
  {
    HttpClient httpClient = new DefaultHttpClient();
    final HttpParams params = httpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(params,
        HTTP_REQUEST_TIMEOUT_MS);
    HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
    ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
    return httpClient;
  }

  /**
   * Connects to the SampleSync test server, authenticates the provided
   * username and password.
   *
   * @param baseUrl the base url
   * @param username The server account username
   * @param password The server account password
   * @return Pair<String String> The <userId, authentication token> returned by the server (or null)
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  public static Pair<String, String> authenticate(String baseUrl,
      String username, String password) throws UnsupportedEncodingException
  {

    final HttpResponse resp;

    StringBuilder urlBuilder = new StringBuilder();

    urlBuilder.append(baseUrl).append(AUTH_URI_PATH);

    Log.i(TAG, "Authenticating to: " + urlBuilder.toString());

    urlBuilder.append("?").append(PARAM_USERNAME).append("=").append(URLEncoder.encode(username)).append("&").append
        (PARAM_PASSWORD).append("=").append(URLEncoder.encode(password));

    final HttpGet authRequest = new HttpGet(urlBuilder.toString());
    //final HttpPost post = new HttpPost(urlBuilder.toString());
    //ArrayList<NameValuePair> creds = new ArrayList<NameValuePair>(2);
    //creds.add(new BasicNameValuePair(PARAM_USERNAME, username));
    //creds.add(new BasicNameValuePair(PARAM_PASSWORD, password));
    //post.setEntity(new UrlEncodedFormEntity(creds));

    try {
      resp = getHttpClient().execute(authRequest);
      Pair<String, String> auth = null;
      if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        InputStream istream = (resp.getEntity() != null) ? resp
            .getEntity().getContent() : null;
        if (istream != null) {
          JSONObject obj = new JSONObject(slurp(istream));
          Boolean delted = obj.getBoolean("deleted");

          if (delted == true) {
            throw new IllegalStateException("User deleted!");
          } else {
            auth = Pair.create(obj.getString("id"),
                obj.getString("authenticationToken"));
          }

        }
      }
      if ((auth != null)) {
        Log.v(TAG, "Successful authentication");
        return auth;
      } else {
        Log.e(TAG, "Error authenticating" + resp.getStatusLine());
        return null;
      }
    } catch (final IOException e) {
      Log.e(TAG, "IOException when getting authtoken", e);
      return null;
    } catch (JSONException e) {
      Log.e(TAG, "JSONException when getting authtoken", e);
      return null;
    } finally {
      Log.v(TAG, "getAuthtoken completing");
    }
  }

  /**
   * Slurp string.
   *
   * @param in the in
   * @return the string
   * @throws IOException the io exception
   */
  public static String slurp(InputStream in) throws IOException
  {
    StringBuilder out = new StringBuilder();
    byte[] b = new byte[4096];
    for (int n; (n = in.read(b)) != -1; ) {
      out.append(new String(b, 0, n));
    }
    return out.toString();
  }

  /**
   * Perform 2-way sync with the server-side contacts. We send a request that
   * includes all the locally-dirty contacts so that the server can process
   * those changes, and we receive (and return) a list of contacts that were
   * updated on the server-side that need to be updated locally.
   *
   * @param context the context
   * @param account The account being synced
   * @param authtoken The authtoken stored in the AccountManager for this account
   * @param serverSyncState A token returned from the server on the last sync
   * @return A list of contacts that we need to update locally
   * @throws JSONException the json exception
   * @throws ParseException the parse exception
   * @throws IOException the io exception
   * @throws AuthenticationException the authentication exception
   */
  public static List<RawContact> syncContacts(Context context,
      Account account, String authtoken, long serverSyncState)
      throws JSONException, ParseException, IOException,
      AuthenticationException
  {
    // Convert our list of User objects into a list of JSONObject
    // List<JSONObject> jsonContacts = new ArrayList<JSONObject>();
    // for (RawContact rawContact : dirtyContacts)
    // jsonContacts.add(rawContact.toJSONObject());
    // }

    // Create a special JSONArray of our JSON contacts
    // JSONArray buffer = new JSONArray(jsonContacts);

    // Create an array that will hold the server-side contacts
    // that have been changed (returned by the server).
    final ArrayList<RawContact> serverDirtyList = new ArrayList<RawContact>();

    // Prepare our POST data
    // final ArrayList<NameValuePair> params = new
    // ArrayList<NameValuePair>();
    // params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
    // params.add(new BasicNameValuePair(PARAM_AUTH_TOKEN, authtoken));
    // params.add(new BasicNameValuePair(PARAM_CONTACTS_DATA,
    // buffer.toString()));

    String baseUrl = AccountManager.get(context)
        .getUserData(account, "url");
    String userId = AccountManager.get(context).getUserData(account, "id");

    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(baseUrl).append(SYNC_CONTACTS_URI).append("?")
        .append("settings.dateTimeFormat=MILLIS_SINCE_1970");

    Log.i(TAG, "Syncing to: " + urlBuilder.toString());

    urlBuilder.append("&").append(PARAM_AUTH_TOKEN).append("=")
        .append(authtoken).append("&").append(PARAM_USER_ID)
        .append("=").append(userId);

    // serverSyncState = 0;
    if (serverSyncState > 0) {
      urlBuilder.append("&").append(PARAM_MODIFIED_SINCE).append("=")
          .append(serverSyncState + 1);
    }

    final HttpGet get = new HttpGet(urlBuilder.toString());

    // Send the updated friends data to the server

    final HttpResponse resp = getHttpClient().execute(get);


    //    final String response = EntityUtils.toString(resp.getEntity(), "utf-8");
    if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      // Our request to the server was successful - so we assume
      // that they accepted all the changes we sent up, and
      // that the response includes the contacts that we need
      // to update on our side...
      InputStream is = resp.getEntity().getContent();

      parse(serverDirtyList, is, context);

      System.err.println("Done with parsing###########################3");

      //final JSONArray serverContacts = new JSONArray(response);
      // Log.d(TAG, response);
      //      for (int i = 0; i < serverContacts.length(); i++) {
      //    //  RawContact rawContact = RawContact.valueOf(serverContacts
      //        .getJSONObject(i));
      //  if (rawContact != null) {
      //  serverDirtyList.add(rawContact);
      //}
      //}
    } else {
      if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
          || resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
        Log.e(TAG, "Authentication exception in sending dirty contacts");
        throw new AuthenticationException();
      } else {
        Log.e(TAG,
            "Server error in sending dirty contacts: "
                + resp.getStatusLine());
        throw new IOException();
      }
    }

    return serverDirtyList;
  }

  private static void parse(final ArrayList<RawContact> serverDirtyList, final InputStream is, Context context)
  {
    new Parser().parse(serverDirtyList, is, context);
  }

  /**
   * Download the avatar image from the server.
   *
   * @param avatarUrl the URL pointing to the avatar image
   * @return a byte array with the raw JPEG avatar image
   */
  public static byte[] downloadAvatar(final String avatarUrl)
  {
    // If there is no avatar, we're done
    if (TextUtils.isEmpty(avatarUrl)) {
      return null;
    }

    try {
      Log.i(TAG, "Downloading avatar: " + avatarUrl);
      // Request the avatar image from the server, and create a bitmap
      // object from the stream we get back.
      URL url = new URL(avatarUrl);
      HttpURLConnection connection = (HttpURLConnection) url
          .openConnection();
      connection.connect();
      try {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        final Bitmap avatar = BitmapFactory.decodeStream(
            connection.getInputStream(), null, options);

        // Take the image we received from the server, whatever format
        // it
        // happens to be in, and convert it to a JPEG image. Note: we're
        // not resizing the avatar - we assume that the image we get
        // from
        // the server is a reasonable size...
        Log.i(TAG, "Converting avatar to JPEG");
        ByteArrayOutputStream convertStream = new ByteArrayOutputStream(
            avatar.getWidth() * avatar.getHeight() * 4);
        avatar.compress(Bitmap.CompressFormat.JPEG, 95, convertStream);
        convertStream.flush();
        convertStream.close();
        // On pre-Honeycomb systems, it's important to call recycle on
        // bitmaps
        avatar.recycle();
        return convertStream.toByteArray();
      } finally {
        connection.disconnect();
      }
    } catch (MalformedURLException muex) {
      // A bad URL - nothing we can really do about it here...
      Log.e(TAG, "Malformed avatar URL: " + avatarUrl);
    } catch (IOException ioex) {
      // If we're unable to download the avatar, it's a bummer but not the
      // end of the world. We'll try to get it next time we sync.
      Log.e(TAG, "Failed to download user avatar: " + avatarUrl);
    }
    return null;
  }

}
