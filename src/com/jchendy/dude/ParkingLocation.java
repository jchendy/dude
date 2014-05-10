package com.jchendy.dude;

public class ParkingLocation {
	private String _address;
	private String _message;
	private double _lat;
	private double _lng;
	
	public ParkingLocation(String address, String message, double lat,
			double lng) {
		_address = address;
		_message = message;
		_lat = lat;
		_lng = lng;
	}	
	
	public String getMessage() {
		return _message;
	}

	public void setMessage(String message) {
		this._message = message;
	}
	
	public String getAddress() {
		return _address;
	}

	public void setAddress(String address) {
		this._address = address;
	}		

	public double getLat() {
		return _lat;
	}

	public void setLat(double lat) {
		this._lat = lat;
	}

	public double getLng() {
		return _lng;
	}

	public void setLng(double lng) {
		this._lng = lng;
	}		
}