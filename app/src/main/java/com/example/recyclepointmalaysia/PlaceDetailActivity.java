package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PlaceDetailActivity extends AppCompatActivity {

    private PlaceItem placeItem;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // Initialize Firebase Auth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialize views
        TextView placeTitle = findViewById(R.id.textViewPlaceTitle);
        ImageView imageViewPlace = findViewById(R.id.imageViewPlace);
        TextView textViewName = findViewById(R.id.textViewName);
        TextView textViewAddress = findViewById(R.id.textViewAddress);
        TextView textViewContact = findViewById(R.id.textViewContact);
        TextView textViewCoordinates = findViewById(R.id.textViewCoordinates);
        TextView textViewRating = findViewById(R.id.textViewRating);

        // Get place data from intent
        placeItem = (PlaceItem) getIntent().getSerializableExtra("PLACE_ITEM");

        if (placeItem != null) {
            // Set all the data
            placeTitle.setText(placeItem.name);
            textViewName.setText(placeItem.name);
            textViewAddress.setText(placeItem.address);

            // Format phone number
            String phone = placeItem.phone;
            textViewContact.setText(phone);
            textViewContact.setTextColor(Color.BLUE);
            textViewContact.setOnClickListener(v -> {
                if (phone != null) {
                    String cleanNum = phone.replace(" ", "").replace("-", "");
                    Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanNum));
                    startActivity(call);
                }
            });

            // Format coordinates nicely (like in the image)
            String coords = placeItem.coordinates;
            if (coords != null && !coords.isEmpty()) {
                try {
                    String[] parts = coords.split(",");
                    if (parts.length >= 2) {
                        // Format to 6 decimal places
                        String lat = String.format("%.6f", Double.parseDouble(parts[0].trim()));
                        String lng = String.format("%.6f", Double.parseDouble(parts[1].trim()));
                        String formattedCoords = lat + ",\n" + lng;
                        textViewCoordinates.setText("📍 Coordinates\n" + formattedCoords);
                    } else {
                        textViewCoordinates.setText("📍 Coordinates\n" + coords);
                    }
                } catch (Exception e) {
                    textViewCoordinates.setText("📍 Coordinates\n" + coords);
                }
            } else {
                textViewCoordinates.setText("📍 Coordinates\nNot available");
            }

            // Set rating if available
            if (placeItem.rating > 0) {
                textViewRating.setText(placeItem.rating + "/5");
            } else {
                textViewRating.setText("Not available");
            }

            // Set image using Glide
            if (placeItem.photoUrl != null && !placeItem.photoUrl.isEmpty()) {
                try {
                    Glide.with(this)
                            .load(placeItem.photoUrl)
                            .placeholder(R.drawable.ic_recycle_default)
                            .error(R.drawable.ic_recycle_default)
                            .into(imageViewPlace);
                } catch (Exception e) {
                    imageViewPlace.setImageResource(R.drawable.ic_recycle_default);
                }
            } else {
                // Use default image
                imageViewPlace.setImageResource(R.drawable.ic_recycle_default);
            }
        } else {
            Toast.makeText(this, "Error: Location information not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // BACK BUTTON
    public void goBack(View view) {
        finish();
    }

    // OPEN LOCATION ON MAP (Opens MapSearchActivity with bottom card)
    public void openLocationOnMap(View view) {
        if (placeItem != null && placeItem.coordinates != null) {
            try {
                // Parse coordinates
                String[] coords = placeItem.coordinates.split(",");
                if (coords.length == 2) {
                    // Open MapSearchActivity (NOT Google Maps app)
                    Intent intent = new Intent(this, MapSearchActivity.class);

                    // Pass place details to MapSearchActivity
                    intent.putExtra("SELECTED_PLACE_NAME", placeItem.name);
                    intent.putExtra("SELECTED_PLACE_ADDRESS", placeItem.address);
                    intent.putExtra("SELECTED_PLACE_COORDINATES", placeItem.coordinates);

                    if (placeItem.phone != null) {
                        intent.putExtra("SELECTED_PLACE_PHONE", placeItem.phone);
                    }

                    startActivity(intent);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid coordinates format", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Coordinates not available", Toast.LENGTH_SHORT).show();
        }
    }

    // ADD REVIEW BUTTON - Opens ReviewActivity with place info
    public void openReviewActivity(View view) {
        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login first to write a review", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        if (placeItem != null) {
            Intent intent = new Intent(this, ReviewActivity.class);
            // Pass place information to ReviewActivity
            intent.putExtra("PLACE_ID", placeItem.placeId != null ? placeItem.placeId : "");
            intent.putExtra("PLACE_NAME", placeItem.name);
            intent.putExtra("PLACE_ADDRESS", placeItem.address);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Place information not available", Toast.LENGTH_SHORT).show();
        }
    }
}