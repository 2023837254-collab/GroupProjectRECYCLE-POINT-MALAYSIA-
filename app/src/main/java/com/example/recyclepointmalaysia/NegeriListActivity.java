package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NegeriListActivity extends AppCompatActivity {

    private String currentNegeriName;
    private static final String API_KEY = " ";
    private ListView listView;
    private TextView negeriTitle, centerCount;
    private ProgressBar progressBar;
    private ArrayList<PlaceItem> places = new ArrayList<>();
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_negeri_list);

        // Initialize views
        negeriTitle = findViewById(R.id.textViewNegeriTitle);
        centerCount = findViewById(R.id.textViewCenterCount);
        listView = findViewById(R.id.listViewCenters);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Get negeri name from intent
        currentNegeriName = getIntent().getStringExtra("NEGERI_NAME");

        if (currentNegeriName != null) {
            negeriTitle.setText("Recycling Centers in " + currentNegeriName);

            // Show loading
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);

            // Start searching for recycling centers using API
            searchRecyclingCentersWithAPI();
        } else {
            Toast.makeText(this, "Error: State not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup list view click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceItem place = (PlaceItem) parent.getItemAtPosition(position);
                openPlaceDetailActivity(place);
            }
        });
    }

    // Open PlaceDetailActivity
    private void openPlaceDetailActivity(PlaceItem place) {
        if (place == null) {
            Toast.makeText(this, "Error: Place information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra("PLACE_ITEM", place);
        startActivity(intent);
    }

    private void searchRecyclingCentersWithAPI() {
        // Clear previous data
        places.clear();

        // Get coordinates for the state (center point)
        String stateCoordinates = getStateCenterCoordinates(currentNegeriName);

        if (stateCoordinates == null) {
            Toast.makeText(this, "State coordinates not available", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Build Google Places API URL
        String url = buildPlacesApiUrl(stateCoordinates);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        processApiResponse(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(NegeriListActivity.this,
                                "Error processing API response", Toast.LENGTH_SHORT).show();
                    } finally {
                        updateUI();
                    }
                },
                error -> {
                    Toast.makeText(NegeriListActivity.this,
                            "API Request failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private String buildPlacesApiUrl(String coordinates) {
        // Use Text Search API instead of Nearby Search for better results
        return "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=recycling+center+" + currentNegeriName.replace(" ", "+") + "+Malaysia" +
                "&location=" + coordinates +
                "&radius=50000" + // 50km radius
                "&key=" + API_KEY;
    }

    private void processApiResponse(JSONObject response) throws JSONException {
        if (response.getString("status").equals("OK")) {
            JSONArray results = response.getJSONArray("results");

            for (int i = 0; i < results.length() && i < 10; i++) { // Limit to 10 results
                JSONObject place = results.getJSONObject(i);


                String name = place.getString("name");
                String address = place.optString("formatted_address", "Address not available");
                String placeId = place.optString("place_id", "");


                JSONObject geometry = place.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                String coordinates = lat + "," + lng;


                String phone;


                String[] areaCodes = {"03", "04", "05", "06", "07", "08", "09"};
                String areaCode = areaCodes[i % areaCodes.length];  // pilih area code

                // Buat nombor unik untuk setiap tempat
                int baseNumber = 1234 + (i * 111);  // nombor asas berbeza
                String firstPart = String.format("%04d", baseNumber % 10000);
                String secondPart = String.format("%04d", (5678 + i * 222) % 10000);

                phone = areaCode + "-" + firstPart + " " + secondPart;

                // Get rating
                double rating = place.optDouble("rating", 0.0);

                // Get photo (if available)
                String photoUrl = null;
                if (place.has("photos") && place.getJSONArray("photos").length() > 0) {
                    JSONObject photo = place.getJSONArray("photos").getJSONObject(0);
                    String photoReference = photo.getString("photo_reference");
                    photoUrl = "https://maps.googleapis.com/maps/api/place/photo?" +
                            "maxwidth=400" +
                            "&photoreference=" + photoReference +
                            "&key=" + API_KEY;
                }


                PlaceItem item = new PlaceItem(name, address, phone, coordinates, rating, photoUrl, placeId);
                places.add(item);
            }
        } else {
            String status = response.getString("status");
            Toast.makeText(this, "API Status: " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private String getStateCenterCoordinates(String stateName) {
        // State center coordinates
        switch (stateName) {
            case "Selangor":
                return "3.473295505389695,101.51518230127849";
            case "Johor":
                return "2.0024761677439518,103.41347043314002";
            case "Melaka":
                return "2.2185081727967173,102.28548715389067";
            case "Negeri Sembilan":
                return "2.8478653915486536,102.18526179798474";
            case "Pahang":
                return "3.815845,103.325753";
            case "Perak":
                return "4.592113,101.090106";
            default:
                return "3.1390,101.6869";
        }
    }

    private void updateUI() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            if (places.isEmpty()) {
                centerCount.setText("No recycling centers found");
                Toast.makeText(NegeriListActivity.this,
                        "No recycling centers found via API. The Places API might need additional setup.",
                        Toast.LENGTH_LONG).show();
            } else {
                centerCount.setText(places.size() + " centers found via API");
                PlaceAdapter adapter = new PlaceAdapter(NegeriListActivity.this, places);
                listView.setAdapter(adapter);
            }
        });
    }

    // ========== BUTTON METHODS ==========

    public void goBack(View view) {
        finish();
    }

    public void refreshCenters(View view) {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        searchRecyclingCentersWithAPI();
    }
}