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

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

/**
 * Represents a low-level contacts RawContact - or at least the fields of the
 * RawContact that we care about.
 */
final public class RawContact
{

  /**
   * The constant PF_MOBILE_CONTACT_VIEW_BASE_URL.
   */
  public static final String PF_MOBILE_CONTACT_VIEW_BASE_URL = "https://projectforge.micromata.de/wa/m-addressView?id=";

  private static final ThreadLocal<SimpleDateFormat> DF = new ThreadLocal<SimpleDateFormat>()
  {

    protected SimpleDateFormat initialValue()
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat(
          "yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      return dateFormat;
    }
  };

  /** The tag used to log to adb console. **/
  private static final String TAG = "RawContact";

  // private String fullName;

  private String firstName;

  private String lastName;

  private String homeMobilePhone;

  private String workMobilePhone;

  private String homePhone;

  private String workPhone;

  private String workFax;

  private String homeEmail;

  private String workEmail;

  //private String status;

  //private String avatarUrl;

  private byte[] avatar;

  private boolean deleted = false;

  private boolean dirty;

  private long serverContactId;

  private long rawContactId;

  private String company;

  private String division;

  private String position;

  private long syncState = 0;

  private String website;

  private String note;

  private RawAddress addr;

  private RawAddress privateAddr;

  private RawAddress postalAddr;

  //pf specific
  private String communicationLanguage;

  private String publicKey;

  private String addressStatus;

  private String form;

  private String contactStatus;

  private String lastUpdate;

  /**
   * Instantiates a new Raw contact.
   */
  public RawContact()
  {

  }

  /**
   * Gets sync state.
   *
   * @return the sync state
   */
  public long getSyncState()
  {
    return syncState;
  }

  /**
   * Gets best name.
   *
   * @return the best name
   */
  public String getBestName()
  {
    // if (!TextUtils.isEmpty(fullName)) {
    // return fullName;
    // } else
    if (TextUtils.isEmpty(firstName)) {
      return lastName;
    } else {
      return firstName;
    }
  }

  // /**
  // * Convert the RawContact object into a JSON string. From the
  // * JSONString interface.
  // * @return a JSON string representation of the object
  // */
  // public JSONObject toJSONObject() {
  // JSONObject json = new JSONObject();
  //
  // try {
  // if (!TextUtils.isEmpty(mFirstName)) {
  // json.put("f", mFirstName);
  // }
  // if (!TextUtils.isEmpty(mLastName)) {
  // json.put("l", mLastName);
  // }
  // if (!TextUtils.isEmpty(mCellPhone)) {
  // json.put("m", mCellPhone);
  // }
  // if (!TextUtils.isEmpty(mOfficePhone)) {
  // json.put("o", mOfficePhone);
  // }
  // if (!TextUtils.isEmpty(mHomePhone)) {
  // json.put("h", mHomePhone);
  // }
  // if (!TextUtils.isEmpty(mEmail)) {
  // json.put("e", mEmail);
  // }
  // if (mServerContactId > 0) {
  // json.put("i", mServerContactId);
  // }
  // if (mRawContactId > 0) {
  // json.put("c", mRawContactId);
  // }
  // if (mDeleted) {
  // json.put("d", mDeleted);
  // }
  // } catch (final Exception ex) {
  // Log.i(TAG, "Error converting RawContact to JSONObject" + ex.toString());
  // }
  //
  // return json;
  // }

  private static long getLastUpdate(JSONObject contact) throws JSONException
  {

    Long lastUpdate = null;
    String lastUpdateString = contact.getString("lastUpdate");

    try {
      lastUpdate = Long.valueOf(lastUpdateString);
    } catch (Exception ex) {
      try {
        lastUpdate = DF.get().parse(lastUpdateString).getTime();
      } catch (Exception e) {
        Log.w(TAG, e.getMessage(), e);
      }
    }

    if (lastUpdate == null) {
      Log.w(TAG, "Can not convert date to millis: " + lastUpdateString);
      return 0;
    }

    return lastUpdate;
  }

  /**
   * Creates and returns an instance of the RawContact from the provided JSON
   * data.
   *
   * @param contact the contact
   * @return user The new instance of Sample RawContact created from the JSON         data.
   */
  public static RawContact valueOf(JSONObject contact)
  {

    try {

      RawContact rc = new RawContact();

      final int serverContactId = contact.getInt("id");

      //			if (dirtyContacts.containsKey(serverContactId) == false) {
      long lastUpdate = getLastUpdate(contact);
      //if (lastUpdate < serverSyncState) {
      //return null;
      //}
      rc.setSyncState(lastUpdate);
      //			}

      rc.setServerContactId(serverContactId);

      final String firstName = !contact.isNull("firstName") ? contact
          .getString("firstName") : null;
      rc.setFirstName(firstName);

      final String lastName = !contact.isNull("name") ? contact
          .getString("name") : null;
      rc.setLastName(lastName);

      //			if (!firstName.equals("Lado")) {
      //			return null;
      //	}

      rc.setHomeEmail(getSafe(contact, "privateEmail"));

      rc.setWorkEmail(getSafe(contact, "email"));

      rc.setHomeMobilePhone(getSafe(contact, "privateMobilePhone"));

      rc.setWorkMobilePhone(getSafe(contact, "mobilePhone"));

      rc.setHomePhone(getSafe(contact, "privatePhone"));

      rc.setWorkPhone(getSafe(contact, "businessPhone"));

      rc.setWorkFax(getSafe(contact, "fax"));

      rc.setCompany(getSafe(contact, "organization"));
      rc.setDivision(getSafe(contact, "division"));
      rc.setPosition(getSafe(contact, "positionText"));

      String website = getSafe(contact, "website");
      if (TextUtils.isEmpty(website)) {
        website = PF_MOBILE_CONTACT_VIEW_BASE_URL + serverContactId;
      }
      rc.setWebsite(website);

      rc.setNote(getSafe(contact, "comment"));

      RawAddress addr = new RawAddress(getSafe(contact, "addressText"),
          getSafe(contact, "zipCode"), getSafe(contact, "city"),
          getSafe(contact, "state"), getSafe(contact, "country"));

      //if (addr.isEmpty() == false) {
      rc.setAddr(addr);
      //}

      RawAddress privateAddr = new RawAddress(getSafe(contact,
          "privateAddressText"), getSafe(contact, "privateZipCode"),
          getSafe(contact, "privateCity"), getSafe(contact,
          "privateState"), getSafe(contact, "privateCountry"));

      //			if (privateAddr.isEmpty() == false) {
      rc.setPrivateAddr(privateAddr);
      //	}

      RawAddress postalAddr = new RawAddress(getSafe(contact,
          "postalAddressText"), getSafe(contact, "postalZipCode"),
          getSafe(contact, "postalCity"), getSafe(contact,
          "postalState"), getSafe(contact, "postalCountry"));

      //			if (postalAddr.isEmpty() == false) {
      rc.setPostalAddr(postalAddr);
      //			}

      final boolean deleted = !contact.isNull("deleted") ? contact
          .getBoolean("deleted") : false;
      rc.setDeleted(deleted);
      return rc;

    } catch (final Exception ex) {
      Log.i(TAG, "Error parsing JSON contact object" + ex.toString());
    }
    return null;
  }

  /**
   * @param contact
   * @param key
   * @param defValue
   * @return
   */
  private static String getSafe(JSONObject contact, String key)
  {
    return getSafe(contact, key, "");


  }

  /**
   * @param contact
   * @param key
   * @param defValue
   * @return
   */
  private static String getSafe(JSONObject contact, String key,
      String defValue)
  {

    try {
      return contact.getString(key);
    } catch (JSONException ex) {
      return defValue;
    }

  }

  /**
   * Creates and returns a User instance that represents a deleted user. Since
   * the user is deleted, all we need are the client/server IDs.
   *
   * @param rawContactId the raw contact id
   * @param serverContactId the server contact id
   * @return a minimal User object representing the deleted contact.
   */
  public static RawContact createDeletedContact(long rawContactId,
      long serverContactId)
  {
    RawContact d = createModifiedContact(rawContactId, serverContactId);
    d.setDeleted(true);
    return d;

  }

  /**
   * Creates and returns a User instance that represents a modified user.
   * Since the user is deleted, all we need are the client/server IDs.
   *
   * @param rawContactId the raw contact id
   * @param serverContactId the server contact id
   * @return a minimal User object representing the deleted contact.
   */
  public static RawContact createModifiedContact(long rawContactId,
      long serverContactId)
  {
    RawContact d = new RawContact();
    d.setServerContactId(serverContactId);
    d.setRawContactId(rawContactId);
    return d;
  }

  // public String getFullName() {
  // return fullName;
  // }
  //
  // public void setFullName(String fullName) {
  // this.fullName = fullName;
  // }

  /**
   * Gets first name.
   *
   * @return the first name
   */
  public String getFirstName()
  {
    return firstName;
  }

  /**
   * Sets first name.
   *
   * @param firstName the first name
   */
  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  /**
   * Gets last name.
   *
   * @return the last name
   */
  public String getLastName()
  {
    return lastName;
  }

  /**
   * Sets last name.
   *
   * @param lastName the last name
   */
  public void setLastName(String lastName)
  {
    this.lastName = lastName;
  }

  /**
   * Gets home phone.
   *
   * @return the home phone
   */
  public String getHomePhone()
  {
    return homePhone;
  }

  /**
   * Sets home phone.
   *
   * @param homePhone the home phone
   */
  public void setHomePhone(String homePhone)
  {
    this.homePhone = homePhone;
  }
  //
  //	public String getStatus() {
  //		return status;
  //	}
  //
  //	public void setStatus(String status) {
  //		this.status = status;
  //	}

  /**
   * Is deleted boolean.
   *
   * @return the boolean
   */
  public boolean isDeleted()
  {
    return deleted;
  }

  /**
   * Gets server contact id.
   *
   * @return the server contact id
   */
  public long getServerContactId()
  {
    return serverContactId;
  }

  /**
   * Sets server contact id.
   *
   * @param serverContactId the server contact id
   */
  public void setServerContactId(long serverContactId)
  {
    this.serverContactId = serverContactId;
  }

  /**
   * Sets deleted.
   *
   * @param deleted the deleted
   */
  public void setDeleted(boolean deleted)
  {
    this.deleted = deleted;
  }

  /**
   * Is dirty boolean.
   *
   * @return the boolean
   */
  public boolean isDirty()
  {
    return dirty;
  }

  /**
   * Sets dirty.
   *
   * @param dirty the dirty
   */
  public void setDirty(boolean dirty)
  {
    this.dirty = dirty;
  }

  /**
   * Gets raw contact id.
   *
   * @return the raw contact id
   */
  public long getRawContactId()
  {
    return rawContactId;
  }

  /**
   * Sets raw contact id.
   *
   * @param rawContactId the raw contact id
   */
  public void setRawContactId(long rawContactId)
  {
    this.rawContactId = rawContactId;
  }

  /**
   * Gets home email.
   *
   * @return the home email
   */
  public String getHomeEmail()
  {
    return homeEmail;
  }

  /**
   * Sets home email.
   *
   * @param homeEmail the home email
   */
  public void setHomeEmail(String homeEmail)
  {
    this.homeEmail = homeEmail;
  }

  /**
   * Gets work email.
   *
   * @return the work email
   */
  public String getWorkEmail()
  {
    return workEmail;
  }

  /**
   * Sets work email.
   *
   * @param workEmail the work email
   */
  public void setWorkEmail(String workEmail)
  {
    this.workEmail = workEmail;
  }

  /**
   * Gets home mobile phone.
   *
   * @return the home mobile phone
   */
  public String getHomeMobilePhone()
  {
    return homeMobilePhone;
  }

  /**
   * Sets home mobile phone.
   *
   * @param homeMobilePhone the home mobile phone
   */
  public void setHomeMobilePhone(String homeMobilePhone)
  {
    this.homeMobilePhone = homeMobilePhone;
  }

  /**
   * Gets work mobile phone.
   *
   * @return the work mobile phone
   */
  public String getWorkMobilePhone()
  {
    return workMobilePhone;
  }

  /**
   * Sets work mobile phone.
   *
   * @param workMobilePhone the work mobile phone
   */
  public void setWorkMobilePhone(String workMobilePhone)
  {
    this.workMobilePhone = workMobilePhone;
  }

  /**
   * Gets work phone.
   *
   * @return the work phone
   */
  public String getWorkPhone()
  {
    return workPhone;
  }

  /**
   * Sets work phone.
   *
   * @param workPhone the work phone
   */
  public void setWorkPhone(String workPhone)
  {
    this.workPhone = workPhone;
  }

  /**
   * Gets work fax.
   *
   * @return the work fax
   */
  public String getWorkFax()
  {
    return workFax;
  }

  /**
   * Sets work fax.
   *
   * @param workFax the work fax
   */
  public void setWorkFax(String workFax)
  {
    this.workFax = workFax;
  }

  //	public String getAvatarUrl() {
  //		return avatarUrl;
  //	}
  //
  //	public void setAvatarUrl(String avatarUrl) {
  //		this.avatarUrl = avatarUrl;
  //	}

  /**
   * Gets company.
   *
   * @return the company
   */
  public String getCompany()
  {
    return company;
  }

  /**
   * Sets company.
   *
   * @param company the company
   */
  public void setCompany(String company)
  {
    this.company = company;
  }

  /**
   * Gets division.
   *
   * @return the division
   */
  public String getDivision()
  {
    return division;
  }

  /**
   * Sets division.
   *
   * @param division the division
   */
  public void setDivision(String division)
  {
    this.division = division;
  }

  /**
   * Gets position.
   *
   * @return the position
   */
  public String getPosition()
  {
    return position;
  }

  /**
   * Sets position.
   *
   * @param position the position
   */
  public void setPosition(String position)
  {
    this.position = position;
  }

  /**
   * Sets sync state.
   *
   * @param syncState the sync state
   */
  public void setSyncState(long syncState)
  {
    this.syncState = syncState;
  }

  /**
   * Gets website.
   *
   * @return the website
   */
  public String getWebsite()
  {
    return website;
  }

  /**
   * Sets website.
   *
   * @param website the website
   */
  public void setWebsite(String website)
  {
    this.website = website;
  }

  /**
   * Gets note.
   *
   * @return the note
   */
  public String getNote()
  {
    return note;
  }

  /**
   * Sets note.
   *
   * @param note the note
   */
  public void setNote(String note)
  {
    this.note = note;
  }

  /**
   * Gets addr.
   *
   * @return the addr
   */
  public RawAddress getAddr()
  {
    return addr;
  }

  /**
   * Sets addr.
   *
   * @param addr the addr
   */
  public void setAddr(RawAddress addr)
  {
    this.addr = addr;
  }

  /**
   * Gets private addr.
   *
   * @return the private addr
   */
  public RawAddress getPrivateAddr()
  {
    return privateAddr;
  }

  /**
   * Sets private addr.
   *
   * @param privateAddr the private addr
   */
  public void setPrivateAddr(RawAddress privateAddr)
  {
    this.privateAddr = privateAddr;
  }

  /**
   * Gets postal addr.
   *
   * @return the postal addr
   */
  public RawAddress getPostalAddr()
  {
    return postalAddr;
  }

  /**
   * Sets postal addr.
   *
   * @param postalAddr the postal addr
   */
  public void setPostalAddr(RawAddress postalAddr)
  {
    this.postalAddr = postalAddr;
  }

  /**
   * Sets communication language.
   *
   * @param communicationLanguage the communication language
   */
  public void setCommunicationLanguage(final String communicationLanguage)
  {
    this.communicationLanguage = communicationLanguage;
  }

  /**
   * Gets communication language.
   *
   * @return the communication language
   */
  public String getCommunicationLanguage()
  {
    return communicationLanguage;
  }

  public void setPublicKey(final String publicKey)
  {
    this.publicKey = publicKey;
  }

  public void setAddressStatus(final String addressStatus)
  {
    this.addressStatus = addressStatus;
  }

  public void setForm(final String form)
  {
    this.form = form;
  }

  public void finalize()
  {
    if (website == null || website.trim().equals("")) {
      website = RawContact.PF_MOBILE_CONTACT_VIEW_BASE_URL + serverContactId;
    }
    try {
      syncState = Long.valueOf(this.lastUpdate);
    } catch (Exception ex) {
      try {
        syncState = DF.get().parse(this.lastUpdate).getTime();
      } catch (Exception e) {
        Log.w(TAG, e.getMessage(), e);
      }
    }
    if (syncState == 0) {
      Log.w(TAG, "Can not convert date to millis: " + lastUpdate);
    }
  }

  public void setContactStatus(final String contactStatus)
  {
    this.contactStatus = contactStatus;
  }

  public byte[] getAvatar()
  {
    return avatar;
  }

  public void setAvatar(final byte[] avatar)
  {
    this.avatar = avatar;
  }

  public void setLastUpdate(final String lastUpdate)
  {
    this.lastUpdate = lastUpdate;
  }
}
