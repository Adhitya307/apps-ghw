package com.example.app_leftpiezo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeLeftPiezoActivity extends AppCompatActivity {

    private LinearLayout btnInputData, btnHistory;
    private ProgressBar syncProgressBar;
    private TextView syncStatusText, syncProgressText;
    private static final String TAG = "HomeLeftPiezoActivity";

    private Handler handler = new Handler();
    private ApiService apiService;
    private LeftPiezoDatabaseHelper dbHelper;

    // Sync Management - 5 tabel untuk Left Piezo
    private AtomicInteger pendingSyncOperations = new AtomicInteger(0);
    private int totalSyncOperations = 6;
    private int successCount = 0;
    private int failureCount = 0;

    private static final String BASE_URL = "http://192.168.1.12/GHW/api-apps/public/api/leftpiezo/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_leftpiezo);

        initViews();
        setupDatabase();
        setupApiService();
        setupClickListeners();

        // Start auto-sync ketika aplikasi dibuka
        startAutoSync();
    }

    private void initViews() {
        btnInputData = findViewById(R.id.btnInputData);
        btnHistory = findViewById(R.id.btnHistory);


        // Initialize progress UI
        if (syncProgressBar != null) {
            syncProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void setupDatabase() {
        dbHelper = new LeftPiezoDatabaseHelper(this);
    }

    private void setupApiService() {
        apiService = ApiClient.getClient(BASE_URL).create(ApiService.class);
        testApiConnection();
    }

    private void testApiConnection() {
        Log.d(TAG, "🔗 Testing API Connection to: " + BASE_URL);

        Call<ApiResponse<Object>> call = apiService.getHealthStatus();
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✅ API Connection Test: SUCCESS");
                } else {
                    Log.e(TAG, "❌ API Connection Test: FAILED - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "❌ API Connection Test: NETWORK ERROR - " + t.getMessage());
            }
        });
    }

    private void setupClickListeners() {
        // Input Data Button
        btnInputData.setOnClickListener(v -> {
            animateButtonClick(v, this::goToInputDataActivity);
        });

        // History Button
        btnHistory.setOnClickListener(v -> {
            animateButtonClick(v, this::goToHistoryActivity);
        });
    }

    // ==================== AUTO-SYNC SYSTEM ====================

    private void startAutoSync() {
        Log.d(TAG, "🚀 Memulai Auto-Sync System Left Piezometer");

        // Reset counters
        successCount = 0;
        failureCount = 0;
        pendingSyncOperations.set(totalSyncOperations);

        // Update UI
        updateSyncStatus("Memulai sinkronisasi...", 0);

        // Sequential sync dengan delay bertahap
        startSequentialSync();
    }

    private void startManualSync() {
        Toast.makeText(this, "Memulai sinkronisasi manual...", Toast.LENGTH_SHORT).show();
        startAutoSync();
    }

    private void startSequentialSync() {
        Log.d(TAG, "🔄 Memulai sequential sync untuk " + totalSyncOperations + " tabel");

        // Phase 1: Master Data (100ms delay)
        syncWithDelay(this::syncPengukuran, 100);

        // Phase 2: Reading Data (300ms delay)
        syncWithDelay(this::syncIReadingA, 300);
        syncWithDelay(this::syncIReadingB, 300);
        syncWithDelay(this::syncTPembacaan, 300);

        // Phase 3: Calculation Data (600ms delay)
        syncWithDelay(this::syncBPiezoMetrik, 600);
        syncWithDelay(this::syncPerhitunganLeftPiez, 600);
    }

    private void syncWithDelay(Runnable syncTask, long delay) {
        handler.postDelayed(syncTask, delay);
    }

    // ==================== SYNC METHODS ====================

    private void syncPengukuran() {
        Log.d(TAG, "📊 Sync: T_Pengukuran_Leftpiez");

        Call<ApiResponse<List<T_pengukuran_leftpiez>>> call = apiService.getPengukuranLeftPiez();
        call.enqueue(new Callback<ApiResponse<List<T_pengukuran_leftpiez>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<T_pengukuran_leftpiez>>> call, Response<ApiResponse<List<T_pengukuran_leftpiez>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<T_pengukuran_leftpiez> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncPengukuranData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync T_Pengukuran berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync T_Pengukuran gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync T_Pengukuran: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing T_Pengukuran: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync T_Pengukuran gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<T_pengukuran_leftpiez>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error T_Pengukuran: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncTPembacaan() {
        Log.d(TAG, "📊 Sync: T_Pembacaan");

        Call<ApiResponse<List<TPembacaanLeftPiez>>> call = apiService.getTPembacaan();
        call.enqueue(new Callback<ApiResponse<List<TPembacaanLeftPiez>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TPembacaanLeftPiez>>> call, Response<ApiResponse<List<TPembacaanLeftPiez>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<TPembacaanLeftPiez> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncTPembacaanData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync T_Pembacaan berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync T_Pembacaan gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync T_Pembacaan: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing T_Pembacaan: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync T_Pembacaan gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TPembacaanLeftPiez>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error T_Pembacaan: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncIReadingA() {
        Log.d(TAG, "📊 Sync: I_Reading_A");

        Call<ApiResponse<List<I_reading_a>>> call = apiService.getIReadingA();
        call.enqueue(new Callback<ApiResponse<List<I_reading_a>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<I_reading_a>>> call, Response<ApiResponse<List<I_reading_a>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<I_reading_a> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncIReadingAData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync I_Reading_A berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync I_Reading_A gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync I_Reading_A: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing I_Reading_A: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync I_Reading_A gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<I_reading_a>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error I_Reading_A: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncIReadingB() {
        Log.d(TAG, "📊 Sync: I_Reading_B");

        Call<ApiResponse<List<I_reading_b>>> call = apiService.getIReadingB();
        call.enqueue(new Callback<ApiResponse<List<I_reading_b>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<I_reading_b>>> call, Response<ApiResponse<List<I_reading_b>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<I_reading_b> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncIReadingBData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync I_Reading_B berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync I_Reading_B gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync I_Reading_B: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing I_Reading_B: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync I_Reading_B gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<I_reading_b>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error I_Reading_B: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncBPiezoMetrik() {
        Log.d(TAG, "📊 Sync: B_Piezo_Metrik");

        Call<ApiResponse<List<B_piezo_metrik>>> call = apiService.getBPiezoMetrik();
        call.enqueue(new Callback<ApiResponse<List<B_piezo_metrik>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<B_piezo_metrik>>> call, Response<ApiResponse<List<B_piezo_metrik>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<B_piezo_metrik> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncBPiezoMetrikData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync B_Piezo_Metrik berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync B_Piezo_Metrik gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync B_Piezo_Metrik: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing B_Piezo_Metrik: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync B_Piezo_Metrik gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<B_piezo_metrik>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error B_Piezo_Metrik: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganLeftPiez() {
        Log.d(TAG, "📊 Sync: Perhitungan_Left_Piez");

        Call<ApiResponse<List<Perhitungan_left_piez>>> call = apiService.getPerhitunganLeftPiez();
        call.enqueue(new Callback<ApiResponse<List<Perhitungan_left_piez>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Perhitungan_left_piez>>> call, Response<ApiResponse<List<Perhitungan_left_piez>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<Perhitungan_left_piez> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncPerhitunganLeftPiezData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync Perhitungan_Left_Piez berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync Perhitungan_Left_Piez gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync Perhitungan_Left_Piez: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing Perhitungan_Left_Piez: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync Perhitungan_Left_Piez gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Perhitungan_left_piez>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error Perhitungan_Left_Piez: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    // ==================== SYNC COMPLETION HANDLER ====================

    private void checkSyncCompletion() {
        int completed = totalSyncOperations - pendingSyncOperations.decrementAndGet();
        int progress = (int) ((completed / (float) totalSyncOperations) * 100);

        Log.d(TAG, "📊 Progress: " + completed + "/" + totalSyncOperations +
                " | Success: " + successCount + " | Failed: " + failureCount);

        // Update UI
        updateSyncStatus("Sinkronisasi: " + completed + "/" + totalSyncOperations, progress);

        // Check if all operations completed
        if (completed >= totalSyncOperations) {
            onSyncComplete();
        }
    }

    private void updateSyncStatus(String message, int progress) {
        runOnUiThread(() -> {
            if (syncProgressBar != null) {
                syncProgressBar.setProgress(progress);
            }
            if (syncStatusText != null) {
                syncStatusText.setText(message);
            }
            if (syncProgressText != null) {
                syncProgressText.setText(progress + "%");
            }
        });
    }

    private void onSyncComplete() {
        runOnUiThread(() -> {
            String statusMessage;

            if (failureCount == 0) {
                statusMessage = "✅ Sync berhasil! " + successCount + " tabel terupdate";
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
            } else if (successCount > 0) {
                statusMessage = "⚠️ Sync partial: " + successCount + " berhasil, " + failureCount + " gagal";
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
            } else {
                statusMessage = "❌ Sync gagal! Periksa koneksi internet";
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show();
            }

            updateSyncStatus(statusMessage, 100);

            // Auto-hide progress after 3 seconds
            handler.postDelayed(() -> {
                if (syncProgressBar != null) {
                    syncProgressBar.setVisibility(View.GONE);
                }
                if (syncStatusText != null) {
                    syncStatusText.setText("Aplikasi siap digunakan");
                }
                if (syncProgressText != null) {
                    syncProgressText.setText("100%");
                }
            }, 3000);

            // Log final sync statistics
            logSyncStatistics();
        });
    }

    private void logSyncStatistics() {
        Log.d(TAG, "📈 FINAL SYNC STATISTICS:");
        Log.d(TAG, "📈 Total Operations: " + totalSyncOperations);
        Log.d(TAG, "📈 Success: " + successCount);
        Log.d(TAG, "📈 Failed: " + failureCount);
        Log.d(TAG, "📈 Database Counts - Pengukuran: " + dbHelper.getPengukuranCount());
        Log.d(TAG, "📈 Database Counts - I_Reading_A: " + dbHelper.getIReadingACount());
        Log.d(TAG, "📈 Database Counts - I_Reading_B: " + dbHelper.getIReadingBCount());
        Log.d(TAG, "📈 Database Counts - B_Piezo_Metrik: " + dbHelper.getBPiezoMetrikCount());
        Log.d(TAG, "📈 Database Counts - Perhitungan_Left_Piez: " + dbHelper.getPerhitunganLeftPiezCount());
    }

    // ==================== UI ANIMATIONS ====================

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
            Intent intent = new Intent(this, InputDataLeftPiezo.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error membuka Input Data Piezometer: " + e.getMessage());
            Toast.makeText(this, "Error membuka Input Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHistoryActivity() {
        try {
            Intent intent = new Intent(this, HistoryLeftPiezoActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error membuka History Piezometer: " + e.getMessage());
            Toast.makeText(this, "Error membuka History", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== LIFECYCLE MANAGEMENT ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler dan database
        handler.removeCallbacksAndMessages(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}