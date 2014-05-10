package com.jchendy.dude;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends ActionBarActivity  implements
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener  {
	
	private static final int HISTORY_SIZE = 5;
	
	private LocationClient _locationClient;
	private GoogleMap _map;
	private Marker _currentMarker;
	private TextView _messageView;
	private boolean _initLocation = true;
    private GsonBuilder _gsonb;
    private Gson _gson;	
    private List<ParkingLocation> _locations;
	
	//find button
	public void find(View view) {
		if(_currentMarker != null) {
			_map.animateCamera(CameraUpdateFactory.newLatLng(_currentMarker.getPosition()));
		} else {
			Toast.makeText(getApplicationContext(), "You haven't parked yet", 
					   Toast.LENGTH_LONG).show();
		}
	}	
	
	//park button
	public void park(View view) {
    	Location currentLocation = _locationClient.getLastLocation();

    	if(currentLocation != null) {
    		LatLng current = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
    		showParkDialog(current, "Park at current location?");
    	} else {
			Toast.makeText(getApplicationContext(), "Couldn't get current location", 
					   Toast.LENGTH_LONG).show();
		}
	    		
	}
	
	private void parkAtLocation(ParkingLocation loc) {
		parkAtLocation(loc, true);
	}
	
	private void parkAtLocation(ParkingLocation loc, boolean updateHistory) {
		if(updateHistory) {
			while (_locations.size() >= HISTORY_SIZE) {
				_locations.remove(HISTORY_SIZE - 1);
			}
			_locations.add(0, loc);
			//regenerate the menu with recent locations
			invalidateOptionsMenu();			
		}
		
		LatLng latLng = new LatLng(loc.getLat(), loc.getLng());
		
		if(_currentMarker != null) {
			_currentMarker.remove();
		}		
		
	    _map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
	    
	    String markerText = "Car";
    	markerText += "\n" + loc.getMessage();
	    
        _currentMarker = _map.addMarker(new MarkerOptions()
	        .title(markerText)
	        .position(latLng));		
        
        _messageView.setText(loc.getAddress() + "\n" + loc.getMessage());
        
        saveLocations();
	}
	
	//save recent locations to shared prefs
	private void saveLocations() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String json = _gson.toJson(_locations);
        editor.putString(getString(R.string.locations_json), json);
        editor.commit();		
	}
	
	//load recent locations from shared prefs
	private void loadLocations() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String json = sharedPref.getString(getString(R.string.locations_json), "");
        if(!"".equals(json)){
	        ParkingLocation[] arr = _gson.fromJson(json, ParkingLocation[].class);
	        
	        for(ParkingLocation l : arr) {
	        	_locations.add(l);
	        }
	        
	        if(_locations.size() > 0) {
	        	parkAtLocation(_locations.get(0), false);
	        }
        }
		//regenerate the menu with recent locations
		invalidateOptionsMenu();        
	}
	
	private void showParkDialog(final LatLng latLng, String message) {
    	
		//create a layout for the input form
		final LinearLayout in = new LinearLayout(MainActivity.this);
    	in.setOrientation(LinearLayout.VERTICAL);    	
        LayoutParams LLParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        in.setLayoutParams(LLParams);
    	
        //text label and input for the form
		final EditText input = new EditText(MainActivity.this);
		final TextView text = new TextView(MainActivity.this);
		text.setText("Message:");
		
		//add text and input
		in.addView(text);
		in.addView(input);
		
		String address = "<unknown address>";
		
		try {
			List<Address> addresses = new Geocoder(this).getFromLocation(latLng.latitude, latLng.longitude, 1);
			if(addresses.size() > 0) {
				address = addresses.get(0).getAddressLine(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final String finalAddress = address;
    	
    	//show the dialog
    	new AlertDialog.Builder(MainActivity.this)
        .setTitle("Park car")
        .setView(in)
        .setMessage(message + "\nNear " + finalAddress)
        .setPositiveButton("Park", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
            	ParkingLocation location = new ParkingLocation(finalAddress, input.getText().toString(), latLng.latitude, latLng.longitude);
            	parkAtLocation(location);
            }
         })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
                // do nothing
            }
         })
        .show();		
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        _locationClient = new LocationClient(this, this, this);
    
        _map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        
        _messageView = (TextView)findViewById(R.id.message);
		
        _map.setMyLocationEnabled(true);   
        
        _map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng point) {
            	showParkDialog(point, "Park at location you tapped?");
            	}
        });    
        
        _gsonb = new GsonBuilder();
        _gson = _gsonb.create();
        
        _locations = new LinkedList<ParkingLocation>(); 
        
        loadLocations();
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
    
    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        _locationClient.connect();
    }
    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        _locationClient.disconnect();
        super.onStop();
    }    

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		
		if(_initLocation) {
			_initLocation = false;
	        Location location = _locationClient.getLastLocation();
	
	        if (location != null) {
	            LatLng myLocation = new LatLng(location.getLatitude(),
	                    location.getLongitude());
	            _map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,
	                    13));              
	        }
		}
		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		//wipe out all existing items and add recent locations
		menu.clear();
	    for(ParkingLocation loc : _locations) {
	    	final ParkingLocation finalLoc = loc;
	    	
	    	MenuItem item = menu.add(loc.getAddress() + " | " + loc.getMessage());
	    	item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					parkAtLocation(finalLoc);
					return true;
				}
			});
	    }
	    return true;
	}		
	
}
