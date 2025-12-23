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

public class MastersListActivity extends AppCompatActivity {

    private ListView listView;
    private MasterListAdapter adapter;
    private List<MasterEntity> masters = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private TextView tvTitle;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masters_list);

        setTitle("Квест-мастера");

        database = AppDatabase.getDatabase(this);
        listView = findViewById(R.id.listViewMasters);
        tvTitle = findViewById(R.id.tvTitle);
        btnBack = findViewById(R.id.backButton);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        tvTitle.setText("Квест-мастера");

        loadMasters();

        adapter = new MasterListAdapter(this, masters);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            MasterEntity master = masters.get(position);
            Intent intent = new Intent(MastersListActivity.this, MasterEditActivity.class);
            intent.putExtra("master_id", master.id);
            startActivityForResult(intent, 1);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            MasterEntity master = masters.get(position);
            showDeleteDialog(master);
            return true;
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MastersListActivity.this, MasterEditActivity.class);
            startActivityForResult(intent, 1);
        });

        // Кнопка Назад
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMasters() {
        executorService.execute(() -> {
            List<MasterEntity> loadedMasters = database.dao().getAllMasters();
            runOnUiThread(() -> {
                masters.clear();
                masters.addAll(loadedMasters);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showDeleteDialog(MasterEntity master) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удаление мастера")
                .setMessage("Удалить мастера \"" + master.fullName + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteMaster(master))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteMaster(MasterEntity master) {
        executorService.execute(() -> {
            database.dao().deleteMaster(master);
            runOnUiThread(() -> {
                loadMasters();
                Toast.makeText(this, "Мастер удален", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadMasters();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}