package com.example.app_dambody;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DamBodyHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dambody);

        // PERBAIKAN: Gunakan LinearLayout dan tambahkan final
        final LinearLayout btnELV625 = findViewById(R.id.btnELV625);
        final LinearLayout btnELV600 = findViewById(R.id.btnELV600);
        final LinearLayout btnHistory = findViewById(R.id.btnHistory);

        // Kasih aksi ketika button diklik dengan lambda
        btnELV625.setOnClickListener(v -> {
            Toast.makeText(DamBodyHomeActivity.this, "Membuka ELV 625", Toast.LENGTH_SHORT).show();
            // Buka activity InputdataElv625
            Intent intent = new Intent(DamBodyHomeActivity.this, InputdataElv625.class);
            startActivity(intent);
        });

        btnELV600.setOnClickListener(v -> {
            Toast.makeText(DamBodyHomeActivity.this, "Membuka ELV 600", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DamBodyHomeActivity.this, InputdataElv600.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v ->
                Toast.makeText(DamBodyHomeActivity.this, "Membuka Data History", Toast.LENGTH_SHORT).show()
        );
    }
}