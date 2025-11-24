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
import com.example.app_dambody.DamBodyHomeActivity;
import com.apps.bubbletilt.BubbleTiltHomeActivity;
import com.example.app.exstenso.ExstensoHomeActivity;
import com.example.app_leftpiezo.HomeLeftPiezoActivity;
import com.example.app_rightpiezo.HomeRightPiezoActivity;

public class WelcomeActivity extends AppCompatActivity {

    private View badgeContainer;
    private TextView welcomeText, subtitleText, sectionTitle;
    private LinearLayout statusIndicator;
    private CardView cardMonitoringRembesan, cardMonitoringDamBody, cardBubbleTiltMeter, cardRodExtensoMeter, cardPiezometerKiri, cardPiezometerKanan;
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
        cardMonitoringDamBody = findViewById(R.id.cardMonitoringTeknis);
        cardBubbleTiltMeter = findViewById(R.id.cardBubbleTiltMeter);
        cardRodExtensoMeter = findViewById(R.id.cardRodExtensoMeter);
        cardPiezometerKiri = findViewById(R.id.cardPiezometerKiri);
        cardPiezometerKanan = findViewById(R.id.cardPiezometerKanan); // TAMBAHAN BARU
    }

    private void startStaggeredAnimation() {
        resetViews();

        handler.postDelayed(() -> {
            animateBadge();
        }, 300);

        handler.postDelayed(() -> {
            animateView(welcomeText, 400);
            animateView(subtitleText, 400);
        }, 600);

        handler.postDelayed(() -> {
            animateView(statusIndicator, 300);
        }, 900);

        handler.postDelayed(() -> {
            animateView(sectionTitle, 400);
        }, 1100);

        handler.postDelayed(() -> {
            animateCardView(cardMonitoringRembesan, 0);
            animateCardView(cardMonitoringDamBody, 100);
            animateCardView(cardBubbleTiltMeter, 200);
            animateCardView(cardRodExtensoMeter, 300);
            animateCardView(cardPiezometerKiri, 400);
            animateCardView(cardPiezometerKanan, 500); // TAMBAHAN BARU
        }, 1300);
    }

    private void resetViews() {
        View[] views = {badgeContainer, welcomeText, subtitleText, statusIndicator, sectionTitle,
                cardMonitoringRembesan, cardMonitoringDamBody, cardBubbleTiltMeter, cardRodExtensoMeter,
                cardPiezometerKiri, cardPiezometerKanan}; // TAMBAHAN BARU

        for (View view : views) {
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(20f);
            }
        }

        if (badgeContainer != null) {
            badgeContainer.setScaleX(0.8f);
            badgeContainer.setScaleY(0.8f);
        }
    }

    private void animateBadge() {
        if (badgeContainer != null) {
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
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        cardMonitoringDamBody.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, DamBodyHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        cardBubbleTiltMeter.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, BubbleTiltHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        cardRodExtensoMeter.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, ExstensoHomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        cardPiezometerKiri.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, HomeLeftPiezoActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });

        // TAMBAHAN BARU: Piezometer Kanan
        cardPiezometerKanan.setOnClickListener(v -> {
            animateClick(v, () -> {
                Intent intent = new Intent(WelcomeActivity.this, HomeRightPiezoActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
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