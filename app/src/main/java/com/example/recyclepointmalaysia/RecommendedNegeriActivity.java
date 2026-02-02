package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class RecommendedNegeriActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommended_negeri);
    }

    public void openMapSearch(View v) {
        // Buka MapSearchActivity tanpa negeri spesifik
        Intent i = new Intent(this, MapSearchActivity.class);
        startActivity(i);
    }

    public void openSelangor(View view) {
        openNegeriList("Selangor");
    }

    public void openJohor(View view) {
        openNegeriList("Johor");
    }

    public void openMelaka(View view) {
        openNegeriList("Melaka");
    }

    public void openNegeriSembilan(View view) {
        openNegeriList("Negeri Sembilan");
    }

    public void openPahang(View view) {
        openNegeriList("Pahang");
    }

    public void openPerak(View view) {
        openNegeriList("Perak");
    }

    // Common method to open negeri list
    private void openNegeriList(String negeriName) {
        Intent intent = new Intent(this, NegeriListActivity.class);
        intent.putExtra("NEGERI_NAME", negeriName);
        startActivity(intent);
    }
}