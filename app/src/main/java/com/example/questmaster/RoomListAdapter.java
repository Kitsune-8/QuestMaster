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

public class RoomListAdapter extends ArrayAdapter<RoomEntity> {

    public RoomListAdapter(Context context, List<RoomEntity> rooms) {
        super(context, 0, rooms);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        RoomEntity room = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_room, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvRoomNumber = convertView.findViewById(R.id.tvRoomNumber);
        TextView tvGenre = convertView.findViewById(R.id.tvGenre);
        TextView tvPrice = convertView.findViewById(R.id.tvPrice);
        TextView tvStatus = convertView.findViewById(R.id.tvStatus);
        TextView tvMaxPlayers = convertView.findViewById(R.id.tvMaxPlayers);

        tvName.setText(room.name);
        tvRoomNumber.setText("Комната: " + room.roomNumber);
        tvGenre.setText(room.genre);
        tvPrice.setText(String.format("%,.0f ₽", room.price));
        tvMaxPlayers.setText("Макс игроков: " + room.maxPlayers);

        if (room.isActive) {
            tvStatus.setText("Активна");
            tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        } else {
            tvStatus.setText("Неактивна");
            tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
        }

        return convertView;
    }
}