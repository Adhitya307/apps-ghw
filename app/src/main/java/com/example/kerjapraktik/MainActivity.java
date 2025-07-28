package com.example.kerjapraktik;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FrameLayout splashLayout;
    private ImageView logoSplash;
    private TextView splashText;
    private LinearLayout dotLoader;
    private View dot1, dot2, dot3;
    private Handler handler = new Handler();

    private Drawable[] gradients;
    private int currentGradient = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashLayout = findViewById(R.id.splashLayout);
        logoSplash = findViewById(R.id.logoSplash);
        splashText = findViewById(R.id.splashText);
        dotLoader = findViewById(R.id.dotLoader);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        gradients = new Drawable[] {
                getResources().getDrawable(R.drawable.gradient1),
                getResources().getDrawable(R.drawable.gradient2),
                getResources().getDrawable(R.drawable.gradient3)
        };

        startBackgroundTransition();
        startSplashAnimations();
    }

    private void startBackgroundTransition() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Drawable oldDrawable = gradients[currentGradient];
                currentGradient = (currentGradient + 1) % gradients.length;
                Drawable newDrawable = gradients[currentGradient];

                TransitionDrawable transition = new TransitionDrawable(new Drawable[]{oldDrawable, newDrawable});
                splashLayout.setBackground(transition);
                transition.startTransition(2000);

                handler.postDelayed(this, 4000); // repeat every 4 sec
            }
        }, 0);
    }

    private void startSplashAnimations() {
        logoSplash.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> handler.postDelayed(this::showText, 400))
                .start();
    }

    private void showText() {
        splashText.animate().alpha(1f).setDuration(600).start();
        typeWriterAnimation("PT INDONESIA POWER", 0);
        handler.postDelayed(this::startDotLoading, 1400);
    }

    private void typeWriterAnimation(String text, int index) {
        if (index <= text.length()) {
            splashText.setText(text.substring(0, index));
            handler.postDelayed(() -> typeWriterAnimation(text, index + 1), 70);
        }
    }

    private void startDotLoading() {
        dotLoader.animate().alpha(1f).setDuration(400).start();
        animateDots();
    }

    private void animateDots() {
        dot1.animate().scaleX(1.4f).scaleY(1.4f).setDuration(300).withEndAction(() -> {
            dot1.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
            dot2.animate().scaleX(1.4f).scaleY(1.4f).setDuration(300).withEndAction(() -> {
                dot2.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
                dot3.animate().scaleX(1.4f).scaleY(1.4f).setDuration(300).withEndAction(() -> {
                    dot3.animate().scaleX(1f).scaleY(1f).setDuration(300).withEndAction(this::animateDots).start();
                }).start();
            }).start();
        }).start();

        handler.postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        }, 5000);
    }
}
