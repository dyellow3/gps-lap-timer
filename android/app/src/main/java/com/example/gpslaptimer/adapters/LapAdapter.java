package com.example.gpslaptimer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpslaptimer.R;

import java.util.List;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapViewHolder> {
    private List<String> lapList;
    private OnItemClickListener itemClickListener;

    public LapAdapter(List<String> lapList, OnItemClickListener itemClickListener) {
        this.lapList = lapList;
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
        String lapFileName = lapList.get(position);
        holder.bind(lapFileName);
    }

    @Override
    public int getItemCount() {
        return lapList.size();
    }

    public class LapViewHolder extends RecyclerView.ViewHolder {
        private TextView lapTextView;
        private Button deleteButton;

        public LapViewHolder(@NonNull View itemView) {
            super(itemView);
            lapTextView = itemView.findViewById(R.id.textViewLap);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(String lapFileName) {
            lapTextView.setText(lapFileName);
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(lapFileName);
                }
            });
            deleteButton.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onDeleteClick(lapFileName, getAdapterPosition());
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(String lapFileName);
        void onDeleteClick(String lapFileName, int position);
    }
}