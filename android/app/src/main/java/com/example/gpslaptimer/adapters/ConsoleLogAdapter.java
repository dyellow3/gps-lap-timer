package com.example.gpslaptimer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpslaptimer.R;

import java.util.List;

public class ConsoleLogAdapter extends RecyclerView.Adapter<ConsoleLogAdapter.ConsoleLogViewHolder> {

    private List<String> logMessages;

    public ConsoleLogAdapter(List<String> logMessages) {
        this.logMessages = logMessages;
    }

    @NonNull
    @Override
    public ConsoleLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_console_log, parent, false);
        return new ConsoleLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConsoleLogViewHolder holder, int position) {
        String logMessage = logMessages.get(position);
        holder.textViewLog.setText(logMessage);
    }

    @Override
    public int getItemCount() {
        return logMessages.size();
    }

    public static class ConsoleLogViewHolder extends RecyclerView.ViewHolder {
        TextView textViewLog;

        public ConsoleLogViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewLog = itemView.findViewById(R.id.textViewLog);
        }
    }

    public void addMessage(String message) {
        logMessages.add(message);
        notifyItemInserted(logMessages.size() - 1);
    }

}
