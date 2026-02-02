package com.example.recyclepointmalaysia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    // UI Components
    private ImageView imageViewPreview;
    private Button btnTakePhoto, btnSelectPhoto, btnSubmit;
    private EditText editTextReview;
    private RatingBar ratingBar;
    private TextView textViewPlaceName;

    // Request Codes
    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    // Notification
    private static final String CHANNEL_ID = "review_channel";
    private static final int NOTIFICATION_ID = 1;

    // Variables
    private Bitmap selectedImage;
    private String placeId, placeName, placeAddress;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // Create notification channel
        createNotificationChannel();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get place information from intent
        Intent intent = getIntent();
        if (intent != null) {
            placeId = intent.getStringExtra("PLACE_ID");
            placeName = intent.getStringExtra("PLACE_NAME");
            placeAddress = intent.getStringExtra("PLACE_ADDRESS");

            // Optional: Show place name in title bar
            if (placeName != null && !placeName.isEmpty()) {
                setTitle("Review: " + placeName);
            }
        }

        // Initialize views
        initViews();

        // Setup click listeners
        setupClickListeners();
    }

    private void initViews() {
        imageViewPreview = findViewById(R.id.imageViewPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnSubmit = findViewById(R.id.btnSubmit);
        editTextReview = findViewById(R.id.editTextReview);
        ratingBar = findViewById(R.id.ratingBar);
        textViewPlaceName = findViewById(R.id.textViewPlaceName);

        // Set place name if available
        if (placeName != null && !placeName.isEmpty() && textViewPlaceName != null) {
            textViewPlaceName.setText("Review for: " + placeName);
        }
    }

    private void setupClickListeners() {
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnSelectPhoto.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    // ============ NOTIFICATION ============
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Review Notifications";
            String description = "Notifications for review submissions";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String placeName, float rating) {
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Review Submitted!")
                .setContentText("Thank you for reviewing " + placeName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // For Android 8.0 and above, add more details
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("Your review for " + placeName + " has been submitted successfully!\n" +
                            "Rating: " + rating + "/5 stars\n" +
                            "Thank you for helping others find the best recycling centers!"));
        }

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    // ============ CAMERA ============
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    // ============ GALLERY ============
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    // ============ CONVERT BITMAP TO BASE64 ============
    private String convertBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";

        // Resize image to reduce size
        Bitmap resizedBitmap = resizeBitmap(bitmap, 800);

        // Compress image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);

        // Convert to Base64
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = (float) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else {
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    // ============ SUBMIT REVIEW ============
    private void submitReview() {
        String reviewText = editTextReview.getText().toString().trim();
        float rating = ratingBar.getRating();

        // Validate input
        if (TextUtils.isEmpty(reviewText)) {
            editTextReview.setError("Please write a review");
            editTextReview.requestFocus();
            return;
        }

        if (rating == 0) {
            Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show();
            ratingBar.requestFocus();
            return;
        }

        // Disable button to prevent multiple submissions
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        // Save review to Firebase
        saveReviewToFirebase(reviewText, rating);
    }

    private void saveReviewToFirebase(String reviewText, float rating) {
        try {
            // Convert image to Base64 (if exists)
            String imageBase64 = "";
            if (selectedImage != null) {
                imageBase64 = convertBitmapToBase64(selectedImage);
                Log.d("ReviewActivity", "Image Base64 length: " + imageBase64.length());
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            }

            // Create review ID
            String reviewId = databaseRef.child("reviews").push().getKey();

            if (reviewId == null) {
                Toast.makeText(this, "Failed to create review", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Review");
                return;
            }

            // Create current date/time
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());

            // Create review data map
            Map<String, Object> review = new HashMap<>();
            review.put("reviewId", reviewId);
            review.put("userId", currentUser.getUid());
            review.put("userEmail", currentUser.getEmail());
            review.put("placeId", placeId != null ? placeId : "");
            review.put("placeName", placeName != null ? placeName : "Unknown Place");
            review.put("placeAddress", placeAddress != null ? placeAddress : "");
            review.put("reviewText", reviewText);
            review.put("rating", rating);
            review.put("imageBase64", imageBase64);
            review.put("timestamp", System.currentTimeMillis());
            review.put("date", currentDateTime);

            // Save to Firebase Realtime Database
            databaseRef.child("reviews").child(reviewId).setValue(review)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ReviewActivity.this,
                                    "Review submitted successfully!", Toast.LENGTH_SHORT).show();

                            // Show notification
                            showNotification(placeName != null ? placeName : "the place", rating);

                            // Go back to previous activity
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ReviewActivity.this,
                                    "Failed to save review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Submit Review");
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Review");
        }
    }

    // ============ PERMISSION HANDLING ============
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ============ ACTIVITY RESULT ============
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                // Get image from camera
                selectedImage = (Bitmap) data.getExtras().get("data");
                if (selectedImage != null) {
                    imageViewPreview.setImageBitmap(selectedImage);
                    imageViewPreview.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
                }

            } else if (requestCode == GALLERY_REQUEST && data != null) {
                // Get image from gallery
                Uri imageUri = data.getData();
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    if (selectedImage != null) {
                        imageViewPreview.setImageBitmap(selectedImage);
                        imageViewPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}