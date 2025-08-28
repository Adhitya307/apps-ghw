package com.example.kerjapraktik;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private Spinner spinnerPengukuran;
    private Button btnTampilkanData;

    // CardView
    private CardView cardTma, cardThomson, cardSR, cardBocoran;

    // TMA
    private TextView tvTmaValue;

    // Thomson
    private TextView tvA1R, tvA1L, tvB1, tvB3, tvB5;

    // SR
    private LinearLayout containerSR;

    // Bocoran
    private LinearLayout containerBocoran;

    private RequestQueue requestQueue;
    private ArrayList<Integer> pengukuranIds = new ArrayList<>();
    private ArrayList<String> pengukuranLabels = new ArrayList<>();

    private static final String BASE_URL = "http://192.168.1.10/API_Android/public/api/rembesan/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        requestQueue = Volley.newRequestQueue(this);

        spinnerPengukuran = findViewById(R.id.spinnerPengukuran);
        btnTampilkanData = findViewById(R.id.btnTampilkanData);

        cardTma = findViewById(R.id.cardTma);
        cardThomson = findViewById(R.id.cardThomson);
        cardSR = findViewById(R.id.cardSR);
        cardBocoran = findViewById(R.id.cardBocoran);

        // init
        tvTmaValue = findViewById(R.id.tvTmaValue);
        tvA1R = findViewById(R.id.tvA1R);
        tvA1L = findViewById(R.id.tvA1L);
        tvB1 = findViewById(R.id.tvB1);
        tvB3 = findViewById(R.id.tvB3);
        tvB5 = findViewById(R.id.tvB5);

        containerSR = findViewById(R.id.containerSR);
        containerBocoran = findViewById(R.id.containerBocoran);

        loadPengukuranList();

        btnTampilkanData.setOnClickListener(v -> {
            int pos = spinnerPengukuran.getSelectedItemPosition();
            if (pos >= 0 && pos < pengukuranIds.size()) {
                int pengukuranId = pengukuranIds.get(pos);
                tampilkanData(pengukuranId);
            }
        });
    }

    private void loadPengukuranList() {
        String url = BASE_URL + "pengukuran";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        pengukuranIds.clear();
                        pengukuranLabels.clear();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            int id = obj.getInt("id");
                            String tgl = obj.getString("tanggal");
                            pengukuranIds.add(id);

                            // langsung tanggal saja
                            pengukuranLabels.add(tgl);
                        }

                        android.widget.ArrayAdapter<String> spinnerAdapter =
                                new android.widget.ArrayAdapter<>(this,
                                        android.R.layout.simple_spinner_item, pengukuranLabels);
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerPengukuran.setAdapter(spinnerAdapter);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace());
        requestQueue.add(request);
    }

    private void tampilkanData(int pengukuranId) {
        String url = BASE_URL + "detail/" + pengukuranId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Hide semua dulu
                        cardTma.setVisibility(View.GONE);
                        cardThomson.setVisibility(View.GONE);
                        cardSR.setVisibility(View.GONE);
                        cardBocoran.setVisibility(View.GONE);

                        JSONObject data = response.getJSONObject("data");

                        // TMA
                        if (data.has("tma")) {
                            cardTma.setVisibility(View.VISIBLE);
                            String tmaValue = data.getString("tma");
                            tvTmaValue.setText(tmaValue);
                        }

                        // Thomson
                        if (data.has("thomson")) {
                            cardThomson.setVisibility(View.VISIBLE);
                            JSONObject th = data.getJSONObject("thomson");
                            tvA1R.setText("A1R: " + th.optString("a1_r", "--"));
                            tvA1L.setText("A1L: " + th.optString("a1_l", "--"));
                            tvB1.setText("B1: " + th.optString("b1", "--"));
                            tvB3.setText("B3: " + th.optString("b3", "--"));
                            tvB5.setText("B5: " + th.optString("b5", "--"));
                        }

                        // SR
                        if (data.has("sr")) {
                            cardSR.setVisibility(View.VISIBLE);
                            JSONArray srArr = data.getJSONArray("sr");
                            containerSR.removeAllViews(); // clear

                            // Header
                            LinearLayout headerLayout = new LinearLayout(this);
                            headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                            headerLayout.setPadding(0, 0, 0, 16);

                            TextView headerNama = createHeaderTextView("SR", 1.5f);
                            TextView headerKode = createHeaderTextView("Kode", 1f);
                            TextView headerNilai = createHeaderTextView("Nilai (detik)", 1f);

                            headerLayout.addView(headerNama);
                            headerLayout.addView(headerKode);
                            headerLayout.addView(headerNilai);

                            containerSR.addView(headerLayout);

                            // Separator
                            View separator = new View(this);
                            separator.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 2));
                            separator.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                            containerSR.addView(separator);

                            for (int i = 0; i < srArr.length(); i++) {
                                JSONObject srObj = srArr.getJSONObject(i);
                                String nama  = srObj.optString("nama", "SR" + (i + 1));
                                String kode  = srObj.optString("kode", "-");
                                String nilai = srObj.optString("nilai", "-");

                                LinearLayout rowLayout = new LinearLayout(this);
                                rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                                rowLayout.setPadding(0, 12, 0, 12);

                                if (i % 2 == 0) {
                                    rowLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
                                } else {
                                    rowLayout.setBackgroundColor(getResources().getColor(R.color.row_alternate_color));
                                }

                                TextView tvNama = createDataTextView(nama, 1.5f);
                                TextView tvKode = createDataTextView(kode, 1f);
                                TextView tvNilai = createDataTextView(nilai, 1f);

                                rowLayout.addView(tvNama);
                                rowLayout.addView(tvKode);
                                rowLayout.addView(tvNilai);

                                containerSR.addView(rowLayout);
                            }
                        }

                        // Bocoran (tabel)
                        if (data.has("bocoran")) {
                            cardBocoran.setVisibility(View.VISIBLE);
                            JSONObject boc = data.getJSONObject("bocoran");
                            containerBocoran.removeAllViews();

                            // Header
                            LinearLayout headerLayout = new LinearLayout(this);
                            headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                            headerLayout.setPadding(0, 0, 0, 16);

                            TextView headerNama = createHeaderTextView("Titik", 1.5f);
                            TextView headerNilai = createHeaderTextView("Nilai (m)", 1f);
                            TextView headerKode = createHeaderTextView("Kode", 1f);

                            headerLayout.addView(headerNama);
                            headerLayout.addView(headerNilai);
                            headerLayout.addView(headerKode);

                            containerBocoran.addView(headerLayout);

                            addBocoranRow(containerBocoran, "ELV 624 T1",
                                    boc.optString("elv_624_t1", "--"),
                                    boc.optString("elv_624_t1_kode", "-"));

                            addBocoranRow(containerBocoran, "ELV 615 T2",
                                    boc.optString("elv_615_t2", "--"),
                                    boc.optString("elv_615_t2_kode", "-"));

                            addBocoranRow(containerBocoran, "PIPA P1",
                                    boc.optString("pipa_p1", "--"),
                                    boc.optString("pipa_p1_kode", "-"));

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace());
        requestQueue.add(request);
    }

    private void addBocoranRow(LinearLayout container, String nama, String nilai, String kode) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView tvNama = createDataTextView(nama, 1.5f);
        TextView tvNilai = createDataTextView(nilai, 1f);
        TextView tvKode = createDataTextView(kode, 1f);

        row.addView(tvNama);
        row.addView(tvNilai);
        row.addView(tvKode);

        container.addView(row);
    }

    private TextView createHeaderTextView(String text, float weight) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setBackgroundColor(getResources().getColor(R.color.primary_color));
        textView.setPadding(8, 12, 8, 12);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    private TextView createDataTextView(String text, float weight) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }
}
