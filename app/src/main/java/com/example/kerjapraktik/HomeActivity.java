package com.example.kerjapraktik;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnInput = findViewById(R.id.btnInput);

        btnInput.setOnClickListener(v -> {
            // Ganti dengan activity form input
            Intent intent = new Intent(HomeActivity.this, InputDataActivity.class);
            startActivity(intent);
        });
    }
}
