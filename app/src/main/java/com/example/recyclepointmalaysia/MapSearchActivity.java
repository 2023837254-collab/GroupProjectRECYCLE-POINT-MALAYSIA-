package com.example.recyclepointmalaysia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapSearchActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText editTextSearch;
    private ImageButton btnSearch;

    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;

    private String currentNegeri = "";
    private LatLng selectedPlaceLocation = null;
    private String selectedPlaceName = "";
    private String selectedPlaceAddress = "";

    // Bottom Card Views
    private CardView cardBottomInfo;
    private TextView tvPlaceName, tvAddress;
    private Button btnGetDirections, btnSaveLocation;

    // State center coordinates
    private static final LatLng SELANGOR_CENTER = new LatLng(3.473295505389695, 101.51518230127849);
    private static final LatLng JOHOR_CENTER = new LatLng(2.0024761677439518, 103.41347043314002);
    private static final LatLng MELAKA_CENTER = new LatLng(2.2185081727967173, 102.28548715389067);
    private static final LatLng NEGERI_SEMBILAN_CENTER = new LatLng(2.8478653915486536, 102.18526179798474);
    private static final LatLng PAHANG_CENTER = new LatLng(3.473295505389695, 101.51518230127849);
    private static final LatLng PERAK_CENTER = new LatLng(3.473295505389695, 101.51518230127849);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_search);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }


        editTextSearch = findViewById(R.id.editText);
        btnSearch = findViewById(R.id.search_button);
        cardBottomInfo = findViewById(R.id.cardBottomInfo);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvAddress = findViewById(R.id.tvAddress);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMapSearch(v);
            }
        });


        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goBack(v);
                }
            });
        }


        btnGetDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGoogleMapsForDirections();
            }
        });

        btnSaveLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MapSearchActivity.this, "Location saved to favorites", Toast.LENGTH_SHORT).show();
            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        client = LocationServices.getFusedLocationProviderClient(this);


        Intent intent = getIntent();
        if (intent != null) {
            // Check for selected place first (from PlaceDetailActivity)
            if (intent.hasExtra("SELECTED_PLACE_COORDINATES")) {
                selectedPlaceName = intent.getStringExtra("SELECTED_PLACE_NAME");
                selectedPlaceAddress = intent.getStringExtra("SELECTED_PLACE_ADDRESS");
                String coordinates = intent.getStringExtra("SELECTED_PLACE_COORDINATES");


                try {
                    String[] latLngArray = coordinates.split(",");
                    if (latLngArray.length == 2) {
                        double lat = Double.parseDouble(latLngArray[0].trim());
                        double lng = Double.parseDouble(latLngArray[1].trim());
                        selectedPlaceLocation = new LatLng(lat, lng);

                        if (selectedPlaceName != null) {
                            editTextSearch.setText(selectedPlaceName);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid coordinates format", Toast.LENGTH_SHORT).show();
                }
            }

            else if (intent.hasExtra("negeri")) {
                currentNegeri = intent.getStringExtra("negeri");

                if (currentNegeri != null && !currentNegeri.isEmpty()) {
                    // Auto-search recycling centers in that negeri
                    String searchQuery = "recycling center " + currentNegeri;
                    editTextSearch.setText(searchQuery);
                } else {
                    // Default search if no negeri
                    editTextSearch.setText("recycling center");
                }
            }
        }

        cardBottomInfo.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Check if we have a selected place to show (from PlaceDetailActivity)
        if (selectedPlaceLocation != null) {
            // Show the selected place with bottom card
            showSelectedPlaceOnMap();
        } else if (currentNegeri != null && !currentNegeri.isEmpty()) {
            // Show state center if negeri is provided
            showStateCenter();
        } else {
            // Default to Selangor center
            showStateCenterAtLocation(SELANGOR_CENTER, "Selangor");
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Set map click listener
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Hide bottom card when clicking on empty map area
                hideBottomCard();
            }
        });

        // Set marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            // If this is the selected place marker, show bottom card
            if (selectedPlaceLocation != null &&
                    marker.getPosition().equals(selectedPlaceLocation)) {
                showBottomCard(selectedPlaceName, selectedPlaceAddress);
            }
            return false;
        });

        // If we have negeri (but no selected place), search automatically
        if (currentNegeri != null && !currentNegeri.isEmpty() && selectedPlaceLocation == null) {
            editTextSearch.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openMapSearch(null);
                }
            }, 1500);
        }
    }

    private void showSelectedPlaceOnMap() {
        if (selectedPlaceLocation != null && mMap != null) {
            // Clear map
            mMap.clear();

            // Add marker for selected place
            mMap.addMarker(new MarkerOptions()
                    .position(selectedPlaceLocation)
                    .title(selectedPlaceName)
                    .snippet(selectedPlaceAddress)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Zoom to selected place
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlaceLocation, 16));

            // SHOW BOTTOM CARD
            if (selectedPlaceName != null && !selectedPlaceName.isEmpty()) {
                showBottomCard(selectedPlaceName, selectedPlaceAddress);
            }

            Toast.makeText(this, "Showing: " + selectedPlaceName, Toast.LENGTH_SHORT).show();
        }
    }

    private void showStateCenter() {
        if (currentNegeri == null || mMap == null) return;

        LatLng stateCenter = getStateCenter(currentNegeri);
        String stateName = currentNegeri;

        showStateCenterAtLocation(stateCenter, stateName);
    }

    private void showStateCenterAtLocation(LatLng location, String stateName) {
        mMap.clear();
        hideBottomCard();

        // Add marker for state center
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(stateName + " Center")
                .snippet("Search for recycling centers here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Zoom to state center
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 11));

        Toast.makeText(this, "Showing " + stateName, Toast.LENGTH_SHORT).show();
    }

    private LatLng getStateCenter(String stateName) {
        String state = stateName.toLowerCase();

        if (state.contains("selangor")) {
            return SELANGOR_CENTER;
        } else if (state.contains("johor")) {
            return JOHOR_CENTER;
        } else if (state.contains("melaka")) {
            return MELAKA_CENTER;
        } else if (state.contains("negeri sembilan")) {
            return NEGERI_SEMBILAN_CENTER;
        } else if (state.contains("pahang")) {
            return PAHANG_CENTER;
        } else if (state.contains("perak")) {
            return PERAK_CENTER;
        } else {
            return SELANGOR_CENTER;
        }
    }

    // Search button clicked
    public void openMapSearch(View view) {
        String location = editTextSearch.getText().toString().trim();

        if (location.isEmpty()) {
            if (currentNegeri != null && !currentNegeri.isEmpty()) {
                location = currentNegeri;
            } else {
                location = "Selangor";
            }
        }

        searchRecyclingCenters(location);
        hideBottomCard();
    }

    private void searchRecyclingCenters(String locationName) {
        mMap.clear();
        selectedPlaceLocation = null;

        String searchLower = locationName.toLowerCase();
        if (searchLower.contains("selangor") || searchLower.contains("johor") ||
                searchLower.contains("melaka") || searchLower.contains("negeri sembilan") ||
                searchLower.contains("pahang") || searchLower.contains("perak")) {

            searchWithinState(locationName);
            return;
        }

        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng searchLocation = new LatLng(address.getLatitude(), address.getLongitude());

                mMap.addMarker(new MarkerOptions()
                        .position(searchLocation)
                        .title("Search Area: " + locationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLocation, 12));
                addRecyclingCentersAround(searchLocation);
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoder error", Toast.LENGTH_SHORT).show();
        }

        if (currentLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        Toast.makeText(this, "Searching: " + locationName, Toast.LENGTH_SHORT).show();
    }

    private void searchWithinState(String stateName) {
        String state = stateName.toLowerCase();
        LatLng stateCenter = getStateCenter(stateName);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(stateCenter, 11));

        mMap.addMarker(new MarkerOptions()
                .position(stateCenter)
                .title(stateName.toUpperCase() + " - State Center")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        addRecyclingCentersForState(state);

        if (currentLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }

        Toast.makeText(this, "Showing recycling centers in " + stateName, Toast.LENGTH_SHORT).show();
    }

    private void addRecyclingCentersAround(LatLng center) {
        for (int i = 0; i < 4; i++) {
            double latOffset = (Math.random() - 0.5) * 0.05;
            double lngOffset = (Math.random() - 0.5) * 0.05;

            LatLng centerLocation = new LatLng(
                    center.latitude + latOffset,
                    center.longitude + lngOffset
            );

            mMap.addMarker(new MarkerOptions()
                    .position(centerLocation)
                    .title("Recycling Center " + (i + 1))
                    .snippet("Near " + editTextSearch.getText().toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    private void addRecyclingCentersForState(String state) {
        List<LatLng> centers = getRecyclingCentersForState(state);

        for (int i = 0; i < centers.size(); i++) {
            LatLng center = centers.get(i);
            mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .title("Recycling Center " + (i + 1))
                    .snippet(state.toUpperCase() + " Area")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    private List<LatLng> getRecyclingCentersForState(String state) {
        List<LatLng> centers = new ArrayList<>();
        LatLng stateCenter = getStateCenter(state);

        for (int i = 0; i < 6; i++) {
            double latOffset = (Math.random() - 0.5) * 0.1;
            double lngOffset = (Math.random() - 0.5) * 0.1;

            centers.add(new LatLng(
                    stateCenter.latitude + latOffset,
                    stateCenter.longitude + lngOffset
            ));
        }

        return centers;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if (selectedPlaceLocation == null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("You are here")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                        if (currentNegeri == null || currentNegeri.isEmpty()) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        }
                    }
                } else {
                    Toast.makeText(MapSearchActivity.this,
                            "Enable GPS for better accuracy", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            }
        } else {
            Toast.makeText(this, "Location permission required for GPS", Toast.LENGTH_SHORT).show();
        }
    }

    // Back button
    public void goBack(View view) {
        finish();
    }

    // BOTTOM CARD METHODS
    private void showBottomCard(String placeName, String address) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPlaceName.setText(placeName);
                tvAddress.setText(address);
                cardBottomInfo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideBottomCard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardBottomInfo.setVisibility(View.GONE);
            }
        });
    }

    private void openGoogleMapsForDirections() {
        if (selectedPlaceLocation != null) {
            try {
                String lat = String.valueOf(selectedPlaceLocation.latitude);
                String lng = String.valueOf(selectedPlaceLocation.longitude);

                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, "Google Maps app not installed", Toast.LENGTH_SHORT).show();
                    String url = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error opening directions", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
        }
    }
}