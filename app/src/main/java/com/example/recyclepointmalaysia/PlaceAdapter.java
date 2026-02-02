package com.example.recyclepointmalaysia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class PlaceAdapter extends ArrayAdapter<PlaceItem> {

    public PlaceAdapter(Context context, ArrayList<PlaceItem> places) {
        super(context, 0, places);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PlaceItem place = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
        }

        // Initialize views
        TextView nameTextView = convertView.findViewById(R.id.textViewPlaceName);
        TextView addressTextView = convertView.findViewById(R.id.textViewPlaceAddress);
        TextView phoneTextView = convertView.findViewById(R.id.textViewPlacePhone);
        TextView ratingTextView = convertView.findViewById(R.id.textViewPlaceRating);
        ImageView placeImageView = convertView.findViewById(R.id.imageViewPlace);

        // Set text values
        nameTextView.setText(place.getName());
        addressTextView.setText(place.getAddress());
        phoneTextView.setText("Phone: " + place.getPhone());
        ratingTextView.setText("Rating: " + place.getRating() + "/5");

        // Load image from URL if available
        if (place.getPhotoUrl() != null && !place.getPhotoUrl().isEmpty()) {
            new LoadImageTask(placeImageView).execute(place.getPhotoUrl());
        } else {
            // Use default image if no photo available
            placeImageView.setImageResource(android.R.drawable.ic_menu_upload);
        }

        return convertView;
    }

    // AsyncTask to load image from URL
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}