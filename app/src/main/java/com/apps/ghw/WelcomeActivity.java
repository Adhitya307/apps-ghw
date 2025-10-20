package com.apps.ghw;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.apps.ghw.rembesan.HomeActivity;

public class WelcomeActivity extends AppCompatActivity {

    private View badgeContainer;
    private TextView welcomeText, subtitleText, sectionTitle;
    private LinearLayout statusIndicator;
    private CardView cardMonitoringRembesan, cardMonitoringTeknis, cardLaporan, cardPengaturan;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initViews();
        startStaggeredAnimation();
        setupMenuActions();
    }

    private void initViews() {
        badgeContainer = findViewById(R.id.badgeContainer);
        welcomeText = findViewById(R.id.welcomeText);
        subtitleText = findViewById(R.id.subtitleText);
        statusIndicator = findViewById(R.id.statusIndicator);
        sectionTitle = findViewById(R.id.sectionTitle);

        cardMonitoringRembesan = findViewById(R.id.cardMonitoringRembesan);
        cardMonitoringTeknis = findViewById(R.id.cardMonitoringTeknis);
        cardLaporan = findViewById(R.id.cardLaporan);
        cardPengaturan = findViewById(R.id.cardPengaturan);
    }

    private void startStaggeredAnimation() {
        // Reset semua view ke state awal
        resetViews();

        // Animasi bertahap dengan sequence yang lebih baik
        handler.postDelayed(() -> {
            // Stage 1: Logo badge dengan scale animation
            animateBadge();
        }, 300);

        handler.postDelayed(() -> {
            // Stage 2: Text elements
            animateView(welcomeText, 400);
            animateView(subtitleText, 400);
        }, 600);

        handler.postDelayed(() -> {
            // Stage 3: Status indicator
            animateView(statusIndicator, 300);
        }, 900);

        handler.postDelayed(() -> {
            // Stage 4: Section title
            animateView(sectionTitle, 400);
        }, 1100);

        handler.postDelayed(() -> {
            // Stage 5: Menu cards dengan stagger
            animateCardView(cardMonitoringRembesan, 0);
            animateCardView(cardMonitoringTeknis, 100);
            animateCardView(cardLaporan, 200);
            animateCardView(cardPengaturan, 300);
        }, 1300);
    }

    private void resetViews() {
        // Reset semua view ke state awal
        View[] views = {badgeContainer, welcomeText, subtitleText, statusIndicator, sectionTitle,
                cardMonitoringRembesan, cardMonitoringTeknis, cardLaporan, cardPengaturan};

        for (View view : views) {
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(20f);
            }
        }

        // Reset khusus untuk badge container
        if (badgeContainer != null) {
            badgeContainer.setScaleX(0.8f);
            badgeContainer.setScaleY(0.8f);
        }
    }

    private void animateBadge() {
        if (badgeContainer != null) {
            // Scale animation untuk badge
            badgeContainer.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(600)
                    .start();
        }
    }

    private void animateView(View view, int duration) {
        if (view != null) {
            view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(duration)
                    .start();
        }
    }

    private void animateCardView(CardView cardView, int delay) {
        if (cardView != null) {
            handler.postDelayed(() -> {
                cardView.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(500)
                        .start();
            }, delay);
        }
    }

    private void setupMenuActions() {
        cardMonitoringRembesan.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
                startActivity(intent);
                // Optional: tambahkan transition animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        cardMonitoringTeknis.setOnClickListener(v -> {
            animateClick(v, () -> showToast("Modul ini - Dalam Pengembangan"));
        });

        cardLaporan.setOnClickListener(v -> {
            animateClick(v, () -> showToast("Modul ini  - Dalam Pengembangan"));
        });

        cardPengaturan.setOnClickListener(v -> {
            animateClick(v, () -> showToast("Modul ini - Dalam Pengembangan"));
        });
    }

    private void animateClick(View view, Runnable action) {
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}