package com.example.gpslaptimer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpslaptimer.R;
import com.example.gpslaptimer.ui.connect.ConnectFragment;

import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {
    private List<ConnectFragment.BluetoothDeviceModel> deviceList;
    private OnItemClickListener itemClickListener;

    public BluetoothDeviceAdapter(List<ConnectFragment.BluetoothDeviceModel> deviceList, OnItemClickListener itemClickListener) {
        this.deviceList = deviceList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        ConnectFragment.BluetoothDeviceModel device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceAddress.setText(device.getHardwareAddress());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(itemClickListener != null) {
                    itemClickListener.onItemClick(device);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void setDeviceList(List<ConnectFragment.BluetoothDeviceModel> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddress = itemView.findViewById(R.id.deviceAddress);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ConnectFragment.BluetoothDeviceModel device);
    }
}