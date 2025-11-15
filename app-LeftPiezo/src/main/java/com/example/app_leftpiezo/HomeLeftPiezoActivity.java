package com.example.app_leftpiezo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HomeLeftPiezoActivity extends AppCompatActivity {

    private LinearLayout btnInputData, btnHistory;
    private static final String TAG = "HomeLeftPiezoActivity";
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_leftpiezo);

        initViews();
        setupClickListeners();

        // Inisialisasi sederhana dengan delay
        handler.postDelayed(() -> {
            Log.d(TAG, "Aplikasi Piezometer Sandaran Bendungan Kiri siap digunakan");
            Toast.makeText(this, "Piezometer Ready", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void initViews() {
        // Menggunakan LinearLayout yang ada di dalam CardView
        btnInputData = findViewById(R.id.btnInputData);
        btnHistory = findViewById(R.id.btnHistory);
    }

    private void setupClickListeners() {
        // Input Data Button
        btnInputData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v, () -> goToInputDataActivity());
            }
        });

/**        // History Button
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v, () -> goToHistoryActivity());
            }
        });

 */
    }


    private void animateButtonClick(View view, Runnable action) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(80)
                            .withEndAction(action)
                            .start();
                })
                .start();
    }

    // ==================== NAVIGATION METHODS ====================

    private void goToInputDataActivity() {
        try {
            // Ganti dengan class activity input data yang sesuai
            Intent intent = new Intent(this, InputDataLeftPiezo.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error membuka Input Data Piezometer: " + e.getMessage());
            Toast.makeText(this, "Error membuka Input Data", Toast.LENGTH_SHORT).show();
        }
    }

/**    private void goToHistoryActivity() {
        try {
            // Ganti dengan class activity history yang sesuai
            Intent intent = new Intent(this, HistoryLeftPiezo.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error membuka History Piezometer: " + e.getMessage());
            Toast.makeText(this, "Error membuka History", Toast.LENGTH_SHORT).show();
        }
    }
 */


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}