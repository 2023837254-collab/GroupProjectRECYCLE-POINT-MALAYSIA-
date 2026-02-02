package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MyReviewsActivity extends AppCompatActivity {

    private ListView listViewReviews;
    private TextView noReviewsText;
    private TextView textViewReviewCount;
    private Button btnAddReview;
    private RadioGroup radioGroupViewType;
    private RadioButton radioMyReviews, radioAllReviews;

    private ArrayList<ReviewItem> reviewList;
    private ArrayAdapter<ReviewItem> adapter;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;

    private boolean showOnlyMyReviews = true; // Default: lihat review sendiri

    // Inner class untuk review item
    public static class ReviewItem {
        String reviewId;
        String placeName;
        String reviewText;
        float rating;
        String date;
        String imageBase64;
        String userId;
        String userEmail; // Untuk display siapa yang buat review

        public ReviewItem(String reviewId, String placeName, String reviewText, float rating,
                          String date, String imageBase64, String userId, String userEmail) {
            this.reviewId = reviewId;
            this.placeName = placeName;
            this.reviewText = reviewText;
            this.rating = rating;
            this.date = date;
            this.imageBase64 = imageBase64;
            this.userId = userId;
            this.userEmail = userEmail;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }

        // Initialize views
        listViewReviews = findViewById(R.id.listViewReviews);
        noReviewsText = findViewById(R.id.noReviewsText);
        textViewReviewCount = findViewById(R.id.textViewReviewCount);
        btnAddReview = findViewById(R.id.btnAddReview);

        // Radio buttons untuk pilihan view
        radioGroupViewType = findViewById(R.id.radioGroupViewType);
        radioMyReviews = findViewById(R.id.radioMyReviews);
        radioAllReviews = findViewById(R.id.radioAllReviews);

        reviewList = new ArrayList<>();

        // Custom adapter dengan user info
        adapter = new ArrayAdapter<ReviewItem>(this, R.layout.list_item_review, R.id.textViewPlaceName, reviewList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ReviewItem item = reviewList.get(position);

                // Get views from layout
                TextView placeNameView = view.findViewById(R.id.textViewPlaceName);
                TextView ratingView = view.findViewById(R.id.textViewRating);
                TextView reviewTextView = view.findViewById(R.id.textViewReviewText);
                TextView dateView = view.findViewById(R.id.textViewDate);
                TextView userView = view.findViewById(R.id.textViewUser); // TextView baru untuk user
                ImageView imageView = view.findViewById(R.id.imageViewReview);

                // Set data
                placeNameView.setText(item.placeName != null ? item.placeName : "Unknown Place");
                ratingView.setText(String.format(Locale.getDefault(), "⭐ %.1f/5", item.rating));
                reviewTextView.setText(item.reviewText != null ? item.reviewText : "");
                dateView.setText(item.date != null ? item.date : "");

                // Show user info jika bukan review sendiri
                if (currentUser != null && item.userId != null && item.userId.equals(currentUser.getUid())) {
                    userView.setText("Your review");
                    userView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                } else {
                    userView.setText("By: " + (item.userEmail != null ?
                            item.userEmail.split("@")[0] : "Anonymous")); // Show username only
                    userView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
                userView.setVisibility(View.VISIBLE);

                // Load image if available
                if (item.imageBase64 != null && !item.imageBase64.isEmpty() && !item.imageBase64.equals("null")) {
                    try {
                        byte[] decodedBytes = Base64.decode(item.imageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        if (bitmap != null) {
                            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                            imageView.setImageBitmap(thumbnail);
                            imageView.setVisibility(View.VISIBLE);
                        } else {
                            imageView.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        imageView.setVisibility(View.GONE);
                    }
                } else {
                    imageView.setVisibility(View.GONE);
                }

                return view;
            }
        };

        listViewReviews.setAdapter(adapter);

        // Set up radio button listeners
        radioGroupViewType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioMyReviews) {
                showOnlyMyReviews = true;
                loadReviews();
            } else if (checkedId == R.id.radioAllReviews) {
                showOnlyMyReviews = false;
                loadReviews();
            }
        });

        // Default pilih my reviews
        radioMyReviews.setChecked(true);

        // Load reviews pertama kali
        loadReviews();

        // Button click listener
        btnAddReview.setOnClickListener(v -> {
            Intent i = new Intent(this, ReviewActivity.class);
            startActivity(i);
        });

        // List item click listener
        listViewReviews.setOnItemClickListener((parent, view, position, id) -> {
            ReviewItem item = reviewList.get(position);
            viewFullReview(item);
        });
    }

    private void loadReviews() {
        // Show loading
        noReviewsText.setText("Loading reviews...");
        noReviewsText.setVisibility(View.VISIBLE);
        listViewReviews.setVisibility(View.GONE);

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                        try {
                            String reviewId = reviewSnap.getKey();
                            String reviewText = reviewSnap.child("reviewText").getValue(String.class);
                            Double rating = reviewSnap.child("rating").getValue(Double.class);
                            String placeName = reviewSnap.child("placeName").getValue(String.class);
                            String imageBase64 = reviewSnap.child("imageBase64").getValue(String.class);
                            String userId = reviewSnap.child("userId").getValue(String.class);
                            String userEmail = reviewSnap.child("userEmail").getValue(String.class);

                            // Get date
                            String date = reviewSnap.child("date").getValue(String.class);
                            if (date == null || date.isEmpty()) {
                                Long timestamp = reviewSnap.child("timestamp").getValue(Long.class);
                                if (timestamp != null) {
                                    date = formatDate(timestamp);
                                }
                            }

                            // Filter jika hanya mau review sendiri
                            if (showOnlyMyReviews) {
                                if (currentUser != null && userId != null &&
                                        !userId.equals(currentUser.getUid())) {
                                    continue; // Skip review orang lain
                                }
                            }

                            if (reviewText != null && rating != null) {
                                ReviewItem item = new ReviewItem(
                                        reviewId,
                                        placeName != null ? placeName : "Unknown Place",
                                        reviewText,
                                        rating.floatValue(),
                                        date != null ? date : "Unknown date",
                                        imageBase64,
                                        userId,
                                        userEmail
                                );
                                reviewList.add(item);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (!reviewList.isEmpty()) {
                        noReviewsText.setVisibility(View.GONE);
                        listViewReviews.setVisibility(View.VISIBLE);
                        String viewType = showOnlyMyReviews ? "Your" : "All";
                        textViewReviewCount.setText(viewType + " reviews: " + reviewList.size());
                    } else {
                        noReviewsText.setText(showOnlyMyReviews ?
                                "You haven't made any reviews yet.\nClick 'ADD NEW REVIEW' to add your first review!" :
                                "No reviews found from any users.");
                        noReviewsText.setVisibility(View.VISIBLE);
                        listViewReviews.setVisibility(View.GONE);
                        textViewReviewCount.setText(showOnlyMyReviews ? "No reviews" : "No reviews found");
                    }

                } else {
                    noReviewsText.setText("No reviews found.");
                    noReviewsText.setVisibility(View.VISIBLE);
                    listViewReviews.setVisibility(View.GONE);
                    textViewReviewCount.setText("No reviews");
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setText("Error loading reviews");
                Toast.makeText(MyReviewsActivity.this, "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        if (showOnlyMyReviews) {
            // Load hanya review user ini
            databaseRef.child("reviews").orderByChild("userId").equalTo(currentUser.getUid())
                    .addValueEventListener(valueEventListener);
        } else {
            // Load semua review
            databaseRef.child("reviews").addValueEventListener(valueEventListener);
        }
    }

    private void viewFullReview(ReviewItem item) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Review Details");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_full_review, null);

        // Initialize dialog views
        TextView dialogPlaceName = dialogView.findViewById(R.id.dialogPlaceName);
        TextView dialogRating = dialogView.findViewById(R.id.dialogRating);
        TextView dialogReviewText = dialogView.findViewById(R.id.dialogReviewText);
        TextView dialogDate = dialogView.findViewById(R.id.dialogDate);
        TextView dialogUser = dialogView.findViewById(R.id.dialogUser); // TextView untuk user
        ImageView dialogImage = dialogView.findViewById(R.id.dialogImage);

        // Set data
        dialogPlaceName.setText(item.placeName);
        dialogRating.setText(String.format(Locale.getDefault(), "Rating: ⭐ %.1f/5", item.rating));
        dialogReviewText.setText(item.reviewText);
        dialogDate.setText("Date: " + item.date);

        // Set user info
        if (currentUser != null && item.userId != null && item.userId.equals(currentUser.getUid())) {
            dialogUser.setText("Your review");
        } else {
            dialogUser.setText("Review by: " + (item.userEmail != null ? item.userEmail : "Anonymous"));
        }
        dialogUser.setVisibility(View.VISIBLE);

        // Load full image
        if (item.imageBase64 != null && !item.imageBase64.isEmpty() && !item.imageBase64.equals("null")) {
            try {
                byte[] decodedBytes = Base64.decode(item.imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    dialogImage.setImageBitmap(bitmap);
                    dialogImage.setVisibility(View.VISIBLE);
                } else {
                    dialogImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                dialogImage.setVisibility(View.GONE);
            }
        } else {
            dialogImage.setVisibility(View.GONE);
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Close", null);

        // Hanya boleh delete jika review milik sendiri
        if (currentUser != null && item.userId != null && item.userId.equals(currentUser.getUid())) {
            builder.setNegativeButton("Delete", (dialog, which) -> {
                deleteReview(item.reviewId);
            });
        } else {
            // Untuk review orang lain, disable atau hide delete button
            builder.setNegativeButton("Report", (dialog, which) -> {
                reportReview(item);
            });
        }

        builder.show();
    }

    private void deleteReview(String reviewId) {
        if (reviewId != null && !reviewId.isEmpty()) {
            databaseRef.child("reviews").child(reviewId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Review deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete review: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void reportReview(ReviewItem item) {
        // Function untuk report review yang tidak pantas
        Toast.makeText(this, "Review reported. Thank you for your feedback.",
                Toast.LENGTH_SHORT).show();
        // Anda bisa tambahkan logic untuk menyimpan report ke database
    }

    private String formatDate(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning
        if (currentUser == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }
        loadReviews();
    }
}