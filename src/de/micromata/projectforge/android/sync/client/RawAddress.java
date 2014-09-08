package de.micromata.projectforge.android.sync.client;

import android.text.TextUtils;

public class RawAddress {

	//Street and Nr
	private String addressText;

	private String zipCode;

	private String city;

	private String state;

	private String country;


	public RawAddress(String addressText, String zipCode, String city, String state, String country) {
		this.addressText = safeValue(addressText);
		this.zipCode = safeValue(zipCode);
		this.city = safeValue(city);
		this.state = safeValue(state);
		this.country = safeValue(country);
	}
	
	private static String safeValue(String value){
		return value != null ? value : ""; 
	}

	public boolean isEmpty(){
		return TextUtils.isEmpty(this.addressText) &&
				TextUtils.isEmpty(this.zipCode) &&
				TextUtils.isEmpty(this.city) &&
				TextUtils.isEmpty(this.state) &&
				TextUtils.isEmpty(this.country);
	}

	public String getAddressText() {
		return addressText;
	}

	public void setAddressText(String addressText) {
		this.addressText = addressText;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
}
