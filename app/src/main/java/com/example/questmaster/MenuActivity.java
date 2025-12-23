package com.example.questmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setTitle("Главное меню");

        Button btnRooms = findViewById(R.id.btnRooms);
        Button btnMasters = findViewById(R.id.btnMasters);
        Button btnBooking = findViewById(R.id.btnBooking);
        Button btnReports = findViewById(R.id.btnReports);
        Button btnExit = findViewById(R.id.btnExit);

        btnRooms.setOnClickListener(v -> {
            startActivity(new Intent(this, RoomsListActivity.class));
        });

        btnMasters.setOnClickListener(v -> {
            startActivity(new Intent(this, MastersListActivity.class));
        });

        btnBooking.setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarActivity.class));
        });

        btnReports.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportsActivity.class));
        });

        btnExit.setOnClickListener(v -> finishAffinity());
    }
}