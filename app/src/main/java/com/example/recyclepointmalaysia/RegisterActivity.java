package com.example.recyclepointmalaysia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    EditText e1, e2;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        e1 = findViewById(R.id.editTextEmail);
        e2 = findViewById(R.id.editTextPassword);
        mAuth = FirebaseAuth.getInstance();
    }

    // Open Login from Register page
    public void openLoginFromRegister(View v){
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Create User Method (called from XML onClick)
    public void createUser(View v){
        String email = e1.getText().toString().trim();
        String password = e2.getText().toString().trim();

        // Validation
        if(email.isEmpty()) {
            e1.setError("Email cannot be empty");
            e1.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            e1.setError("Invalid email address");
            e1.requestFocus();
            return;
        }

        if(password.isEmpty()){
            e2.setError("Password cannot be empty");
            e2.requestFocus();
            return;
        }

        if(password.length() < 6){
            e2.setError("Minimum 6 characters");
            e2.requestFocus();
            return;
        }

        // Firebase create user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(),
                                    "Account created successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
}