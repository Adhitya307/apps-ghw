package com.apps.bubbletilt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

public class BubbleTiltHomeActivity extends AppCompatActivity {

    private LinearLayout btnInputData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bubble_tilt);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnInputData = findViewById(R.id.btnInputData);
    }

    private void setupClickListeners() {
        btnInputData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Animasi click
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(80)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(80)
                                    .withEndAction(() -> {
                                        // Pindah ke InputDataBubbleTilt activity
                                        goToInputDataActivity();
                                    })
                                    .start();
                        })
                        .start();
            }
        });
    }

    private void goToInputDataActivity() {
        try {
            Intent intent = new Intent(this, InputDataBubbleTilt.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}