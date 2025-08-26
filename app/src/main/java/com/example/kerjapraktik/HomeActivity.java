package com.example.kerjapraktik;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout btnInput, btnInputHP2, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inisialisasi tombol
        btnInput = findViewById(R.id.btnInput);       // Input HP 1
        btnInputHP2 = findViewById(R.id.btnInputHP2); // Input HP 2
        btnHistory = findViewById(R.id.btnHistory);   // Data History

        // Tombol untuk HP 1 (input pengukuran dan lainnya)
        btnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, InputDataActivity.class);
                intent.putExtra("device_mode", "hp1"); // kirim mode (opsional)
                startActivity(intent);
            }
        });

        // Tombol untuk HP 2 (memilih pengukuran ID & input SR/Bocoran)
        btnInputHP2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, InputData2Activity.class);
                intent.putExtra("device_mode", "hp2"); // opsional
                startActivity(intent);
            }
        });

        // Tombol untuk Data History
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }
}