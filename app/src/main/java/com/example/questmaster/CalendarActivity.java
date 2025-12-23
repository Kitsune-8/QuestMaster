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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarActivity extends AppCompatActivity {

    private GridView gridViewCalendar;
    private TextView tvMonthYear;
    private TextView tvTitle;
    private Button btnPrev;
    private Button btnNext;
    private ImageView btnBack;
    private Button btnWeekView;
    private Calendar currentDate = Calendar.getInstance();
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ru"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private List<CalendarDay> days = new ArrayList<>();
    private CalendarMonthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_month);
        setTitle("Календарь бронирований");
        database = AppDatabase.getDatabase(this);

        initViews();
        setupListeners();
        updateCalendar();
    }

    private void initViews() {
        gridViewCalendar = findViewById(R.id.gridViewCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvTitle = findViewById(R.id.tvTitle);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.backButton);
        btnWeekView = findViewById(R.id.btnWeekView);

        tvTitle.setText("Календарь (месяц)");

        adapter = new CalendarMonthAdapter(this, days);
        gridViewCalendar.setAdapter(adapter);
    }

    private void setupListeners() {
        btnPrev.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNext.setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        btnBack.setOnClickListener(v -> finish());

        btnWeekView.setOnClickListener(v -> {
            // Переходим в недельный вид на текущую неделю
            Intent intent = new Intent(CalendarActivity.this, WeekScheduleActivity.class);
            intent.putExtra("date", currentDate.getTimeInMillis());
            startActivity(intent);
        });

        gridViewCalendar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CalendarDay day = days.get(position);
                if (day.isCurrentMonth && !day.isEmpty) {
                    // Переходим в недельный вид на выбранный день
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.setTime(day.date);

                    Intent intent = new Intent(CalendarActivity.this, WeekScheduleActivity.class);
                    intent.putExtra("date", selectedDate.getTimeInMillis());
                    startActivity(intent);
                }
            }
        });
    }

    private void updateCalendar() {
        tvMonthYear.setText(monthFormat.format(currentDate.getTime()));
        generateCalendarDays();
        loadBookingsForMonth();
    }

    private void generateCalendarDays() {
        days.clear();

        Calendar firstDayOfMonth = Calendar.getInstance();
        firstDayOfMonth.setTime(currentDate.getTime());
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - Calendar.MONDAY;

        // Заголовки дней недели
        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String day : daysOfWeek) {
            CalendarDay header = new CalendarDay();
            header.isHeader = true;
            header.text = day;
            days.add(header);
        }

        // Пустые ячейки в начале месяца
        for (int i = 0; i < offset; i++) {
            CalendarDay emptyDay = new CalendarDay();
            emptyDay.isEmpty = true;
            days.add(emptyDay);
        }

        // Дни месяца
        int daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();

        for (int day = 1; day <= daysInMonth; day++) {
            CalendarDay calendarDay = new CalendarDay();
            calendarDay.dayNumber = day;
            calendarDay.isCurrentMonth = true;

            Calendar dayCal = Calendar.getInstance();
            dayCal.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), day);
            calendarDay.date = dayCal.getTime();

            calendarDay.isToday = (dayCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    dayCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    dayCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));

            days.add(calendarDay);
        }

        // Пустые ячейки в конце месяца
        int totalCells = days.size();
        int remainingCells = 42 - totalCells; // 6 строк * 7 дней
        for (int i = 0; i < remainingCells; i++) {
            CalendarDay emptyDay = new CalendarDay();
            emptyDay.isEmpty = true;
            days.add(emptyDay);
        }

        adapter.notifyDataSetChanged();
    }

    private void loadBookingsForMonth() {
        executorService.execute(() -> {
            Calendar startDate = Calendar.getInstance();
            startDate.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), 1);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);

            Calendar endDate = Calendar.getInstance();
            endDate.set(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH),
                    startDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);

            List<BookingEntity> bookings = database.dao().getBookingsByDateRange(
                    startDate.getTime(), endDate.getTime());

            runOnUiThread(() -> {
                // Обновляем счетчики бронирований для каждого дня
                for (CalendarDay day : days) {
                    if (day.isCurrentMonth && !day.isEmpty) {
                        day.bookingsCount = 0;
                        for (BookingEntity booking : bookings) {
                            if (!"отменена".equals(booking.status) &&
                                    isSameDay(day.date, booking.bookingDate)) {
                                day.bookingsCount++;
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            });
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    public static class CalendarDay {
        public boolean isHeader = false;
        public boolean isEmpty = false;
        public boolean isCurrentMonth = false;
        public boolean isToday = false;
        public int dayNumber;
        public int bookingsCount = 0;
        public Date date;
        public String text = "";
    }
}