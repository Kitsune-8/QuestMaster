package com.example.questmaster;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class WeekScheduleAdapter extends BaseAdapter {

    private Context context;
    private List<WeekScheduleActivity.WeekTimeSlot> timeSlots;
    private List<String> days;

    public WeekScheduleAdapter(Context context, List<WeekScheduleActivity.WeekTimeSlot> timeSlots, List<String> days) {
        this.context = context;
        this.timeSlots = timeSlots;
        this.days = days;
    }

    public void setDays(List<String> days) {
        this.days = days;
    }

    @Override
    public int getCount() {
        return timeSlots.size();
    }

    @Override
    public Object getItem(int position) {
        return timeSlots.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WeekScheduleActivity.WeekTimeSlot slot = timeSlots.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_week_slot, parent, false);
        }

        TextView tvSlot = convertView.findViewById(R.id.tvSlot);

        if (slot.isHeader) {
            tvSlot.setText(slot.text);
            tvSlot.setTextColor(Color.parseColor("#4A148C"));
            tvSlot.setBackgroundColor(Color.parseColor("#E0F7FA"));
        } else if (slot.isTimeSlot) {
            if (slot.isBooked && slot.booking != null) {
                tvSlot.setText(slot.text);
                tvSlot.setBackgroundColor(Color.parseColor("#E1BEE7")); // Нежно-фиолетовый
                tvSlot.setTextColor(Color.parseColor("#4A148C"));
            } else {
                tvSlot.setText("•");
                tvSlot.setBackgroundColor(Color.parseColor("#FFF3E0")); // Бежевый
                tvSlot.setTextColor(Color.parseColor("#EF6C00"));
            }
        }

        return convertView;
    }
}