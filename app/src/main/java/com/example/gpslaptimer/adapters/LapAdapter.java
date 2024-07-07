package com.example.gpslaptimer.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpslaptimer.R;
import com.example.gpslaptimer.models.Lap;

import java.util.ArrayList;
import java.util.List;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapViewHolder> {

    private List<Lap> laps;
    private OnItemClickListener itemClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public LapAdapter(List<Lap> laps, OnItemClickListener itemClickListener) {
        this.laps = laps != null ? laps : new ArrayList<>();
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public LapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lap, parent, false);
        return new LapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LapViewHolder holder, int position) {
        Lap lap = laps.get(position);
        holder.lapNumber.setText(String.valueOf(lap.getLapNumber()));
        holder.lapTime.setText(formatTime(lap.getLapTime()));

        double variation = lap.getVariation();
        holder.variation.setText("+" + String.format("%.3f", variation));
        holder.variation.setTextColor(variation <= 0 ? Color.GREEN : Color.RED);

        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.border_selected);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.border);
        }

        holder.bind(String.valueOf(lap.getLapNumber()));
    }

    @Override
    public int getItemCount() {
        return laps.size();
    }

    public class LapViewHolder extends RecyclerView.ViewHolder {
        TextView lapNumber;
        TextView lapTime;
        TextView variation;

        public LapViewHolder(@NonNull View itemView) {
            super(itemView);
            lapNumber = itemView.findViewById(R.id.textViewLapNumber);
            lapTime = itemView.findViewById(R.id.textViewLapTime);
            variation = itemView.findViewById(R.id.textViewGap);
        }

        public void bind(String lapNumber) {
            itemView.setOnClickListener(v -> {
                if(itemClickListener != null) {
                    int oldPosition = selectedPosition;
                    selectedPosition = getAdapterPosition();
                    notifyItemChanged(oldPosition);
                    notifyItemChanged(selectedPosition);
                    itemClickListener.onItemClick(lapNumber);
                }
            });
        }

    }

    private String formatTime(double timeInSeconds) {
        int minutes = (int) (timeInSeconds / 60);
        double seconds = timeInSeconds % 60;
        return String.format("%d:%06.3f", minutes, seconds);
    }

    public interface OnItemClickListener {
        void onItemClick(String lapNumber);
    }
}
