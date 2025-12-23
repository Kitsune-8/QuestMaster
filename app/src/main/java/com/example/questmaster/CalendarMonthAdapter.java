package com.example.questmaster;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class CalendarMonthAdapter extends BaseAdapter {

    private Context context;
    private List<CalendarActivity.CalendarDay> days;

    public CalendarMonthAdapter(Context context, List<CalendarActivity.CalendarDay> days) {
        this.context = context;
        this.days = days;
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public Object getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CalendarActivity.CalendarDay day = days.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        }

        TextView tvDay = convertView.findViewById(R.id.tvDay);
        TextView tvBookingCount = convertView.findViewById(R.id.tvBookingCount);
        View todayIndicator = convertView.findViewById(R.id.todayIndicator);

        if (day.isHeader) {
            tvDay.setText(day.text);
            tvDay.setTextColor(Color.parseColor("#4A148C"));
            tvDay.setBackgroundColor(Color.parseColor("#E0F7FA"));
            tvBookingCount.setVisibility(View.GONE);
            todayIndicator.setVisibility(View.GONE);
        } else if (day.isEmpty) {
            tvDay.setText("");
            tvDay.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tvBookingCount.setVisibility(View.GONE);
            todayIndicator.setVisibility(View.GONE);
        } else {
            tvDay.setText(String.valueOf(day.dayNumber));

            if (day.isToday) {
                todayIndicator.setVisibility(View.VISIBLE);
                tvDay.setBackgroundColor(Color.parseColor("#FFF3E0"));
            } else if (day.bookingsCount > 0) {
                tvDay.setBackgroundColor(Color.parseColor("#E1BEE7"));
                tvBookingCount.setText(String.valueOf(day.bookingsCount));
                tvBookingCount.setVisibility(View.VISIBLE);
                todayIndicator.setVisibility(View.GONE);
            } else {
                tvDay.setBackgroundColor(Color.WHITE);
                tvBookingCount.setVisibility(View.GONE);
                todayIndicator.setVisibility(View.GONE);
            }

            tvDay.setTextColor(day.isCurrentMonth ? Color.parseColor("#333333") : Color.parseColor("#CCCCCC"));
        }

        return convertView;
    }
}