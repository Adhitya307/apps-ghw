package com.apps.bubbletilt;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BubbleTiltHomeActivity extends AppCompatActivity {

    private LinearLayout btnInputData, btnHistory;
    private DatabaseHelperBtm dbHelper;
    private BtmApiService api;
    private static final String TAG = "BubbleTiltHome";
    private AtomicInteger pendingSyncOperations = new AtomicInteger(0);
    private Handler syncHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bubble_tilt);

        // Initialize database and API
        dbHelper = new DatabaseHelperBtm(this);
        api = ApiClient.getBtmApiService();

        initViews();
        setupClickListeners();

        // Log database contents
        logAllTables();

        // Auto sync if online dengan delay untuk menghindari blocking UI
        syncHandler.postDelayed(() -> {
            if (isOnline()) {
                Log.d(TAG, "📱 Device online, memulai sinkronisasi otomatis...");
                syncAllDataFromServer();
            } else {
                Log.w(TAG, "📱 Offline Mode - Hanya menggunakan data lokal");
                Toast.makeText(this, "📱 Offline Mode - Data lokal tersedia", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private void initViews() {
        btnInputData = findViewById(R.id.btnInputData);
        btnHistory = findViewById(R.id.btnDataHistory);
    }

    private void setupClickListeners() {
        // Input Data Button
        btnInputData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v, () -> goToInputDataActivity());
            }
        });

        // History Button
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v, () -> goToHistoryActivity());
            }
        });
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
            Intent intent = new Intent(this, InputDataBubbleTilt.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error membuka Input Data: " + e.getMessage());
            Toast.makeText(this, "Error membuka Input Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHistoryActivity() {
        try {
            Intent intent = new Intent(this, HistoryBtmActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error membuka History: " + e.getMessage());
            Toast.makeText(this, "Error membuka History", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== NETWORK & SYNC METHODS ====================

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "❌ Cek koneksi error: " + e.getMessage());
            return false;
        }
    }

    // ==================== MAIN SYNC METHOD ====================

    private void syncAllDataFromServer() {
        Log.d(TAG, "🚀 ================================");
        Log.d(TAG, "🚀 Memulai sinkronisasi semua data BTM dari server...");
        Log.d(TAG, "🚀 ================================");

        Toast.makeText(this, "🔄 Sinkronisasi data BTM dimulai...", Toast.LENGTH_SHORT).show();

        // Reset counter
        pendingSyncOperations.set(0);

        // Hitung total operasi sync
        int totalOperations = 1 + 8 + 8 + 8; // Pengukuran + Bacaan + Perhitungan + Scatter
        pendingSyncOperations.set(totalOperations);

        Log.d(TAG, "📊 Total operasi sync: " + totalOperations);

        // Sync all data in sequence dengan delay antar group
        syncHandler.postDelayed(this::syncPengukuran, 100);
        syncHandler.postDelayed(this::syncAllBacaan, 500);
        syncHandler.postDelayed(this::syncAllPerhitungan, 1000);
        syncHandler.postDelayed(this::syncAllScatter, 1500);
    }

    private void checkSyncCompletion() {
        int remaining = pendingSyncOperations.decrementAndGet();
        Log.d(TAG, "📊 Sisa operasi sync: " + remaining);

        if (remaining <= 0) {
            syncHandler.postDelayed(() -> {
                Log.d(TAG, "✅ ================================");
                Log.d(TAG, "✅ Semua operasi sinkronisasi selesai!");
                Log.d(TAG, "✅ ================================");
                Toast.makeText(BubbleTiltHomeActivity.this, "✅ Sinkronisasi selesai", Toast.LENGTH_SHORT).show();

                // Refresh log database
                logAllTables();
            }, 1000);
        }
    }

    // ==================== SYNC: PENGUKURAN ====================

    private void syncPengukuran() {
        Log.d(TAG, "🔄 Memulai sync Pengukuran...");
        api.getPengukuran().enqueue(new Callback<PengukuranResponse>() {
            @Override
            public void onResponse(Call<PengukuranResponse> call, Response<PengukuranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PengukuranResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PengukuranBtmModel> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PengukuranBtmModel item : list) {
                                long result = dbHelper.insertOrUpdatePengukuran(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Pengukuran BERHASIL: " + successCount + "/" + list.size() + " records");
                        } else {
                            Log.i(TAG, "ℹ️ Sync Pengukuran: Tidak ada data dari server");
                        }
                    } else {
                        Log.w(TAG, "⚠️ Sync Pengukuran: Response tidak success - " + apiResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "❌ Sync Pengukuran: Response gagal - " + response.message());
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PengukuranResponse> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Pengukuran: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    // ==================== SYNC: ALL BACAAN ====================

    private void syncAllBacaan() {
        Log.d(TAG, "🔄 Memulai sync semua Bacaan...");
        syncBacaanBt1();
        syncBacaanBt2();
        syncBacaanBt3();
        syncBacaanBt4();
        syncBacaanBt6();
        syncBacaanBt7();
        syncBacaanBt8();
    }

    private void syncBacaanBt1() {
        api.getBacaanBt1().enqueue(new Callback<BacaanBt1Response>() {
            @Override
            public void onResponse(Call<BacaanBt1Response> call, Response<BacaanBt1Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt1Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt1Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt1Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt1(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT1 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt1Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt2() {
        api.getBacaanBt2().enqueue(new Callback<BacaanBt2Response>() {
            @Override
            public void onResponse(Call<BacaanBt2Response> call, Response<BacaanBt2Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt2Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt2Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt2Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt2(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT2 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt2Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt3() {
        api.getBacaanBt3().enqueue(new Callback<BacaanBt3Response>() {
            @Override
            public void onResponse(Call<BacaanBt3Response> call, Response<BacaanBt3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt3Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt3Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt3Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt3(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT3 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt3Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt4() {
        api.getBacaanBt4().enqueue(new Callback<BacaanBt4Response>() {
            @Override
            public void onResponse(Call<BacaanBt4Response> call, Response<BacaanBt4Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt4Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt4Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt4Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt4(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT4 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt4Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt6() {
        api.getBacaanBt6().enqueue(new Callback<BacaanBt6Response>() {
            @Override
            public void onResponse(Call<BacaanBt6Response> call, Response<BacaanBt6Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt6Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt6Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt6Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt6(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT6 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt6Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT6: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt7() {
        api.getBacaanBt7().enqueue(new Callback<BacaanBt7Response>() {
            @Override
            public void onResponse(Call<BacaanBt7Response> call, Response<BacaanBt7Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt7Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt7Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt7Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt7(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT7 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt7Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT7: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncBacaanBt8() {
        api.getBacaanBt8().enqueue(new Callback<BacaanBt8Response>() {
            @Override
            public void onResponse(Call<BacaanBt8Response> call, Response<BacaanBt8Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BacaanBt8Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<BacaanBt8Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (BacaanBt8Model item : list) {
                                long result = dbHelper.insertOrUpdateBacaanBt8(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Bacaan BT8 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<BacaanBt8Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Bacaan BT8: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    // ==================== SYNC: ALL PERHITUNGAN ====================

    private void syncAllPerhitungan() {
        Log.d(TAG, "🔄 Memulai sync semua Perhitungan...");
        syncPerhitunganBt1();
        syncPerhitunganBt2();
        syncPerhitunganBt3();
        syncPerhitunganBt4();
        syncPerhitunganBt6();
        syncPerhitunganBt7();
        syncPerhitunganBt8();
    }

    private void syncPerhitunganBt1() {
        api.getPerhitunganBt1().enqueue(new Callback<PerhitunganBt1Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt1Response> call, Response<PerhitunganBt1Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt1Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt1Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt1Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt1(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT1 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt1Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt2() {
        api.getPerhitunganBt2().enqueue(new Callback<PerhitunganBt2Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt2Response> call, Response<PerhitunganBt2Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt2Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt2Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt2Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt2(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT2 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt2Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt3() {
        api.getPerhitunganBt3().enqueue(new Callback<PerhitunganBt3Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt3Response> call, Response<PerhitunganBt3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt3Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt3Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt3Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt3(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT3 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt3Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt4() {
        api.getPerhitunganBt4().enqueue(new Callback<PerhitunganBt4Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt4Response> call, Response<PerhitunganBt4Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt4Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt4Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt4Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt4(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT4 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt4Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt6() {
        api.getPerhitunganBt6().enqueue(new Callback<PerhitunganBt6Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt6Response> call, Response<PerhitunganBt6Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt6Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt6Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt6Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt6(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT6 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt6Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT6: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt7() {
        api.getPerhitunganBt7().enqueue(new Callback<PerhitunganBt7Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt7Response> call, Response<PerhitunganBt7Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt7Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt7Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt7Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt7(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT7 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt7Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT7: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncPerhitunganBt8() {
        api.getPerhitunganBt8().enqueue(new Callback<PerhitunganBt8Response>() {
            @Override
            public void onResponse(Call<PerhitunganBt8Response> call, Response<PerhitunganBt8Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerhitunganBt8Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<PerhitunganBt8Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (PerhitunganBt8Model item : list) {
                                long result = dbHelper.insertOrUpdatePerhitunganBt8(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Perhitungan BT8 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<PerhitunganBt8Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Perhitungan BT8: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    // ==================== SYNC: ALL SCATTER ====================

    private void syncAllScatter() {
        Log.d(TAG, "🔄 Memulai sync semua Scatter...");
        syncScatterBt1();
        syncScatterBt2();
        syncScatterBt3();
        syncScatterBt4();
        syncScatterBt6();
        syncScatterBt7();
        syncScatterBt8();
    }

    private void syncScatterBt1() {
        api.getScatterBt1().enqueue(new Callback<ScatterBt1Response>() {
            @Override
            public void onResponse(Call<ScatterBt1Response> call, Response<ScatterBt1Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt1Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt1Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt1Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt1(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT1 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt1Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT1: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt2() {
        api.getScatterBt2().enqueue(new Callback<ScatterBt2Response>() {
            @Override
            public void onResponse(Call<ScatterBt2Response> call, Response<ScatterBt2Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt2Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt2Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt2Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt2(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT2 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt2Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT2: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt3() {
        api.getScatterBt3().enqueue(new Callback<ScatterBt3Response>() {
            @Override
            public void onResponse(Call<ScatterBt3Response> call, Response<ScatterBt3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt3Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt3Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt3Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt3(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT3 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt3Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT3: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt4() {
        api.getScatterBt4().enqueue(new Callback<ScatterBt4Response>() {
            @Override
            public void onResponse(Call<ScatterBt4Response> call, Response<ScatterBt4Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt4Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt4Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt4Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt4(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT4 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt4Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT4: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt6() {
        api.getScatterBt6().enqueue(new Callback<ScatterBt6Response>() {
            @Override
            public void onResponse(Call<ScatterBt6Response> call, Response<ScatterBt6Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt6Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt6Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt6Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt6(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT6 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt6Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT6: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt7() {
        api.getScatterBt7().enqueue(new Callback<ScatterBt7Response>() {
            @Override
            public void onResponse(Call<ScatterBt7Response> call, Response<ScatterBt7Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt7Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt7Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt7Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt7(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT7 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt7Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT7: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    private void syncScatterBt8() {
        api.getScatterBt8().enqueue(new Callback<ScatterBt8Response>() {
            @Override
            public void onResponse(Call<ScatterBt8Response> call, Response<ScatterBt8Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ScatterBt8Response apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        List<ScatterBt8Model> list = apiResponse.getData();
                        if (list != null && !list.isEmpty()) {
                            int successCount = 0;
                            for (ScatterBt8Model item : list) {
                                long result = dbHelper.insertOrUpdateScatterBt8(item);
                                if (result != -1) successCount++;
                            }
                            Log.i(TAG, "✅ Sync Scatter BT8 BERHASIL: " + successCount + "/" + list.size() + " records");
                        }
                    }
                }
                checkSyncCompletion();
            }

            @Override
            public void onFailure(Call<ScatterBt8Response> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Scatter BT8: " + t.getMessage());
                checkSyncCompletion();
            }
        });
    }

    // ==================== DEBUG: LOG DATABASE ====================

    private void logAllTables() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            Log.d(TAG, "===============================");
            Log.d(TAG, "📋 Daftar tabel BTM dalam database:");
            Log.d(TAG, "===============================");
            if (c.moveToFirst()) {
                do {
                    String tableName = c.getString(0);
                    Log.d(TAG, "➡️  " + tableName);
                    Cursor data = db.rawQuery("SELECT COUNT(*) as count FROM " + tableName, null);
                    if (data.moveToFirst()) {
                        int count = data.getInt(0);
                        Log.d(TAG, "    Jumlah data: " + count);
                    }
                    data.close();
                } while (c.moveToNext());
            }
            c.close();
            Log.d(TAG, "===============================");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error logging database: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler
        syncHandler.removeCallbacksAndMessages(null);
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