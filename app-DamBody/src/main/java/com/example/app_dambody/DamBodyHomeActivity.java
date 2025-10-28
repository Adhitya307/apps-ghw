package com.example.app_dambody;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DamBodyHomeActivity extends AppCompatActivity {

    private LinearLayout btnELV625, btnELV600, btnHistory;
    private DatabaseHelper dbHelper;
    private ApiService api;
    private static final String TAG = "DamBodyHomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_dambody);

        dbHelper = new DatabaseHelper(this);
        api = ApiClient.getClient().create(ApiService.class);

        btnELV625 = findViewById(R.id.btnELV625);
        btnELV600 = findViewById(R.id.btnELV600);
        btnHistory = findViewById(R.id.btnHistory);

        // Log isi database
        logAllTables();

        // Sinkronisasi otomatis bila online
        if (isOnline()) {
            syncAllDataFromServer();
        } else {
            Toast.makeText(this, "📱 Offline - tidak bisa sync", Toast.LENGTH_SHORT).show();
        }

        btnELV625.setOnClickListener(v -> {
            startActivity(new Intent(this, InputdataElv625.class));
        });

        btnELV600.setOnClickListener(v -> {
            startActivity(new Intent(this, InputdataElv600.class));
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryDamBodyActivity.class));
        });
    }

    // =============================================================
    // ✅ CEK KONEKSI INTERNET
    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Cek koneksi error: " + e.getMessage());
            return false;
        }
    }

    // =============================================================
    // ✅ SINKRONISASI UTAMA (DENGAN AMBANG BATAS)
    private void syncAllDataFromServer() {
        Log.d(TAG, "🚀 Memulai sinkronisasi semua data dari server...");
        Toast.makeText(this, "🔄 Sinkronisasi dimulai...", Toast.LENGTH_SHORT).show();

        // Urutan sync semua data
        syncPengukuran();
        syncPembacaan625();
        syncPembacaan600();
        syncDepth625();
        syncDepth600();
        syncInitial625();
        syncInitial600();
        syncPergerakan625();
        syncPergerakan600();

        // ✅ TAMBAHKAN SYNC AMBANG BATAS
        syncAmbangBatas625();
        syncAmbangBatas600();
    }

    // =============================================================
    // ✅ SYNC: PENGUKURAN
    private void syncPengukuran() {
        api.getPengukuran().enqueue(new Callback<PengukuranResponse>() {
            @Override
            public void onResponse(Call<PengukuranResponse> call, Response<PengukuranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PengukuranModel> list = response.body().getData();
                    if (list != null) {
                        for (PengukuranModel item : list) {
                            dbHelper.insertOrUpdatePengukuran(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Pengukuran: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<PengukuranResponse> call, Throwable t) {
                Log.e(TAG, "❌ Gagal sync Pengukuran: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // ✅ SYNC: PEMBACAAN ELV625
    private void syncPembacaan625() {
        api.getPembacaan625().enqueue(new Callback<Pembacaan625Response>() {
            @Override
            public void onResponse(Call<Pembacaan625Response> call, Response<Pembacaan625Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pembacaan625Model> list = response.body().getData();
                    if (list != null) {
                        for (Pembacaan625Model item : list) {
                            dbHelper.insertOrUpdatePembacaan625(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Pembacaan ELV625: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Pembacaan ELV625 dari server.");
                }
            }

            @Override
            public void onFailure(Call<Pembacaan625Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Pembacaan ELV625: " + t.getMessage());
            }
        });
    }

    // ✅ SYNC: PEMBACAAN ELV600
    private void syncPembacaan600() {
        api.getPembacaan600().enqueue(new Callback<Pembacaan600Response>() {
            @Override
            public void onResponse(Call<Pembacaan600Response> call, Response<Pembacaan600Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pembacaan600Model> list = response.body().getData();
                    if (list != null) {
                        for (Pembacaan600Model item : list) {
                            dbHelper.insertOrUpdatePembacaan600(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Pembacaan ELV600: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Pembacaan ELV600 dari server.");
                }
            }

            @Override
            public void onFailure(Call<Pembacaan600Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Pembacaan ELV600: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // ✅ SYNC: DEPTH, INITIAL, PERGERAKAN
    private void syncDepth625() {
        api.getDepth625().enqueue(new Callback<Depth625Response>() {
            @Override
            public void onResponse(Call<Depth625Response> call, Response<Depth625Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Depth625Model> list = response.body().getData();
                    if (list != null) {
                        for (Depth625Model item : list) {
                            dbHelper.insertOrUpdateDepth625(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Depth ELV625: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Depth625Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Depth ELV625: " + t.getMessage());
            }
        });
    }

    private void syncDepth600() {
        api.getDepth600().enqueue(new Callback<Depth600Response>() {
            @Override
            public void onResponse(Call<Depth600Response> call, Response<Depth600Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Depth600Model> list = response.body().getData();
                    if (list != null) {
                        for (Depth600Model item : list) {
                            dbHelper.insertOrUpdateDepth600(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Depth ELV600: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Depth600Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Depth ELV600: " + t.getMessage());
            }
        });
    }

    private void syncInitial625() {
        api.getInitial625().enqueue(new Callback<Initial625Response>() {
            @Override
            public void onResponse(Call<Initial625Response> call, Response<Initial625Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Initial625Model> list = response.body().getData();
                    if (list != null) {
                        for (Initial625Model item : list) {
                            dbHelper.insertOrUpdateInitial625(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Initial ELV625: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Initial625Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Initial ELV625: " + t.getMessage());
            }
        });
    }

    private void syncInitial600() {
        api.getInitial600().enqueue(new Callback<Initial600Response>() {
            @Override
            public void onResponse(Call<Initial600Response> call, Response<Initial600Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Initial600Model> list = response.body().getData();
                    if (list != null) {
                        for (Initial600Model item : list) {
                            dbHelper.insertOrUpdateInitial600(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Initial ELV600: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Initial600Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Initial ELV600: " + t.getMessage());
            }
        });
    }

    private void syncPergerakan625() {
        api.getPergerakan625().enqueue(new Callback<Pergerakan625Response>() {
            @Override
            public void onResponse(Call<Pergerakan625Response> call, Response<Pergerakan625Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pergerakan625Model> list = response.body().getData();
                    if (list != null) {
                        for (Pergerakan625Model item : list) {
                            dbHelper.insertOrUpdatePergerakan625(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Pergerakan ELV625: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Pergerakan625Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Pergerakan ELV625: " + t.getMessage());
            }
        });
    }

    private void syncPergerakan600() {
        api.getPergerakan600().enqueue(new Callback<Pergerakan600Response>() {
            @Override
            public void onResponse(Call<Pergerakan600Response> call, Response<Pergerakan600Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Pergerakan600Model> list = response.body().getData();
                    if (list != null) {
                        for (Pergerakan600Model item : list) {
                            dbHelper.insertOrUpdatePergerakan600(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Pergerakan ELV600: " + list.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<Pergerakan600Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Pergerakan ELV600: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // ✅ SYNC: AMBANG BATAS ELV625
    // =============================================================
    private void syncAmbangBatas625() {
        Log.d(TAG, "🔄 Memulai sinkronisasi Ambang Batas ELV625...");

        syncAmbangBatas625H1();
        syncAmbangBatas625H2();
        syncAmbangBatas625H3();
    }

    private void syncAmbangBatas625H1() {
        api.getAmbangBatas625H1().enqueue(new Callback<AmbangBatas625H1Response>() {
            @Override
            public void onResponse(Call<AmbangBatas625H1Response> call, Response<AmbangBatas625H1Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas625H1Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas625H1Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas625H1(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 625 H1: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 625 H1 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas625H1Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 625 H1: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas625H2() {
        api.getAmbangBatas625H2().enqueue(new Callback<AmbangBatas625H2Response>() {
            @Override
            public void onResponse(Call<AmbangBatas625H2Response> call, Response<AmbangBatas625H2Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas625H2Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas625H2Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas625H2(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 625 H2: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 625 H2 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas625H2Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 625 H2: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas625H3() {
        api.getAmbangBatas625H3().enqueue(new Callback<AmbangBatas625H3Response>() {
            @Override
            public void onResponse(Call<AmbangBatas625H3Response> call, Response<AmbangBatas625H3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas625H3Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas625H3Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas625H3(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 625 H3: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 625 H3 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas625H3Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 625 H3: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // ✅ SYNC: AMBANG BATAS ELV600
    // =============================================================
    private void syncAmbangBatas600() {
        Log.d(TAG, "🔄 Memulai sinkronisasi Ambang Batas ELV600...");

        syncAmbangBatas600H1();
        syncAmbangBatas600H2();
        syncAmbangBatas600H3();
        syncAmbangBatas600H4();
        syncAmbangBatas600H5();
    }

    private void syncAmbangBatas600H1() {
        api.getAmbangBatas600H1().enqueue(new Callback<AmbangBatas600H1Response>() {
            @Override
            public void onResponse(Call<AmbangBatas600H1Response> call, Response<AmbangBatas600H1Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas600H1Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas600H1Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas600H1(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 600 H1: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 600 H1 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas600H1Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 600 H1: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas600H2() {
        api.getAmbangBatas600H2().enqueue(new Callback<AmbangBatas600H2Response>() {
            @Override
            public void onResponse(Call<AmbangBatas600H2Response> call, Response<AmbangBatas600H2Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas600H2Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas600H2Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas600H2(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 600 H2: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 600 H2 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas600H2Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 600 H2: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas600H3() {
        api.getAmbangBatas600H3().enqueue(new Callback<AmbangBatas600H3Response>() {
            @Override
            public void onResponse(Call<AmbangBatas600H3Response> call, Response<AmbangBatas600H3Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas600H3Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas600H3Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas600H3(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 600 H3: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 600 H3 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas600H3Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 600 H3: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas600H4() {
        api.getAmbangBatas600H4().enqueue(new Callback<AmbangBatas600H4Response>() {
            @Override
            public void onResponse(Call<AmbangBatas600H4Response> call, Response<AmbangBatas600H4Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas600H4Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas600H4Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas600H4(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 600 H4: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 600 H4 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas600H4Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 600 H4: " + t.getMessage());
            }
        });
    }

    private void syncAmbangBatas600H5() {
        api.getAmbangBatas600H5().enqueue(new Callback<AmbangBatas600H5Response>() {
            @Override
            public void onResponse(Call<AmbangBatas600H5Response> call, Response<AmbangBatas600H5Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AmbangBatas600H5Model> list = response.body().getData();
                    if (list != null) {
                        for (AmbangBatas600H5Model item : list) {
                            dbHelper.insertOrUpdateAmbangBatas600H5(item);
                        }
                        Log.i(TAG, "✅ Sinkronisasi Ambang Batas 600 H5: " + list.size());
                    }
                } else {
                    Log.w(TAG, "⚠️ Tidak ada data Ambang Batas 600 H5 dari server.");
                }
            }

            @Override
            public void onFailure(Call<AmbangBatas600H5Response> call, Throwable t) {
                Log.e(TAG, "❌ Error sync Ambang Batas 600 H5: " + t.getMessage());
            }
        });
    }

    // =============================================================
    // 🧠 DEBUG: TAMPILKAN SEMUA TABEL & DATA
    private void logAllTables() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        Log.d(TAG, "===============================");
        Log.d(TAG, "📋 Daftar tabel dalam database:");
        Log.d(TAG, "===============================");
        if (c.moveToFirst()) {
            do {
                String tableName = c.getString(0);
                Log.d(TAG, "➡️  " + tableName);
                Cursor data = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
                if (data.moveToFirst()) {
                    StringBuilder sb = new StringBuilder("contoh data: ");
                    for (int i = 0; i < data.getColumnCount(); i++) {
                        sb.append(data.getColumnName(i)).append("=")
                                .append(data.getString(i)).append("  ");
                    }
                    Log.d(TAG, "    " + sb);
                } else {
                    Log.d(TAG, "    (tabel kosong)");
                }
                data.close();
            } while (c.moveToNext());
        }
        c.close();
        Log.d(TAG, "===============================");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}