package com.fivevoltslab.flamecontrol;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


/**
 * Created by anupamchugh on 09/02/16.
 */
public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<BluetoothDevice> dataSet;
    Context mContext;
    int total_types;
    private OnClickListenerScanDevice onClickListener;


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView deviceNameTextView, macAddressTextView;
        ImageView deleteImageView;
        OnClickListenerScanDevice onClickListener;

        public ViewHolder(View itemView, OnClickListenerScanDevice onClickListener) {
            super(itemView);
            this.deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            this.macAddressTextView = itemView.findViewById(R.id.macAddressTextView);
            this.onClickListener = onClickListener;
            itemView.setOnClickListener(this);
//            this.deleteImageView = itemView.findViewById(R.id.deleteImgView);
        }


        @Override
        public void onClick(View view) {
            onClickListener.onItemClick(getAdapterPosition());
        }


    }


    public DeviceAdapter(List<BluetoothDevice> data, Context context, OnClickListenerScanDevice onClickListener) {
        this.dataSet = data;
        this.mContext = context;
        this.onClickListener = onClickListener;
        total_types = dataSet.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_adapter_layout,
                parent, false);
        return new ViewHolder(v, onClickListener);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int listPosition) {

        try {

//            Product product = dataSet.get(listPosition);
            ((ViewHolder) holder).deviceNameTextView.setText(dataSet.get(listPosition).getName());
            Log.i("NAME", dataSet.get(listPosition).getName());
            Log.i("ADDRESS", dataSet.get(listPosition).getAddress());
            ((ViewHolder) holder).macAddressTextView.setText(dataSet.get(listPosition).getAddress());

//            byte[] image = product.getImage();
//            Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
//                ((ViewHolder) holder).imageView.setImageBitmap(Bitmap.createScaledBitmap(bmp, 100,
//                        100, false));
//            ((ViewHolder) holder).imageView.setImageBitmap(bmp);


        } catch (Exception e) {

        }

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public interface OnClickListenerScanDevice {
        void onItemClick(int position);
    }
}
