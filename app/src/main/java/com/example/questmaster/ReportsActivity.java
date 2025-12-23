package com.example.questmaster;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {

    private Spinner spinnerPeriod;
    private TextView tvTotalRevenue, tvTotalSessions, tvAveragePrice, tvBestDay, tvTitle;
    private ListView listViewDetails;
    private Button btnGenerate;
    private ImageView btnBack;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Устанавливаем заголовок через ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Отчёты");
        }

        database = AppDatabase.getDatabase(this);
        initViews();
        setupSpinner();
    }

    private void initViews() {
        spinnerPeriod = findViewById(R.id.spinnerPeriod);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvAveragePrice = findViewById(R.id.tvAveragePrice);
        tvBestDay = findViewById(R.id.tvBestDay);
        tvTitle = findViewById(R.id.tvTitle);
        listViewDetails = findViewById(R.id.listViewDetails);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnBack = findViewById(R.id.backButton);

        tvTitle.setText("Отчёты");

        btnGenerate.setOnClickListener(v -> generateReport());
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        List<String> periods = new ArrayList<>();
        periods.add("За сегодня");
        periods.add("За неделю");
        periods.add("За месяц");
        periods.add("За год");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
    }

    private void generateReport() {
        String period = spinnerPeriod.getSelectedItem().toString();
        Calendar calendar = Calendar.getInstance();
        final Date endDate = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        final Date startDate;

        switch (period) {
            case "За сегодня":
                startDate = calendar.getTime();
                break;
            case "За неделю":
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                startDate = calendar.getTime();
                break;
            case "За месяц":
                calendar.add(Calendar.MONTH, -1);
                startDate = calendar.getTime();
                break;
            case "За год":
                calendar.add(Calendar.YEAR, -1);
                startDate = calendar.getTime();
                break;
            default:
                startDate = calendar.getTime();
        }

        executorService.execute(() -> {
            // Загружаем все бронирования за период
            List<BookingEntity> bookings = database.dao().getBookingsByDateRange(startDate, endDate);

            // Вычисляем статистику вручную
            final double[] revenue = {0}; // Используем массив для обхода final ограничения
            final int[] sessionsCount = {0}; // Используем массив для обхода final ограничения
            List<BookingEntity> completedBookings = new ArrayList<>();

            for (BookingEntity booking : bookings) {
                if ("завершена".equals(booking.status)) {
                    revenue[0] += booking.totalAmount;
                    sessionsCount[0]++;
                    completedBookings.add(booking);
                }
            }

            // Вычисляем лучший день
            final String bestDay = calculateBestDay(completedBookings);

            runOnUiThread(() -> {
                updateReportUI(revenue[0], sessionsCount[0], completedBookings, bestDay);
            });
        });
    }

    private String calculateBestDay(List<BookingEntity> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return "Нет данных";
        }

        // Группируем по дням и считаем выручку
        Map<String, Double> dailyRevenue = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));

        for (BookingEntity booking : bookings) {
            if ("завершена".equals(booking.status)) {
                String day = dayFormat.format(booking.bookingDate);
                dailyRevenue.put(day, dailyRevenue.getOrDefault(day, 0.0) + booking.totalAmount);
            }
        }

        if (dailyRevenue.isEmpty()) {
            return "Нет завершённых сеансов";
        }

        // Находим день с максимальной выручкой
        String bestDay = "";
        double maxRevenue = 0;

        for (Map.Entry<String, Double> entry : dailyRevenue.entrySet()) {
            if (entry.getValue() > maxRevenue) {
                maxRevenue = entry.getValue();
                bestDay = entry.getKey();
            }
        }

        return bestDay + " (" + String.format("%,.0f", maxRevenue) + " ₽)";
    }

    private void updateReportUI(double revenue, int sessionsCount, List<BookingEntity> bookings, String bestDay) {
        tvTotalRevenue.setText(String.format("%,.0f ₽", revenue));
        tvTotalSessions.setText(sessionsCount + " сеансов");
        tvBestDay.setText(bestDay != null ? bestDay : "Нет данных");

        List<String> details = new ArrayList<>();
        double totalCompletedRevenue = 0;
        int completedSessions = 0;

        if (bookings != null) {
            for (BookingEntity booking : bookings) {
                if ("завершена".equals(booking.status)) {
                    totalCompletedRevenue += booking.totalAmount;
                    completedSessions++;

                    String detail = String.format("%s %s - %s - %d чел. - %,.0f₽",
                            dateFormat.format(booking.bookingDate),
                            booking.startTime,
                            booking.clientName,
                            booking.playersCount,
                            booking.totalAmount);
                    details.add(detail);
                }
            }

            if (completedSessions > 0) {
                double averageCheck = totalCompletedRevenue / completedSessions;
                tvAveragePrice.setText(String.format("%,.0f ₽", averageCheck));
            } else {
                tvAveragePrice.setText("0 ₽");
            }
        } else {
            tvAveragePrice.setText("0 ₽");
        }

        if (details.isEmpty()) {
            details.add("Нет завершённых сеансов за период");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, details);
        listViewDetails.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}