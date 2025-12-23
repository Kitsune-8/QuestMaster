package com.example.questmaster;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingEditActivity extends AppCompatActivity {

    private EditText etClientName, etClientPhone, etPlayersCount, etPricePerPerson,
            etDate, etStartTime, etEndTime, etPaidAmount, etNotes;
    private Spinner spinnerRoom, spinnerMaster, spinnerStatus;
    private Button btnSelectDate, btnSelectStartTime, btnSelectEndTime, btnSave, btnCancel;
    private ImageView btnBack;
    private TextView tvTitle;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private int bookingId = -1;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private List<RoomEntity> rooms = new ArrayList<>();
    private List<MasterEntity> masters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_edit);

        database = AppDatabase.getDatabase(this);
        initViews();

        setupStatusSpinner();
        loadRoomsAndMasters();
        setupListeners();

        bookingId = getIntent().getIntExtra("booking_id", -1);
        if (bookingId != -1) {
            tvTitle.setText("Редактировать бронирование");
            loadBookingData();
        } else {
            tvTitle.setText("Новое бронирование");

            // Устанавливаем значения по умолчанию из intent
            long dateMillis = getIntent().getLongExtra("date", System.currentTimeMillis());
            calendar.setTimeInMillis(dateMillis);
            etDate.setText(dateFormat.format(calendar.getTime()));

            int startHour = getIntent().getIntExtra("start_hour", 10);
            int endHour = getIntent().getIntExtra("end_hour", 12);

            etStartTime.setText(String.format("%02d:00", startHour));
            etEndTime.setText(String.format("%02d:00", endHour));

            // Устанавливаем цену по умолчанию
            etPricePerPerson.setText("2000");
        }

        btnSave.setOnClickListener(v -> saveBooking());
        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etClientName = findViewById(R.id.etClientName);
        etClientPhone = findViewById(R.id.etClientPhone);
        etPlayersCount = findViewById(R.id.etPlayersCount);
        etPricePerPerson = findViewById(R.id.etPricePerPerson);
        etDate = findViewById(R.id.etDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etPaidAmount = findViewById(R.id.etPaidAmount);
        etNotes = findViewById(R.id.etNotes);

        spinnerRoom = findViewById(R.id.spinnerRoom);
        spinnerMaster = findViewById(R.id.spinnerMaster);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectStartTime = findViewById(R.id.btnSelectStartTime);
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.backButton);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void loadRoomsAndMasters() {
        executorService.execute(() -> {
            rooms.addAll(database.dao().getActiveRooms());
            masters.addAll(database.dao().getActiveMasters());

            runOnUiThread(() -> {
                setupRoomSpinner();
                setupMasterSpinner();
            });
        });
    }

    private void setupRoomSpinner() {
        List<String> roomNames = new ArrayList<>();
        for (RoomEntity room : rooms) {
            roomNames.add(room.name + " (Комната " + room.roomNumber + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, roomNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoom.setAdapter(adapter);
    }

    private void setupMasterSpinner() {
        List<String> masterNames = new ArrayList<>();
        masterNames.add("Не назначен");
        for (MasterEntity master : masters) {
            masterNames.add(master.fullName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, masterNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaster.setAdapter(adapter);
    }

    private void setupStatusSpinner() {
        String[] statuses = {"ожидание", "подтверждена", "завершена", "отменена"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectStartTime.setOnClickListener(v -> showTimePicker(true));
        btnSelectEndTime.setOnClickListener(v -> showTimePicker(false));
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    etDate.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar timeCal = Calendar.getInstance();
        try {
            String currentTime = isStartTime ? etStartTime.getText().toString() : etEndTime.getText().toString();
            if (!currentTime.isEmpty()) {
                Date time = timeFormat.parse(currentTime);
                if (time != null) {
                    timeCal.setTime(time);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    if (isStartTime) {
                        etStartTime.setText(time);
                    } else {
                        etEndTime.setText(time);
                    }
                },
                timeCal.get(Calendar.HOUR_OF_DAY),
                timeCal.get(Calendar.MINUTE),
                true)
                .show();
    }

    private void loadBookingData() {
        executorService.execute(() -> {
            BookingEntity booking = database.dao().getBookingById(bookingId);
            if (booking != null) {
                runOnUiThread(() -> {
                    etClientName.setText(booking.clientName);
                    etClientPhone.setText(booking.clientPhone);
                    etPlayersCount.setText(String.valueOf(booking.playersCount));
                    etPricePerPerson.setText(String.valueOf(booking.pricePerPerson));

                    if (booking.bookingDate != null) {
                        calendar.setTime(booking.bookingDate);
                        etDate.setText(dateFormat.format(booking.bookingDate));
                    }

                    etStartTime.setText(booking.startTime);
                    etEndTime.setText(booking.endTime);
                    etPaidAmount.setText(String.valueOf(booking.paidAmount));
                    etNotes.setText(booking.notes);

                    setSpinnerSelections(booking);
                });
            }
        });
    }

    private void setSpinnerSelections(BookingEntity booking) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id == booking.roomId) {
                spinnerRoom.setSelection(i);
                break;
            }
        }

        if (booking.masterId != null) {
            for (int i = 0; i < masters.size(); i++) {
                if (masters.get(i).id == booking.masterId) {
                    spinnerMaster.setSelection(i + 1);
                    break;
                }
            }
        }

        String[] statuses = {"ожидание", "подтверждена", "завершена", "отменена"};
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(booking.status)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }
    }

    private void saveBooking() {
        String clientName = etClientName.getText().toString().trim();
        String clientPhone = etClientPhone.getText().toString().trim();
        String playersCountStr = etPlayersCount.getText().toString().trim();
        String pricePerPersonStr = etPricePerPerson.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String paidAmountStr = etPaidAmount.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String status = (String) spinnerStatus.getSelectedItem();

        int roomIndex = spinnerRoom.getSelectedItemPosition();

        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            Toast.makeText(this, "Выберите комнату", Toast.LENGTH_SHORT).show();
            return;
        }

        int masterIndex = spinnerMaster.getSelectedItemPosition();

        if (clientName.isEmpty() || clientPhone.isEmpty() || playersCountStr.isEmpty() ||
                pricePerPersonStr.isEmpty() || dateStr.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка времени: конец должен быть позже начала
        try {
            Date startDate = timeFormat.parse(startTime);
            Date endDate = timeFormat.parse(endTime);
            if (!endDate.after(startDate)) {
                Toast.makeText(this, "Время окончания должно быть позже времени начала", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка в формате времени", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int playersCount = Integer.parseInt(playersCountStr);
            double pricePerPerson = Double.parseDouble(pricePerPersonStr);
            double paidAmount = paidAmountStr.isEmpty() ? 0 : Double.parseDouble(paidAmountStr);

            Date bookingDate = dateFormat.parse(dateStr);
            int roomId = rooms.get(roomIndex).id;
            Integer masterId = (masterIndex > 0) ? masters.get(masterIndex - 1).id : null;

            // Парсим время для вычисления часов
            Date startDate = timeFormat.parse(startTime);
            Date endDate = timeFormat.parse(endTime);
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(startDate);
            endCal.setTime(endDate);

            int startHour = startCal.get(Calendar.HOUR_OF_DAY);
            int endHour = endCal.get(Calendar.HOUR_OF_DAY);

            // Проверяем пересечение времени
            executorService.execute(() -> {
                // Используем существующий метод getOverlappingBookings
                List<BookingEntity> overlappingBookings = database.dao().getOverlappingBookings(
                        bookingDate, roomId, startTime, endTime);

                // Если редактируем существующее бронирование, исключаем его из проверки
                if (bookingId != -1) {
                    overlappingBookings.removeIf(b -> b.id == bookingId);
                }

                if (!overlappingBookings.isEmpty()) {
                    runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(BookingEditActivity.this);
                        builder.setTitle("Конфликт времени")
                                .setMessage("В это время комната уже занята. Выберите другое время.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                    return;
                }

                // Сохраняем бронирование
                BookingEntity booking = new BookingEntity();
                booking.roomId = roomId;
                booking.masterId = masterId;
                booking.clientName = clientName;
                booking.clientPhone = clientPhone;
                booking.playersCount = playersCount;
                booking.bookingDate = bookingDate;
                booking.startTime = startTime;
                booking.endTime = endTime;
                booking.startHour = startHour;
                booking.endHour = endHour;
                booking.pricePerPerson = pricePerPerson;
                booking.totalAmount = playersCount * pricePerPerson;
                booking.paidAmount = paidAmount;
                booking.status = status;
                booking.notes = notes;

                if (bookingId != -1) {
                    booking.id = bookingId;
                    database.dao().updateBooking(booking);
                } else {
                    database.dao().insertBooking(booking);
                }

                runOnUiThread(() -> {
                    Toast.makeText(BookingEditActivity.this,
                            bookingId != -1 ? "Бронирование обновлено" : "Бронирование создано",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            });

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}