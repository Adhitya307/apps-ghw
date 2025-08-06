package com.example.kerjapraktik;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnInput, btnInputHP2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inisialisasi tombol
        btnInput = findViewById(R.id.btnInput);       // Input HP 1
        btnInputHP2 = findViewById(R.id.btnInputHP2); // Input HP 2

        // Tombol untuk HP 1 (input pengukuran dan lainnya)
        btnInput.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputDataActivity.class);
            intent.putExtra("device_mode", "hp1"); // kirim mode (opsional)
            startActivity(intent);
        });

// Tombol untuk HP 2 (memilih pengukuran ID & input SR/Bocoran)
        btnInputHP2.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputData2Activity.class); // ← Ganti ke activity yang benar
            intent.putExtra("device_mode", "hp2"); // opsional
            startActivity(intent);
        });
    }
}
