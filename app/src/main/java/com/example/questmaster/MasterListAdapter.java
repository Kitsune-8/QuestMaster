package com.example.questmaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class MasterListAdapter extends ArrayAdapter<MasterEntity> {

    public MasterListAdapter(Context context, List<MasterEntity> masters) {
        super(context, 0, masters);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MasterEntity master = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_master, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvPhone = convertView.findViewById(R.id.tvPhone);
        TextView tvExperience = convertView.findViewById(R.id.tvExperience);
        TextView tvSpecialization = convertView.findViewById(R.id.tvSpecialization);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);

        tvName.setText(master.fullName);
        tvPhone.setText(master.phone);
        tvExperience.setText("Стаж: " + master.experienceYears + " лет");
        tvSpecialization.setText(master.questsSpecialization);

        if (master.isActive) {
            tvStatus.setText("Активен");
            tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        } else {
            tvStatus.setText("Неактивен");
            tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
        }

        return convertView;
    }
}