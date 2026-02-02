package com.example.recyclepointmalaysia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class setremain extends AppCompatActivity {

    private TextView textTitle, textStatus, textTime;
    private EditText editMessage;
    private Button btnSetTime, btnSave, btnCancel;
    private CardView cardStatus;

    private Calendar selectedTime;
    private SimpleDateFormat timeFormat;
    private static final int ALARM_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setremain);

        // Initialize views
        textTitle = findViewById(R.id.textTitle);
        textStatus = findViewById(R.id.textStatus);
        textTime = findViewById(R.id.textTime);
        editMessage = findViewById(R.id.editMessage);
        btnSetTime = findViewById(R.id.btnSetTime);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        cardStatus = findViewById(R.id.cardStatus);

        timeFormat = new SimpleDateFormat("hh:mm a, EEE", Locale.getDefault());

        // Set default message
        editMessage.setText("Don't forget to recycle today!");

        // Button listeners
        btnSetTime.setOnClickListener(v -> openTimePicker());

        btnSave.setOnClickListener(v -> {
            if (selectedTime == null) {
                Toast.makeText(this, "Please select time first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (editMessage.getText().toString().trim().isEmpty()) {
                editMessage.setError("Enter reminder message");
                return;
            }
            setReminder();
        });

        btnCancel.setOnClickListener(v -> finish());

        // Initial status
        updateStatus("No reminder set", "#9E9E9E");
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hour);
                    selectedTime.set(Calendar.MINUTE, minute);
                    selectedTime.set(Calendar.SECOND, 0);

                    // If time passed, set for tomorrow
                    if (selectedTime.getTimeInMillis() <= System.currentTimeMillis()) {
                        selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    textTime.setText("Selected: " + timeFormat.format(selectedTime.getTime()));
                    textTime.setTextColor(Color.parseColor("#4CAF50"));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );
        dialog.setTitle("Select Reminder Time");
        dialog.show();
    }

    private void setReminder() {
        String message = editMessage.getText().toString().trim();
        String timeStr = timeFormat.format(selectedTime.getTime());

        // Update UI
        updateStatus("✅ Reminder set for " + timeStr, "#4CAF50");

        // Set alarm
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    selectedTime.getTimeInMillis(),
                    pendingIntent
            );
        }

        Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String status, String color) {
        textStatus.setText(status);
        textStatus.setTextColor(Color.parseColor(color));
        cardStatus.setCardBackgroundColor(Color.parseColor(color + "20")); // 20 = 12% opacity
    }
}