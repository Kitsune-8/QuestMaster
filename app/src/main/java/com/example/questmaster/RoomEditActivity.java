package com.example.questmaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomEditActivity extends AppCompatActivity {

    private EditText etName, etRoomNumber, etMaxPlayers, etGenre, etPrice, etDuration, etDescription;
    private CheckBox cbActive;
    private Button btnSave;
    private Button btnCancel;
    private ImageView btnBack;
    private TextView tvTitle;
    private int roomId = -1;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_edit);

        database = AppDatabase.getDatabase(this);
        initViews();

        roomId = getIntent().getIntExtra("room_id", -1);
        if (roomId != -1) {
            tvTitle.setText("Редактировать комнату");
            loadRoomData();
        } else {
            tvTitle.setText("Добавить комнату");
        }

        btnSave.setOnClickListener(v -> saveRoom());
        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etRoomNumber = findViewById(R.id.etRoomNumber);
        etMaxPlayers = findViewById(R.id.etMaxPlayers);
        etGenre = findViewById(R.id.etGenre);
        etPrice = findViewById(R.id.etPrice);
        etDuration = findViewById(R.id.etDuration);
        etDescription = findViewById(R.id.etDescription);
        cbActive = findViewById(R.id.cbActive);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.backButton);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void loadRoomData() {
        executorService.execute(() -> {
            RoomEntity room = database.dao().getRoomById(roomId);
            if (room != null) {
                runOnUiThread(() -> {
                    etName.setText(room.name);
                    etRoomNumber.setText(room.roomNumber);
                    etMaxPlayers.setText(String.valueOf(room.maxPlayers));
                    etGenre.setText(room.genre);
                    etPrice.setText(String.valueOf(room.price));
                    etDuration.setText(String.valueOf(room.durationHours));
                    etDescription.setText(room.description);
                    cbActive.setChecked(room.isActive);
                });
            }
        });
    }

    private void saveRoom() {
        String name = etName.getText().toString().trim();
        String roomNumber = etRoomNumber.getText().toString().trim();
        String maxPlayersStr = etMaxPlayers.getText().toString().trim();
        String genre = etGenre.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        boolean isActive = cbActive.isChecked();

        if (name.isEmpty() || roomNumber.isEmpty() || maxPlayersStr.isEmpty() ||
                genre.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int maxPlayers = Integer.parseInt(maxPlayersStr);
            double price = Double.parseDouble(priceStr);
            double duration = Double.parseDouble(durationStr);

            RoomEntity room = new RoomEntity();
            room.name = name;
            room.roomNumber = roomNumber;
            room.maxPlayers = maxPlayers;
            room.genre = genre;
            room.price = price;
            room.durationHours = duration;
            room.description = description;
            room.isActive = isActive;

            executorService.execute(() -> {
                if (roomId != -1) {
                    room.id = roomId;
                    database.dao().updateRoom(room);
                } else {
                    database.dao().insertRoom(room);
                }

                runOnUiThread(() -> {
                    Toast.makeText(RoomEditActivity.this,
                            roomId != -1 ? "Комната обновлена" : "Комната добавлена",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректные числовые значения", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}