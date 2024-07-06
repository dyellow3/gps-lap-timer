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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<String> lapList;
    private OnItemClickListener itemClickListener;

    public HistoryAdapter(List<String> lapList, OnItemClickListener itemClickListener) {
        this.lapList = lapList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String lapFileName = lapList.get(position);
        holder.bind(lapFileName);
    }

    @Override
    public int getItemCount() {
        return lapList.size();
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView historyTextView;
        private Button deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            historyTextView = itemView.findViewById(R.id.textViewLap);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(String lapFileName) {
            historyTextView.setText(lapFileName);
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