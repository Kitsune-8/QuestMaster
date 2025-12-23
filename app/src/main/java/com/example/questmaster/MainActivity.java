package com.example.questmaster;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ждем 3 секунды и переходим в меню
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
        }, 3000);
    }
}