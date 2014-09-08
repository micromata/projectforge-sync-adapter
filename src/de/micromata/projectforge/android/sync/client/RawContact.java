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
final public class RawContact {

	private static final String PF_MOVILE_CONTACT_VIEW_BASE_URL = "https://projectforge.micromata.de/wa/m-addressView?id=";
	
	private static final ThreadLocal<SimpleDateFormat> DF = new ThreadLocal<SimpleDateFormat>() {

		protected SimpleDateFormat initialValue() {
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

	private boolean deleted;

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

	public RawContact() {

	}

	public long getSyncState() {
		return syncState;
	}

	public String getBestName() {
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

	private static long getLastUpdate(JSONObject contact) throws JSONException {

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
	 * @param dirtyContacts
	 *
	 * @param user
	 *            The JSONObject containing user data
	 * @return user The new instance of Sample RawContact created from the JSON
	 *         data.
	 */
	public static RawContact valueOf(JSONObject contact) {

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
			if(TextUtils.isEmpty(website)){
				website = PF_MOVILE_CONTACT_VIEW_BASE_URL + serverContactId;
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
	private static String getSafe(JSONObject contact, String key) {
		return getSafe(contact, key, "");
		
		
	}

	/**
	 * @param contact
	 * @param key
	 * @param defValue
	 * @return
	 */
	private static String getSafe(JSONObject contact, String key,
			String defValue) {

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
	 * @param clientUserId
	 *            The client-side ID for the contact
	 * @param serverUserId
	 *            The server-side ID for the contact
	 * @return a minimal User object representing the deleted contact.
	 */
	public static RawContact createDeletedContact(long rawContactId,
			long serverContactId) {
		RawContact d = createModifiedContact(rawContactId, serverContactId);
		d.setDeleted(true);
		return d;

	}

	/**
	 * Creates and returns a User instance that represents a modified user.
	 * Since the user is deleted, all we need are the client/server IDs.
	 *
	 * @param clientUserId
	 *            The client-side ID for the contact
	 * @param serverUserId
	 *            The server-side ID for the contact
	 * @return a minimal User object representing the deleted contact.
	 */
	public static RawContact createModifiedContact(long rawContactId,
			long serverContactId) {
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

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getHomePhone() {
		return homePhone;
	}

	public void setHomePhone(String homePhone) {
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

	public boolean isDeleted() {
		return deleted;
	}

	public long getServerContactId() {
		return serverContactId;
	}

	public void setServerContactId(long serverContactId) {
		this.serverContactId = serverContactId;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public long getRawContactId() {
		return rawContactId;
	}

	public void setRawContactId(long rawContactId) {
		this.rawContactId = rawContactId;
	}

	public String getHomeEmail() {
		return homeEmail;
	}

	public void setHomeEmail(String homeEmail) {
		this.homeEmail = homeEmail;
	}

	public String getWorkEmail() {
		return workEmail;
	}

	public void setWorkEmail(String workEmail) {
		this.workEmail = workEmail;
	}

	public String getHomeMobilePhone() {
		return homeMobilePhone;
	}

	public void setHomeMobilePhone(String homeMobilePhone) {
		this.homeMobilePhone = homeMobilePhone;
	}

	public String getWorkMobilePhone() {
		return workMobilePhone;
	}

	public void setWorkMobilePhone(String workMobilePhone) {
		this.workMobilePhone = workMobilePhone;
	}

	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkPhone(String workPhone) {
		this.workPhone = workPhone;
	}

	public String getWorkFax() {
		return workFax;
	}

	public void setWorkFax(String workFax) {
		this.workFax = workFax;
	}

//	public String getAvatarUrl() {
//		return avatarUrl;
//	}
//
//	public void setAvatarUrl(String avatarUrl) {
//		this.avatarUrl = avatarUrl;
//	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		this.division = division;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public void setSyncState(long syncState) {
		this.syncState = syncState;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public RawAddress getAddr() {
		return addr;
	}

	public void setAddr(RawAddress addr) {
		this.addr = addr;
	}

	public RawAddress getPrivateAddr() {
		return privateAddr;
	}

	public void setPrivateAddr(RawAddress privateAddr) {
		this.privateAddr = privateAddr;
	}

	public RawAddress getPostalAddr() {
		return postalAddr;
	}

	public void setPostalAddr(RawAddress postalAddr) {
		this.postalAddr = postalAddr;
	}

}
