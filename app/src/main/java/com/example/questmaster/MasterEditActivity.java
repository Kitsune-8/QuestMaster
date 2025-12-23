package com.example.questmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterEditActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etEmail, etExperience, etQuests, etNotes;
    private CheckBox cbActive;
    private Button btnSave;
    private Button btnCancel;
    private ImageView btnBack;
    private TextView tvTitle;
    private int masterId = -1;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_edit);

        database = AppDatabase.getDatabase(this);
        initViews();

        masterId = getIntent().getIntExtra("master_id", -1);
        if (masterId != -1) {
            tvTitle.setText("Редактировать мастера");
            loadMasterData();
        } else {
            tvTitle.setText("Добавить мастера");
        }

        btnSave.setOnClickListener(v -> saveMaster());
        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etExperience = findViewById(R.id.etExperience);
        etQuests = findViewById(R.id.etQuests);
        etNotes = findViewById(R.id.etNotes);
        cbActive = findViewById(R.id.cbActive);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.backButton);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void loadMasterData() {
        executorService.execute(() -> {
            MasterEntity master = database.dao().getMasterById(masterId);
            if (master != null) {
                runOnUiThread(() -> {
                    etFullName.setText(master.fullName);
                    etPhone.setText(master.phone);
                    etEmail.setText(master.email);
                    etExperience.setText(String.valueOf(master.experienceYears));
                    etQuests.setText(master.questsSpecialization);
                    etNotes.setText(master.notes);
                    cbActive.setChecked(master.isActive);
                });
            }
        });
    }

    private void saveMaster() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String experienceStr = etExperience.getText().toString().trim();
        String quests = etQuests.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        boolean isActive = cbActive.isChecked();

        if (fullName.isEmpty() || phone.isEmpty() || experienceStr.isEmpty()) {
            Toast.makeText(this, "Заполните ФИО, телефон и стаж", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double experience = Double.parseDouble(experienceStr);

            MasterEntity master = new MasterEntity();
            master.fullName = fullName;
            master.phone = phone;
            master.email = email;
            master.experienceYears = experience;
            master.questsSpecialization = quests;
            master.notes = notes;
            master.isActive = isActive;

            executorService.execute(() -> {
                if (masterId != -1) {
                    master.id = masterId;
                    database.dao().updateMaster(master);
                } else {
                    database.dao().insertMaster(master);
                }

                runOnUiThread(() -> {
                    Toast.makeText(MasterEditActivity.this,
                            masterId != -1 ? "Мастер обновлен" : "Мастер добавлен",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректное значение стажа", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}