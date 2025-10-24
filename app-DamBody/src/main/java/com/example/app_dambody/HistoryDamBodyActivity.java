package com.example.app_dambody;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.ArrayList;
import java.util.List;

public class HistoryDamBodyActivity extends AppCompatActivity {

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData, btnExportDB;
    private TextView tvData625, tvData600;
    private CardView cardELV625, cardELV600;
    private DatabaseHelper dbHelper;
    private List<PengukuranModel> pengukuranList;

    private int selectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_dambody);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnTampilkanData = findViewById(R.id.btnTampilkanData);
        btnExportDB = findViewById(R.id.btnExportDB);
        tvData625 = findViewById(R.id.tvData625);
        tvData600 = findViewById(R.id.tvData600);
        cardELV625 = findViewById(R.id.cardELV625);
        cardELV600 = findViewById(R.id.cardELV600);

        dbHelper = new DatabaseHelper(this);

        loadSpinnerData();

        btnTampilkanData.setOnClickListener(v -> {
            if (selectedId == -1) {
                Toast.makeText(this, "⚠️ Pilih data pengukuran dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            tampilkanData(selectedId);
        });

        btnExportDB.setOnClickListener(v ->
                Toast.makeText(this, "📦 Export database belum diaktifkan", Toast.LENGTH_SHORT).show());
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
            sb625.append("📊 Data ELV625\n\n");

            // tampilkan pembacaan
            sb625.append("📊 PEMBACAAN:\n");
            if (!baca625.isEmpty()) {
                for (Pembacaan625Model b : baca625) {
                    sb625.append("HV1=").append(b.getHv_1())
                            .append("  HV2=").append(b.getHv_2())
                            .append("  HV3=").append(b.getHv_3())
                            .append("\n");
                }
            } else {
                sb625.append("- Tidak ada data pembacaan\n");
            }

            // tampilkan pergerakan
            sb625.append("\n📈 PERGERAKAN:\n");
            if (!gerak625.isEmpty()) {
                for (Pergerakan625Model m : gerak625) {
                    sb625.append("HV1=").append(m.getHv_1())
                            .append("  HV2=").append(m.getHv_2())
                            .append("  HV3=").append(m.getHv_3())
                            .append("\n");
                }
            } else {
                sb625.append("- Tidak ada data pergerakan\n");
            }

        } else {
            cardELV625.setVisibility(View.GONE);
        }

        // ======== ELV600 ========
        if (!baca600.isEmpty() || !gerak600.isEmpty()) {
            cardELV600.setVisibility(View.VISIBLE);
            sb600.append("📈 Data ELV600\n\n");

            // tampilkan pembacaan
            sb600.append("📊 PEMBACAAN:\n");
            if (!baca600.isEmpty()) {
                for (Pembacaan600Model b : baca600) {
                    sb600.append("HV1=").append(b.getHv_1())
                            .append("  HV2=").append(b.getHv_2())
                            .append("  HV3=").append(b.getHv_3())
                            .append("  HV4=").append(b.getHv_4())
                            .append("  HV5=").append(b.getHv_5())
                            .append("\n");
                }
            } else {
                sb600.append("- Tidak ada data pembacaan\n");
            }

            // tampilkan pergerakan
            sb600.append("\n📈 PERGERAKAN:\n");
            if (!gerak600.isEmpty()) {
                for (Pergerakan600Model m : gerak600) {
                    sb600.append("HV1=").append(m.getHv_1())
                            .append("  HV2=").append(m.getHv_2())
                            .append("  HV3=").append(m.getHv_3())
                            .append("  HV4=").append(m.getHv_4())
                            .append("  HV5=").append(m.getHv_5())
                            .append("\n");
                }
            } else {
                sb600.append("- Tidak ada data pergerakan\n");
            }

        } else {
            cardELV600.setVisibility(View.GONE);
        }

        tvData625.setText(sb625.toString());
        tvData600.setText(sb600.toString());
    }

}
