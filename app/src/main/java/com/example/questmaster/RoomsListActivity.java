package com.example.questmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomsListActivity extends AppCompatActivity {

    private ListView listView;
    private RoomListAdapter adapter;
    private List<RoomEntity> rooms = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private TextView tvTitle;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms_list);

        setTitle("Квест-комнаты");

        database = AppDatabase.getDatabase(this);
        listView = findViewById(R.id.listViewRooms);
        tvTitle = findViewById(R.id.tvTitle);
        btnBack = findViewById(R.id.backButton);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        tvTitle.setText("Квест-комнаты");

        loadRooms();

        adapter = new RoomListAdapter(this, rooms);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            RoomEntity room = rooms.get(position);
            Intent intent = new Intent(RoomsListActivity.this, RoomEditActivity.class);
            intent.putExtra("room_id", room.id);
            startActivityForResult(intent, 1);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            RoomEntity room = rooms.get(position);
            showDeleteDialog(room);
            return true;
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(RoomsListActivity.this, RoomEditActivity.class);
            startActivityForResult(intent, 1);
        });

        // Кнопка Назад (ImageView)
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadRooms() {
        executorService.execute(() -> {
            List<RoomEntity> loadedRooms = database.dao().getAllRooms();
            runOnUiThread(() -> {
                rooms.clear();
                rooms.addAll(loadedRooms);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showDeleteDialog(RoomEntity room) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удаление комнаты")
                .setMessage("Удалить комнату \"" + room.name + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteRoom(room))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteRoom(RoomEntity room) {
        executorService.execute(() -> {
            database.dao().deleteRoom(room);
            runOnUiThread(() -> {
                loadRooms();
                Toast.makeText(this, "Комната удалена", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadRooms();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}