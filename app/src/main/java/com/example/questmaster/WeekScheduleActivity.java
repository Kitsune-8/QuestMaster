package com.example.questmaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeekScheduleActivity extends AppCompatActivity {

    private GridView gridViewWeek;
    private TextView tvWeekRange;
    private TextView tvTitle;
    private Button btnPrevWeek;
    private Button btnNextWeek;
    private ImageView btnBack;
    private Calendar currentWeek = Calendar.getInstance();
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("ru"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private List<RoomEntity> rooms = new ArrayList<>();
    private List<MasterEntity> masters = new ArrayList<>();

    private List<WeekTimeSlot> timeSlots = new ArrayList<>();
    private WeekScheduleAdapter adapter;

    private final int START_HOUR = 8;
    private final int END_HOUR = 22;
    private final int SLOT_DURATION = 2;
    private final int HOURS_PER_DAY = (END_HOUR - START_HOUR) / SLOT_DURATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week_schedule);

        setTitle("Недельное расписание");
        database = AppDatabase.getDatabase(this);

        // Получаем дату из intent или используем текущую
        long dateMillis = getIntent().getLongExtra("date", System.currentTimeMillis());
        currentWeek.setTimeInMillis(dateMillis);
        currentWeek.setFirstDayOfWeek(Calendar.MONDAY);
        currentWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        initViews();
        setupListeners();
        loadRooms();
    }

    private void initViews() {
        gridViewWeek = findViewById(R.id.gridViewWeek);
        tvWeekRange = findViewById(R.id.tvWeekRange);
        tvTitle = findViewById(R.id.tvTitle);
        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        btnBack = findViewById(R.id.backButton);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        tvTitle.setText("Недельное расписание");

        fabAdd.setOnClickListener(v -> {
            // Создание нового бронирования без конкретного времени
            Intent intent = new Intent(WeekScheduleActivity.this, BookingEditActivity.class);
            intent.putExtra("date", currentWeek.getTimeInMillis());
            startActivityForResult(intent, 1);
        });

        adapter = new WeekScheduleAdapter(this, timeSlots, getDaysOfWeek());
        gridViewWeek.setAdapter(adapter);

        updateWeekHeader();
    }

    private void setupListeners() {
        btnPrevWeek.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekHeader();
            generateTimeSlots();
            loadBookingsForWeek();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekHeader();
            generateTimeSlots();
            loadBookingsForWeek();
        });

        btnBack.setOnClickListener(v -> finish());

        gridViewWeek.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WeekTimeSlot slot = timeSlots.get(position);
                if (slot.booking != null) {
                    showBookingDetails(slot.booking);
                } else if (slot.isTimeSlot) {
                    showCreateBookingDialog(slot);
                }
            }
        });
    }

    private void loadRooms() {
        executorService.execute(() -> {
            rooms.clear();
            rooms.addAll(database.dao().getActiveRooms());

            masters.clear();
            masters.addAll(database.dao().getActiveMasters());

            runOnUiThread(() -> {
                generateTimeSlots();
                loadBookingsForWeek();
            });
        });
    }

    private void generateTimeSlots() {
        timeSlots.clear();

        Calendar dayCal = getStartOfWeek(currentWeek);
        List<String> days = new ArrayList<>();

        // Пустая ячейка в левом верхнем углу
        WeekTimeSlot emptySlot = new WeekTimeSlot();
        emptySlot.isHeader = true;
        emptySlot.text = "";
        timeSlots.add(emptySlot);

        // Заголовки дней недели
        for (int i = 0; i < 7; i++) {
            WeekTimeSlot dayHeader = new WeekTimeSlot();
            dayHeader.isHeader = true;
            dayHeader.text = dayFormat.format(dayCal.getTime()) + "\n" +
                    dayCal.get(Calendar.DAY_OF_MONTH);
            dayHeader.date = dayCal.getTime();
            timeSlots.add(dayHeader);

            days.add(dayFormat.format(dayCal.getTime()) + "\n" + dayCal.get(Calendar.DAY_OF_MONTH));
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Временные слоты
        dayCal.setTime(getStartOfWeek(currentWeek).getTime());

        for (int hourSlot = 0; hourSlot < HOURS_PER_DAY; hourSlot++) {
            int hour = START_HOUR + (hourSlot * SLOT_DURATION);

            // Заголовок времени
            WeekTimeSlot timeHeader = new WeekTimeSlot();
            timeHeader.isHeader = true;
            timeHeader.text = String.format("%02d:00", hour);
            timeSlots.add(timeHeader);

            // Ячейки для каждого дня недели
            for (int day = 0; day < 7; day++) {
                WeekTimeSlot slot = new WeekTimeSlot();
                slot.isTimeSlot = true;
                slot.isClickable = true;
                slot.date = dayCal.getTime();
                slot.hour = hour;
                slot.text = "";
                slot.isBooked = false;
                slot.booking = null;

                timeSlots.add(slot);
                dayCal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Сбрасываем день для следующего временного слота
            dayCal.setTime(getStartOfWeek(currentWeek).getTime());
        }

        adapter.setDays(days);
        adapter.notifyDataSetChanged();
        gridViewWeek.setNumColumns(8);
    }

    private void loadBookingsForWeek() {
        executorService.execute(() -> {
            Calendar startOfWeek = getStartOfWeek(currentWeek);
            Calendar endOfWeek = getEndOfWeek(startOfWeek);

            List<BookingEntity> bookings = database.dao().getBookingsByDateRange(
                    startOfWeek.getTime(), endOfWeek.getTime());

            runOnUiThread(() -> {
                // Сбрасываем статусы всех слотов
                for (WeekTimeSlot slot : timeSlots) {
                    if (slot.isTimeSlot) {
                        slot.isBooked = false;
                        slot.booking = null;
                        slot.text = "";
                    }
                }

                // Заполняем слоты бронированиями
                for (BookingEntity booking : bookings) {
                    if ("отменена".equals(booking.status)) continue;

                    int startHour = Integer.parseInt(booking.startTime.substring(0, 2));

                    for (WeekTimeSlot slot : timeSlots) {
                        if (slot.isTimeSlot && slot.hour == startHour &&
                                isSameDay(slot.date, booking.bookingDate)) {
                            slot.isBooked = true;
                            slot.booking = booking;
                            slot.text = booking.clientName;
                            break;
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            });
        });
    }

    private Calendar getStartOfWeek(Calendar calendar) {
        Calendar start = (Calendar) calendar.clone();
        int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
        int daysToMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        start.add(Calendar.DAY_OF_MONTH, -daysToMonday);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        return start;
    }

    private Calendar getEndOfWeek(Calendar startOfWeek) {
        Calendar end = (Calendar) startOfWeek.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        return end;
    }

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private void updateWeekHeader() {
        Calendar startOfWeek = getStartOfWeek(currentWeek);
        Calendar endOfWeek = getEndOfWeek(startOfWeek);

        tvWeekRange.setText(dateFormat.format(startOfWeek.getTime()) + " - " +
                dateFormat.format(endOfWeek.getTime()));
    }

    private List<String> getDaysOfWeek() {
        List<String> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(getStartOfWeek(currentWeek).getTime());

        for (int i = 0; i < 7; i++) {
            days.add(dayFormat.format(calendar.getTime()) + "\n" + calendar.get(Calendar.DAY_OF_MONTH));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    private void showBookingDetails(BookingEntity booking) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String roomName = getRoomName(booking.roomId);
        String masterName = "Не назначен";
        if (booking.masterId != null) {
            for (MasterEntity master : masters) {
                if (master.id == booking.masterId) {
                    masterName = master.fullName;
                    break;
                }
            }
        }

        String details = "Клиент: " + booking.clientName + "\n" +
                "Телефон: " + booking.clientPhone + "\n" +
                "Комната: " + roomName + "\n" +
                "Мастер: " + masterName + "\n" +
                "Дата: " + dateFormat.format(booking.bookingDate) + "\n" +
                "Время: " + booking.startTime + " - " + booking.endTime + "\n" +
                "Игроков: " + booking.playersCount + "\n" +
                "Сумма: " + String.format("%,.0f", booking.totalAmount) + " ₽\n" +
                "Статус: " + booking.status + "\n" +
                "Заметки: " + (booking.notes != null ? booking.notes : "нет");

        builder.setTitle("Детали бронирования")
                .setMessage(details)
                .setPositiveButton("Редактировать", (dialog, which) -> {
                    Intent intent = new Intent(WeekScheduleActivity.this, BookingEditActivity.class);
                    intent.putExtra("booking_id", booking.id);
                    startActivityForResult(intent, 2);
                })
                .setNeutralButton("Отменить бронь", (dialog, which) -> {
                    showCancelBookingDialog(booking);
                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void showCancelBookingDialog(BookingEntity booking) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Отмена бронирования")
                .setMessage("Отменить бронирование для " + booking.clientName + "?")
                .setPositiveButton("Отменить", (dialog, which) -> cancelBooking(booking))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showCreateBookingDialog(WeekTimeSlot slot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создание бронирования")
                .setMessage("Время " + String.format("%02d:00", slot.hour) +
                        " на " + dateFormat.format(slot.date) + " свободно.\n" +
                        "Создать бронирование?")
                .setPositiveButton("Да", (dialog, which) -> {
                    createNewBooking(slot);
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void createNewBooking(WeekTimeSlot slot) {
        Intent intent = new Intent(WeekScheduleActivity.this, BookingEditActivity.class);
        intent.putExtra("date", slot.date.getTime());
        intent.putExtra("start_hour", slot.hour);
        intent.putExtra("end_hour", slot.hour + SLOT_DURATION);
        startActivityForResult(intent, 1);
    }

    private void cancelBooking(BookingEntity booking) {
        executorService.execute(() -> {
            booking.status = "отменена";
            database.dao().updateBooking(booking);

            runOnUiThread(() -> {
                Toast.makeText(WeekScheduleActivity.this, "Бронирование отменено", Toast.LENGTH_SHORT).show();
                loadBookingsForWeek();
            });
        });
    }

    private String getRoomName(int roomId) {
        for (RoomEntity room : rooms) {
            if (room.id == roomId) {
                return room.name;
            }
        }
        return "Комната " + roomId;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            loadBookingsForWeek();
            Toast.makeText(this, "Расписание обновлено", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    public static class WeekTimeSlot {
        public boolean isHeader = false;
        public boolean isTimeSlot = false;
        public boolean isClickable = false;
        public boolean isBooked = false;
        public String text = "";
        public Date date;
        public int hour;
        public BookingEntity booking;
    }
}