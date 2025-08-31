package com.example.kerjapraktik;

import android.content.Intent;
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

public class HomeActivity extends AppCompatActivity {

    private LinearLayout btnInput, btnInputHP2, btnHistory;
    private DatabaseHelper dbHelper;
    private ApiService api;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);
        api = ApiClient.getClient().create(ApiService.class);

        btnInput = findViewById(R.id.btnInput);
        btnInputHP2 = findViewById(R.id.btnInputHP2);
        btnHistory = findViewById(R.id.btnHistory);

        if (isOnline()) {
            syncDataFromServer();
        } else {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
        }

        btnInput.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputDataActivity.class);
            intent.putExtra("device_mode", "hp1");
            startActivity(intent);
        });

        btnInputHP2.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, InputData2Activity.class);
            intent.putExtra("device_mode", "hp2");
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void syncDataFromServer() {
        Log.d(TAG, "Memulai sinkronisasi data dari server...");

        // === 1. Data Pengukuran ===
        api.getPengukuran().enqueue(new Callback<PengukuranResponse>() {
            @Override
            public void onResponse(Call<PengukuranResponse> call, Response<PengukuranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PengukuranModel> pengukuranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = pengukuranList.size();

                    for (PengukuranModel p : pengukuranList) {
                        try {
                            dbHelper.insertPengukuran(
                                    p.getId(),
                                    p.getTahun(),
                                    p.getBulan(),
                                    p.getPeriode(),
                                    p.getTanggal(),
                                    p.getTmaWaduk(),
                                    p.getCurahHujan(),
                                    p.getTempId()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan pengukuran: " + p.getTanggal(), e);
                        }
                    }

                    String message = "Pengukuran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PengukuranResponse> call, Throwable t) {
                Log.e(TAG, "Error pengukuran: " + t.getMessage(), t);
            }
        });

        // === 2. Data Thomson Weir ===
        api.getThomson().enqueue(new Callback<ThomsonResponse>() {
            @Override
            public void onResponse(Call<ThomsonResponse> call, Response<ThomsonResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ThomsonWeirModel> thomsonList = response.body().getData();
                    int successCount = 0;
                    int totalCount = thomsonList.size();

                    for (ThomsonWeirModel t : thomsonList) {
                        try {
                            dbHelper.insertThomsonWeir(
                                    t.getPengukuran_id(),
                                    t.getA1_r(),
                                    t.getA1_l(),
                                    t.getB1(),
                                    t.getB3(),
                                    t.getB5()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan thomson, pengukuran ID=" + t.getPengukuran_id(), e);
                        }
                    }

                    String message = "Thomson: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ThomsonResponse> call, Throwable t) {
                Log.e(TAG, "Error thomson: " + t.getMessage(), t);
            }
        });

        // === 3. Data SR (pakai insertSRFull) ===
        api.getSR().enqueue(new Callback<SRResponse>() {
            @Override
            public void onResponse(Call<SRResponse> call, Response<SRResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SRModel> srList = response.body().getData();
                    int successCount = 0;
                    int totalCount = srList.size();

                    for (SRModel s : srList) {
                        try {
                            dbHelper.insertSRFull(
                                    s.getPengukuranId(),
                                    s.getSr1Kode(), s.getSr1Nilai(),
                                    s.getSr40Kode(), s.getSr40Nilai(),
                                    s.getSr66Kode(), s.getSr66Nilai(),
                                    s.getSr68Kode(), s.getSr68Nilai(),
                                    s.getSr70Kode(), s.getSr70Nilai(),
                                    s.getSr79Kode(), s.getSr79Nilai(),
                                    s.getSr81Kode(), s.getSr81Nilai(),
                                    s.getSr83Kode(), s.getSr83Nilai(),
                                    s.getSr85Kode(), s.getSr85Nilai(),
                                    s.getSr92Kode(), s.getSr92Nilai(),
                                    s.getSr94Kode(), s.getSr94Nilai(),
                                    s.getSr96Kode(), s.getSr96Nilai(),
                                    s.getSr98Kode(), s.getSr98Nilai(),
                                    s.getSr100Kode(), s.getSr100Nilai(),
                                    s.getSr102Kode(), s.getSr102Nilai(),
                                    s.getSr104Kode(), s.getSr104Nilai(),
                                    s.getSr106Kode(), s.getSr106Nilai()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan SR, pengukuran ID=" + s.getPengukuranId(), e);
                        }
                    }

                    String message = "SR: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SRResponse> call, Throwable t) {
                Log.e(TAG, "Error SR: " + t.getMessage(), t);
            }
        });

        // === 4. Data Bocoran Baru (pakai value + kode) ===
        api.getBocoran().enqueue(new Callback<BocoranResponse>() {
            @Override
            public void onResponse(Call<BocoranResponse> call, Response<BocoranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BocoranBaruModel> bocoranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = bocoranList.size();

                    for (BocoranBaruModel b : bocoranList) {
                        try {
                            dbHelper.insertBocoranFull(
                                    b.getPengukuran_id(),
                                    b.getElv_624_t1(), b.getElv_624_t1_kode(),
                                    b.getElv_615_t2(), b.getElv_615_t2_kode(),
                                    b.getPipa_p1(), b.getPipa_p1_kode()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan bocoran, pengukuran ID=" + b.getPengukuran_id(), e);
                        }
                    }

                    String message = "Bocoran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BocoranResponse> call, Throwable t) {
                Log.e(TAG, "Error bocoran: " + t.getMessage(), t);
            }
        });

        // === 5. Data Batas Maksimal ===
        api.getBatasMaksimal().enqueue(new Callback<BatasMaksimalResponse>() {
            @Override
            public void onResponse(Call<BatasMaksimalResponse> call, Response<BatasMaksimalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BatasMaksimalModel> batasList = response.body().getData();
                    int successCount = 0;
                    int totalCount = batasList.size();

                    for (BatasMaksimalModel b : batasList) {
                        try {
                            dbHelper.insertPBatasMaksimal(
                                    b.getPengukuran_id(),
                                    b.getBatas_maksimal()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan batas maksimal, pengukuran ID=" + b.getPengukuran_id(), e);
                        }
                    }

                    String message = "Batas Maksimal: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BatasMaksimalResponse> call, Throwable t) {
                Log.e(TAG, "Error batas maksimal: " + t.getMessage(), t);
            }
        });

        // === 6. Data Bocoran Baru (p_) ===
        api.getPBocoranBaru().enqueue(new Callback<PBocoranBaruResponse>() {
            @Override
            public void onResponse(Call<PBocoranBaruResponse> call, Response<PBocoranBaruResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PBocoranBaruModel> bocoranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = bocoranList.size();

                    for (PBocoranBaruModel b : bocoranList) {
                        try {
                            dbHelper.insertPBocoranBaru(
                                    b.getId(),
                                    b.getPengukuran_id(),
                                    b.getTalang1(),
                                    b.getTalang2(),
                                    b.getPipa(),
                                    b.getCreated_at(),
                                    b.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_bocoran_baru, ID=" + b.getId(), e);
                        }
                    }

                    String message = "P Bocoran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PBocoranBaruResponse> call, Throwable t) {
                Log.e(TAG, "Error p_bocoran_baru: " + t.getMessage(), t);
            }
        });

        // === 7. Data Inti Gallery ===
        api.getPIntiGallery().enqueue(new Callback<PIntiGalleryResponse>() {
            @Override
            public void onResponse(Call<PIntiGalleryResponse> call, Response<PIntiGalleryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PIntiGalleryModel> intiList = response.body().getData();
                    int successCount = 0;
                    int totalCount = intiList.size();

                    for (PIntiGalleryModel i : intiList) {
                        try {
                            dbHelper.insertPIntiGallery(
                                    i.getPengukuran_id(),
                                    i.getA1(),
                                    i.getAmbang_a1(),
                                    i.getCreated_at(),
                                    i.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_intigalery, pengukuran ID=" + i.getPengukuran_id(), e);
                        }
                    }

                    String message = "Inti Gallery: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PIntiGalleryResponse> call, Throwable t) {
                Log.e(TAG, "Error p_intigalery: " + t.getMessage(), t);
            }
        });

        // === 8. Data Spillway ===
        api.getPSpillway().enqueue(new Callback<PSpillwayResponse>() {
            @Override
            public void onResponse(Call<PSpillwayResponse> call, Response<PSpillwayResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PSpillwayModel> spillwayList = response.body().getData();
                    int successCount = 0;
                    int totalCount = spillwayList.size();

                    for (PSpillwayModel s : spillwayList) {
                        try {
                            dbHelper.insertPSpillway(
                                    s.getPengukuran_id(),
                                    s.getB3(),
                                    s.getAmbang(),
                                    s.getCreated_at(),
                                    s.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_spillway, pengukuran ID=" + s.getPengukuran_id(), e);
                        }
                    }

                    String message = "Spillway: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PSpillwayResponse> call, Throwable t) {
                Log.e(TAG, "Error p_spillway: " + t.getMessage(), t);
            }
        });

        // === 9. Data P SR (hasil) ===
        api.getPSR().enqueue(new Callback<PSRResponse>() {
            @Override
            public void onResponse(Call<PSRResponse> call, Response<PSRResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PSRModel> psrList = response.body().getData();
                    int successCount = 0;
                    int totalCount = psrList.size();

                    for (PSRModel p : psrList) {
                        try {
                            dbHelper.insertPSr(
                                    p.getPengukuran_id(),
                                    p.getSr_1_q(), p.getSr_40_q(), p.getSr_66_q(), p.getSr_68_q(),
                                    p.getSr_70_q(), p.getSr_79_q(), p.getSr_81_q(), p.getSr_83_q(),
                                    p.getSr_85_q(), p.getSr_92_q(), p.getSr_94_q(), p.getSr_96_q(),
                                    p.getSr_98_q(), p.getSr_100_q(), p.getSr_102_q(), p.getSr_104_q(),
                                    p.getSr_106_q(), p.getCreated_at(), p.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_sr, pengukuran ID=" + p.getPengukuran_id(), e);
                        }
                    }

                    String message = "P SR: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PSRResponse> call, Throwable t) {
                Log.e(TAG, "Error p_sr: " + t.getMessage(), t);
            }
        });

        // === 10. Data Tebing Kanan ===
        api.getPTebingKanan().enqueue(new Callback<PTebingKananResponse>() {
            @Override
            public void onResponse(Call<PTebingKananResponse> call, Response<PTebingKananResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PTebingKananModel> tebingList = response.body().getData();
                    int successCount = 0;
                    int totalCount = tebingList.size();

                    for (PTebingKananModel t : tebingList) {
                        try {
                            dbHelper.insertPTebingKanan(
                                    t.getPengukuran_id(),
                                    t.getSr(),
                                    t.getAmbang(),
                                    t.getB5(),
                                    t.getCreated_at(),
                                    t.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_tebingkanan, pengukuran ID=" + t.getPengukuran_id(), e);
                        }
                    }

                    String message = "Tebing Kanan: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PTebingKananResponse> call, Throwable t) {
                Log.e(TAG, "Error p_tebingkanan: " + t.getMessage(), t);
            }
        });

        // === 11. Data P Thomson Weir (hasil) ===
        api.getPThomsonWeir().enqueue(new Callback<PThomsonWeirResponse>() {
            @Override
            public void onResponse(Call<PThomsonWeirResponse> call, Response<PThomsonWeirResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PThomsonWeirModel> thomsonList = response.body().getData();
                    int successCount = 0;
                    int totalCount = thomsonList.size();

                    for (PThomsonWeirModel t : thomsonList) {
                        try {
                            dbHelper.insertPThomsonWeir(
                                    t.getPengukuran_id(),
                                    t.getA1_r(),
                                    t.getA1_l(),
                                    t.getB1(),
                                    t.getB3(),
                                    t.getB5()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_thomson_weir, pengukuran ID=" + t.getPengukuran_id(), e);
                        }
                    }

                    String message = "P Thomson: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PThomsonWeirResponse> call, Throwable t) {
                Log.e(TAG, "Error p_thomson_weir: " + t.getMessage(), t);
            }
        });

        // === 12. Data Total Bocoran ===
        api.getPTotalBocoran().enqueue(new Callback<PTotalBocoranResponse>() {
            @Override
            public void onResponse(Call<PTotalBocoranResponse> call, Response<PTotalBocoranResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PTotalBocoranModel> totalBocoranList = response.body().getData();
                    int successCount = 0;
                    int totalCount = totalBocoranList.size();

                    for (PTotalBocoranModel t : totalBocoranList) {
                        try {
                            dbHelper.insertPTotalBocoran(
                                    t.getPengukuran_id(),
                                    t.getR1(),
                                    t.getCreated_at(),
                                    t.getUpdated_at()
                            );
                            successCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal simpan p_totalbocoran, pengukuran ID=" + t.getPengukuran_id(), e);
                        }
                    }

                    String message = "Total Bocoran: " + successCount + "/" + totalCount + " data tersinkron";
                    Log.i(TAG, message);
                    Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PTotalBocoranResponse> call, Throwable t) {
                Log.e(TAG, "Error p_totalbocoran: " + t.getMessage(), t);
            }
        });
    }
}