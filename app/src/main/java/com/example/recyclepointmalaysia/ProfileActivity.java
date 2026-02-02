package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;
    TextView textViewWelcome, textViewEmail;
    Button btnSetReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();

        // Initialize views
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textViewEmail = findViewById(R.id.textViewEmail);
        btnSetReminders = findViewById(R.id.btnSetReminders);

        user = auth.getCurrentUser();
        if (user != null) {
            // Set welcome text
            String welcomeMessage = "Welcome back!";
            textViewWelcome.setText(welcomeMessage);

            // Set user email
            if (user.getEmail() != null) {
                textViewEmail.setText(user.getEmail());
            } else {
                textViewEmail.setText("User");
            }
        } else {
            // If user is not logged in, redirect to MainActivity
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }

        // Set click listener for SET REMINDERS button
        btnSetReminders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReminders(v);
            }
        });
    }

    // ========== BUTTON METHODS ==========

    // 1. FIND RECYCLING CENTERS Button
    public void openRecommended(View v) {
        Intent i = new Intent(this, RecommendedNegeriActivity.class);
        startActivity(i);
    }

    // 2. SET REMINDERS Button
    public void setReminders(View v) {
        Intent i = new Intent(this, setremain.class);
        startActivity(i);
    }

    // 3. EDIT PROFILE Button (NEW)
    public void editProfile(View v) {
        // Navigate to ProfileUserActivity for editing profile/password
        Intent i = new Intent(this, ProfileUserActivity.class);
        startActivity(i);
    }

    // 4. VIEW MY REVIEWS Button
    public void viewMyReviews(View v) {
        Intent i = new Intent(this, MyReviewsActivity.class);
        startActivity(i);
    }

    // 5. LOG OUT Button
    public void signout(View v) {
        auth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check authentication when returning to profile
        user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }
    }
}