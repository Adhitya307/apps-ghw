package com.example.app_rightpiezo;

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

import com.example.app_rightpiezo.HistoryRightPiezoActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRightPiezoActivity extends AppCompatActivity {

    private LinearLayout btnInputData, btnHistory;
    private ProgressBar syncProgressBar;
    private TextView syncStatusText, syncProgressText;
    private static final String TAG = "HomeRightPiezoActivity";

    private Handler handler = new Handler();
    private ApiService apiService;
    private RightPiezoDatabaseHelper dbHelper;

    // Sync Management - DIPERBAIKI: Hanya 5 tabel (tanpa Elevasi Dasar)
    private AtomicInteger pendingSyncOperations = new AtomicInteger(0);
    private int totalSyncOperations = 5; // DIPERBAIKI: dari 6 jadi 5 tabel
    private int successCount = 0;
    private int failureCount = 0;

    private static final String BASE_URL = "http://10.73.69.30/GHW/api-apps/public/api/rightpiezo/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_rightpiezo);

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

    }

    private void setupDatabase() {
        dbHelper = new RightPiezoDatabaseHelper(this);
    }

    private void setupApiService() {
        // ✅ ✅ ✅ PERBAIKI INI: Gunakan BASE_URL yang sudah didefinisikan
        apiService = ApiClient.getClient(BASE_URL).create(ApiService.class);

        // ✅ OPTIONAL: Tambahkan test koneksi
        testApiConnection();
    }
    // ✅ METHOD UNTUK TEST KONEKSI
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
        Log.d(TAG, "🚀 Memulai Auto-Sync System Right Piezometer");

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
        syncWithDelay(this::syncIReadingAtas, 300);
        syncWithDelay(this::syncTPembacaan, 300);

        // Phase 3: Calculation Data (600ms delay) - DIPERBAIKI: tanpa Elevasi Dasar
        syncWithDelay(this::syncBPiezoMetrik, 600);
        syncWithDelay(this::syncPerhitunganPsMetrik, 600);
        // DIPERBAIKI: syncElevasiDasar dihapus
    }

    private void syncWithDelay(Runnable syncTask, long delay) {
        handler.postDelayed(syncTask, delay);
    }

    // ==================== SYNC METHODS ====================

    private void syncPengukuran() {
        Log.d(TAG, "📊 Sync: T_Pengukuran_RightPiez");

        // DIPERBAIKI: Menggunakan List untuk response
        Call<ApiResponse<List<T_pengukuran_rightpiez>>> call = apiService.getPengukuranRightPiez();
        call.enqueue(new Callback<ApiResponse<List<T_pengukuran_rightpiez>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<T_pengukuran_rightpiez>>> call, Response<ApiResponse<List<T_pengukuran_rightpiez>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<T_pengukuran_rightpiez> dataList = response.body().getData();
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
            public void onFailure(Call<ApiResponse<List<T_pengukuran_rightpiez>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error T_Pengukuran: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncIReadingAtas() {
        Log.d(TAG, "📊 Sync: I_Reading_Atas");

        // DIPERBAIKI: Menggunakan List untuk response
        Call<ApiResponse<List<I_reading_atas>>> call = apiService.getIReadingAtas();
        call.enqueue(new Callback<ApiResponse<List<I_reading_atas>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<I_reading_atas>>> call, Response<ApiResponse<List<I_reading_atas>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<I_reading_atas> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncIReadingAtasData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync I_Reading_Atas berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync I_Reading_Atas gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync I_Reading_Atas: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing I_Reading_Atas: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync I_Reading_Atas gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<I_reading_atas>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error I_Reading_Atas: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncTPembacaan() {
        Log.d(TAG, "📊 Sync: T_Pembacaan");

        // DIPERBAIKI: Menggunakan List untuk response
        Call<ApiResponse<List<T_pembacaan>>> call = apiService.getTPembacaan();
        call.enqueue(new Callback<ApiResponse<List<T_pembacaan>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<T_pembacaan>>> call, Response<ApiResponse<List<T_pembacaan>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<T_pembacaan> dataList = response.body().getData();
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
            public void onFailure(Call<ApiResponse<List<T_pembacaan>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error T_Pembacaan: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    private void syncBPiezoMetrik() {
        Log.d(TAG, "📊 Sync: B_Piezo_Metrik");

        // DIPERBAIKI: Menggunakan List untuk response
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

    private void syncPerhitunganPsMetrik() {
        Log.d(TAG, "📊 Sync: Perhitungan_T_PsMetrik");

        // DIPERBAIKI: Menggunakan List untuk response
        Call<ApiResponse<List<Perhitungan_t_psmetrik>>> call = apiService.getPerhitunganPsMetrik();
        call.enqueue(new Callback<ApiResponse<List<Perhitungan_t_psmetrik>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Perhitungan_t_psmetrik>>> call, Response<ApiResponse<List<Perhitungan_t_psmetrik>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    try {
                        List<Perhitungan_t_psmetrik> dataList = response.body().getData();
                        if (dataList != null) {
                            boolean success = dbHelper.syncPerhitunganPsMetrikData(dataList);
                            if (success) {
                                Log.d(TAG, "✅ Sync Perhitungan_T_PsMetrik berhasil: " + dataList.size() + " records");
                                successCount++;
                            } else {
                                Log.e(TAG, "❌ Sync Perhitungan_T_PsMetrik gagal (database error)");
                                failureCount++;
                            }
                        } else {
                            Log.e(TAG, "❌ Sync Perhitungan_T_PsMetrik: data list null");
                            failureCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing Perhitungan_T_PsMetrik: " + e.getMessage());
                        failureCount++;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : response.message();
                    Log.e(TAG, "❌ Sync Perhitungan_T_PsMetrik gagal: " + errorMsg);
                    failureCount++;
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Perhitungan_t_psmetrik>>> call, Throwable t) {
                Log.e(TAG, "❌ Network error Perhitungan_T_PsMetrik: " + t.getMessage());
                failureCount++;
                checkSyncCompletion();
            }
        });
    }

    // DIPERBAIKI: Method syncElevasiDasar dihapus total

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
        Log.d(TAG, "📈 Database Counts - I_Reading: " + dbHelper.getIReadingCount());
        Log.d(TAG, "📈 Database Counts - T_Pembacaan: " + dbHelper.getTPembacaanCount());
        Log.d(TAG, "📈 Database Counts - B_Piezo_Metrik: " + dbHelper.getBPiezoMetrikCount());
        Log.d(TAG, "📈 Database Counts - Perhitungan_PsMetrik: " + dbHelper.getPerhitunganPsMetrikCount());
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
            Intent intent = new Intent(this, InputDataRightPiezo.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error membuka Input Data Piezometer: " + e.getMessage());
            Toast.makeText(this, "Error membuka Input Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHistoryActivity() {
        try {
            // Di file HomeRightPiezoActivity.java - perbaikan baris 466
            Intent intent = new Intent(this, HistoryRightPiezoActivity.class);
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