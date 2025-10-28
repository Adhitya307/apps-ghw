package com.example.app_dambody;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryDamBodyActivity extends AppCompatActivity {

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData, btnExportDB;
    private TextView tvData625, tvData600;
    private CardView cardELV625, cardELV600;
    private DatabaseHelper dbHelper;
    private List<PengukuranModel> pengukuranList;
    private ProgressBar progressBarExport;

    private int selectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_dambody);

        initViews();
        dbHelper = new DatabaseHelper(this);
        loadSpinnerData();
        setupClickListeners();
    }

    private void initViews() {
        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnTampilkanData = findViewById(R.id.btnTampilkanData);
        btnExportDB = findViewById(R.id.btnExportDB);
        tvData625 = findViewById(R.id.tvData625);
        tvData600 = findViewById(R.id.tvData600);
        cardELV625 = findViewById(R.id.cardELV625);
        cardELV600 = findViewById(R.id.cardELV600);
        progressBarExport = findViewById(R.id.progressBarExport);
    }

    private void setupClickListeners() {
        btnTampilkanData.setOnClickListener(v -> {
            if (selectedId == -1) {
                Toast.makeText(this, "⚠️ Pilih data pengukuran dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            tampilkanData(selectedId);
        });

        btnExportDB.setOnClickListener(v -> exportDatabaseToSQL());
    }

    private void loadSpinnerData() {
        pengukuranList = dbHelper.getAllPengukuran();
        if (pengukuranList.isEmpty()) {
            Toast.makeText(this, "📭 Tidak ada data pengukuran", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (PengukuranModel p : pengukuranList) {
            labels.add("ID " + p.getId_pengukuran() + " - " + p.getTanggal() + " (" + p.getPeriode() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPengukuran.setAdapter(adapter);

        spinnerPengukuran.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedId = pengukuranList.get(position).getId_pengukuran();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedId = -1;
            }
        });
    }

    private void tampilkanData(int idPengukuran) {
        List<Pembacaan625Model> baca625 = dbHelper.getPembacaan625ByPengukuran(idPengukuran);
        List<Pembacaan600Model> baca600 = dbHelper.getPembacaan600ByPengukuran(idPengukuran);
        List<Pergerakan625Model> gerak625 = dbHelper.getPergerakan625ByPengukuran(idPengukuran);
        List<Pergerakan600Model> gerak600 = dbHelper.getPergerakan600ByPengukuran(idPengukuran);

        StringBuilder sb625 = new StringBuilder();
        StringBuilder sb600 = new StringBuilder();

        // ======== ELV625 ========
        if (!baca625.isEmpty() || !gerak625.isEmpty()) {
            cardELV625.setVisibility(View.VISIBLE);

            // Pembacaan ELV625
            if (!baca625.isEmpty()) {
                sb625.append("📊 <b>PEMBACAAN ELV625</b><br><br>");
                int i = 1;
                for (Pembacaan625Model b : baca625) {
                    sb625.append("• <b>Data Pembacaan ").append(i++).append("</b><br>")
                            .append("  HV1 : ").append(b.getHv_1()).append("<br>")
                            .append("  HV2 : ").append(b.getHv_2()).append("<br>")
                            .append("  HV3 : ").append(b.getHv_3()).append("<br><br>");
                }
            }

            // Pergerakan ELV625
            if (!gerak625.isEmpty()) {
                sb625.append("🔄 <b>PERGERAKAN ELV625</b><br><br>");
                int i = 1;
                for (Pergerakan625Model m : gerak625) {
                    sb625.append("• <b>Data Pergerakan ").append(i++).append("</b><br>")
                            .append("  HV1 : ").append(m.getHv_1()).append("<br>")
                            .append("  HV2 : ").append(m.getHv_2()).append("<br>")
                            .append("  HV3 : ").append(m.getHv_3()).append("<br><br>");
                }
            }
        } else {
            cardELV625.setVisibility(View.GONE);
            sb625.append("Tidak ada data ELV625");
        }

        // ======== ELV600 ========
        if (!baca600.isEmpty() || !gerak600.isEmpty()) {
            cardELV600.setVisibility(View.VISIBLE);

            // Pembacaan ELV600
            if (!baca600.isEmpty()) {
                sb600.append("📊 <b>PEMBACAAN ELV600</b><br><br>");
                int i = 1;
                for (Pembacaan600Model b : baca600) {
                    sb600.append("• <b>Data Pembacaan ").append(i++).append("</b><br>")
                            .append("  HV1 : ").append(b.getHv_1()).append("<br>")
                            .append("  HV2 : ").append(b.getHv_2()).append("<br>")
                            .append("  HV3 : ").append(b.getHv_3()).append("<br>")
                            .append("  HV4 : ").append(b.getHv_4()).append("<br>")
                            .append("  HV5 : ").append(b.getHv_5()).append("<br><br>");
                }
            }

            // Pergerakan ELV600
            if (!gerak600.isEmpty()) {
                sb600.append("🔄 <b>PERGERAKAN ELV600</b><br><br>");
                int i = 1;
                for (Pergerakan600Model m : gerak600) {
                    sb600.append("• <b>Data Pergerakan ").append(i++).append("</b><br>")
                            .append("  HV1 : ").append(m.getHv_1()).append("<br>")
                            .append("  HV2 : ").append(m.getHv_2()).append("<br>")
                            .append("  HV3 : ").append(m.getHv_3()).append("<br>")
                            .append("  HV4 : ").append(m.getHv_4()).append("<br>")
                            .append("  HV5 : ").append(m.getHv_5()).append("<br><br>");
                }
            }
        } else {
            cardELV600.setVisibility(View.GONE);
            sb600.append("Tidak ada data ELV600");
        }

        // Set text dengan HTML formatting
        tvData625.setText(Html.fromHtml(sb625.toString()));
        tvData600.setText(Html.fromHtml(sb600.toString()));
    }

    private void exportDatabaseToSQL() {
        progressBarExport.setVisibility(View.VISIBLE);
        btnExportDB.setEnabled(false);
        btnExportDB.setText("Mengekspor...");

        new Thread(() -> {
            try {
                // Buat nama file dengan timestamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "DamBody_DB_" + timeStamp + ".sql";

                // Direktori download
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                File exportFile = new File(downloadDir, fileName);

                // Generate SQL content
                String sqlContent = generateSQLContent();

                // Write SQL content to file
                FileWriter writer = new FileWriter(exportFile);
                writer.write(sqlContent);
                writer.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Database berhasil diekspor ke SQL: " + fileName, Toast.LENGTH_LONG).show();
                    resetExportButton();

                    // Tampilkan dialog sukses dengan opsi share
                    showExportSuccessDialog(exportFile);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Gagal mengekspor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetExportButton();
                });
            }
        }).start();
    }

    private String generateSQLContent() {
        StringBuilder sql = new StringBuilder();

        // Header SQL
        sql.append("-- DamBody Database Export\n");
        sql.append("-- Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sql.append("-- Database: ").append(DatabaseHelper.DATABASE_NAME).append("\n\n");

        // Export semua tabel
        exportTablePengukuran(sql);
        exportTablePembacaan625(sql);
        exportTablePembacaan600(sql);
        exportTableDepth625(sql);
        exportTableDepth600(sql);
        exportTableInitial625(sql);
        exportTableInitial600(sql);
        exportTablePergerakan625(sql);
        exportTablePergerakan600(sql);

        // Export tabel ambang batas
        exportTableAmbangBatas625H1(sql);
        exportTableAmbangBatas625H2(sql);
        exportTableAmbangBatas625H3(sql);
        exportTableAmbangBatas600H1(sql);
        exportTableAmbangBatas600H2(sql);
        exportTableAmbangBatas600H3(sql);
        exportTableAmbangBatas600H4(sql);
        exportTableAmbangBatas600H5(sql);

        return sql.toString();
    }

    private void exportTablePengukuran(StringBuilder sql) {
        sql.append("-- Table: t_pengukuran_hdm\n");
        List<PengukuranModel> data = dbHelper.getAllPengukuran();
        for (PengukuranModel item : data) {
            sql.append("INSERT OR REPLACE INTO t_pengukuran_hdm (id_pengukuran, tahun, periode, tanggal, dma, temp_id, created_at, updated_at) VALUES (")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getTahun()).append(", ")
                    .append(quote(item.getPeriode())).append(", ")
                    .append(quote(item.getTanggal())).append(", ")
                    .append(quote(item.getDma())).append(", ")
                    .append(quote(item.getTemp_id())).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTablePembacaan625(StringBuilder sql) {
        sql.append("-- Table: t_pembacaan_hdm_elv625\n");
        List<Pembacaan625Model> data = dbHelper.getAllPembacaan625();
        for (Pembacaan625Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_pembacaan_hdm_elv625 (id_pembacaan, id_pengukuran, hv_1, hv_2, hv_3, created_at, updated_at) VALUES (")
                    .append(item.getId_pembacaan()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTablePembacaan600(StringBuilder sql) {
        sql.append("-- Table: t_pembacaan_hdm_elv600\n");
        List<Pembacaan600Model> data = dbHelper.getAllPembacaan600();
        for (Pembacaan600Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_pembacaan_hdm_elv600 (id_pembacaan, id_pengukuran, hv_1, hv_2, hv_3, hv_4, hv_5, created_at, updated_at) VALUES (")
                    .append(item.getId_pembacaan()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append(item.getHv_4()).append(", ")
                    .append(item.getHv_5()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTableDepth625(StringBuilder sql) {
        sql.append("-- Table: t_depth_elv625\n");
        List<Depth625Model> data = dbHelper.getAllDepth625();
        for (Depth625Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_depth_elv625 (id_depth, id_pengukuran, hv_1, hv_2, hv_3, created_at, updated_at) VALUES (")
                    .append(item.getId_depth()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTableDepth600(StringBuilder sql) {
        sql.append("-- Table: t_depth_elv600\n");
        List<Depth600Model> data = dbHelper.getAllDepth600();
        for (Depth600Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_depth_elv600 (id_depth, id_pengukuran, hv_1, hv_2, hv_3, hv_4, hv_5, created_at, updated_at) VALUES (")
                    .append(item.getId_depth()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append(item.getHv_4()).append(", ")
                    .append(item.getHv_5()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTableInitial625(StringBuilder sql) {
        sql.append("-- Table: m_initial_reading_elv_625\n");
        List<Initial625Model> data = dbHelper.getAllInitial625();
        for (Initial625Model item : data) {
            sql.append("INSERT OR REPLACE INTO m_initial_reading_elv_625 (id_initial_reading, id_pengukuran, hv_1, hv_2, hv_3, created_at, updated_at) VALUES (")
                    .append(item.getId_initial_reading()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTableInitial600(StringBuilder sql) {
        sql.append("-- Table: m_initial_reading_elv_600\n");
        List<Initial600Model> data = dbHelper.getAllInitial600();
        for (Initial600Model item : data) {
            sql.append("INSERT OR REPLACE INTO m_initial_reading_elv_600 (id_initial_reading, id_pengukuran, hv_1, hv_2, hv_3, hv_4, hv_5, created_at, updated_at) VALUES (")
                    .append(item.getId_initial_reading()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append(item.getHv_4()).append(", ")
                    .append(item.getHv_5()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTablePergerakan625(StringBuilder sql) {
        sql.append("-- Table: t_pergerakan_elv625\n");
        List<Pergerakan625Model> data = dbHelper.getAllPergerakan625();
        for (Pergerakan625Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_pergerakan_elv625 (id_pergerakan, id_pengukuran, hv_1, hv_2, hv_3, created_at, updated_at) VALUES (")
                    .append(item.getId_pergerakan()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    private void exportTablePergerakan600(StringBuilder sql) {
        sql.append("-- Table: t_pergerakan_elv600\n");
        List<Pergerakan600Model> data = dbHelper.getAllPergerakan600();
        for (Pergerakan600Model item : data) {
            sql.append("INSERT OR REPLACE INTO t_pergerakan_elv600 (id_pergerakan, id_pengukuran, hv_1, hv_2, hv_3, hv_4, hv_5, created_at, updated_at) VALUES (")
                    .append(item.getId_pergerakan()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getHv_1()).append(", ")
                    .append(item.getHv_2()).append(", ")
                    .append(item.getHv_3()).append(", ")
                    .append(item.getHv_4()).append(", ")
                    .append(item.getHv_5()).append(", ")
                    .append("datetime('now'), datetime('now'));\n");
        }
        sql.append("\n");
    }

    // ==================== AMBANG BATAS EXPORT METHODS ====================

    private void exportTableAmbangBatas625H1(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_625_h1\n");
        List<AmbangBatas625H1Model> data = dbHelper.getAllAmbangBatas625H1();
        for (AmbangBatas625H1Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_625_h1 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas625H2(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_625_h2\n");
        List<AmbangBatas625H2Model> data = dbHelper.getAllAmbangBatas625H2();
        for (AmbangBatas625H2Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_625_h2 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas625H3(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_625_h3\n");
        List<AmbangBatas625H3Model> data = dbHelper.getAllAmbangBatas625H3();
        for (AmbangBatas625H3Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_625_h3 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas600H1(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_600_h1\n");
        List<AmbangBatas600H1Model> data = dbHelper.getAllAmbangBatas600H1();
        for (AmbangBatas600H1Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_600_h1 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas600H2(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_600_h2\n");
        List<AmbangBatas600H2Model> data = dbHelper.getAllAmbangBatas600H2();
        for (AmbangBatas600H2Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_600_h2 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas600H3(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_600_h3\n");
        List<AmbangBatas600H3Model> data = dbHelper.getAllAmbangBatas600H3();
        for (AmbangBatas600H3Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_600_h3 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas600H4(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_600_h4\n");
        List<AmbangBatas600H4Model> data = dbHelper.getAllAmbangBatas600H4();
        for (AmbangBatas600H4Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_600_h4 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private void exportTableAmbangBatas600H5(StringBuilder sql) {
        sql.append("-- Table: ambang_batas_600_h5\n");
        List<AmbangBatas600H5Model> data = dbHelper.getAllAmbangBatas600H5();
        for (AmbangBatas600H5Model item : data) {
            sql.append("INSERT OR REPLACE INTO ambang_batas_600_h5 (id_ambang_batas, id_pengukuran, aman, peringatan, bahaya, pergerakan, created_at, updated_at) VALUES (")
                    .append(item.getId_ambang_batas()).append(", ")
                    .append(item.getId_pengukuran()).append(", ")
                    .append(item.getAman()).append(", ")
                    .append(item.getPeringatan()).append(", ")
                    .append(item.getBahaya()).append(", ")
                    .append(item.getPergerakan()).append(", ")
                    .append(quote(item.getCreated_at())).append(", ")
                    .append(quote(item.getUpdated_at())).append(");\n");
        }
        sql.append("\n");
    }

    private String quote(String value) {
        if (value == null) return "NULL";
        return "'" + value.replace("'", "''") + "'";
    }

    private void resetExportButton() {
        runOnUiThread(() -> {
            progressBarExport.setVisibility(View.GONE);
            btnExportDB.setEnabled(true);
            btnExportDB.setText("Export Database ke SQL");
        });
    }

    private void showExportSuccessDialog(File exportedFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Ekspor Berhasil")
                .setMessage("Database berhasil diekspor ke file SQL:\n" + exportedFile.getName() + "\n\nLokasi: Downloads/")
                .setPositiveButton("BAGIKAN", (dialog, which) -> shareSQLFile(exportedFile))
                .setNegativeButton("OK", null)
                .show();
    }

    private void shareSQLFile(File sqlFile) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            Uri fileUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", sqlFile);

            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DamBody Database Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "File ekspor database DamBody dalam format SQL");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Bagikan File SQL"));
        } catch (Exception e) {
            Toast.makeText(this, "❌ Gagal membagikan file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}