package com.example.bluetoothfactest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rk.commonlib.CommonBaseFragment;
import com.rk.commonlib.bluetooth.BluetoothDeviceHolder;
import com.rk.commonlib.bluetooth.CourtUnitBluetoothInstance;
import com.rk.commonlib.util.LogUtils;
import com.rk.commonlib.widge.RappleButtonWidge;

import java.util.ArrayList;

public class CourtMgrUnitFragment extends CommonBaseFragment {

    private RappleButtonWidge mScanBtn;
    private RecyclerView mRecyclerView;
    private BleDeviceItemAdapter mBleDeviceItemAdapter;
    private ArrayList<BluetoothDeviceHolder> mBleList = new ArrayList<>();

    private boolean mScanning = false;

    private static boolean IS_BLE = false;

    private CourtUnitBluetoothInstance.ILeScanVallback mLeScanListener = new CourtUnitBluetoothInstance.ILeScanVallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            sParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device != null && !containDevice(device)) {
                        LogUtils.i("onLeScan, device: " + device.getName() + ", device mac: " + device.getAddress());
                        mBleList.add(new BluetoothDeviceHolder(device, rssi));
                    }
                    if (mBleDeviceItemAdapter != null) {
                        LogUtils.i("onLeScan, data size : " + mBleDeviceItemAdapter.getItemCount());
                        mBleDeviceItemAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };

    private boolean containDevice(BluetoothDevice device) {
        boolean isContain = false;
        if (mBleList != null && mBleList.size() > 0) {
            for (int i = 0; i < mBleList.size(); i++) {
                BluetoothDeviceHolder item = mBleList.get(i);
                if (device != null && device.getAddress().equals(item.bluetoothDevice.getAddress())) {
                    isContain = true;
                    break;
                }
            }
        }
        return isContain;
    }

    private BroadcastReceiver mBleFoundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.i("onReceive, action: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                if (device != null && !containDevice(device)) {
                    LogUtils.i("onReceive, device: " + device.getName() + ", device mac: " + device.getAddress());
                    mBleList.add(new BluetoothDeviceHolder(device, rssi));
                }
                if (mBleDeviceItemAdapter != null) {
                    LogUtils.i("onReceive, data size : " + mBleDeviceItemAdapter.getItemCount());
                    mBleDeviceItemAdapter.notifyDataSetChanged();
                }

            }
        }
    };

    public static CourtMgrUnitFragment newInstance(Activity activity) {
        sParentActivity = activity;
        CourtMgrUnitFragment f = new CourtMgrUnitFragment();
        return f;
    }

    @Override
    protected void initEvent() {
        LogUtils.i("initEvent");
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (IS_BLE) {
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).setLeScanListener(mLeScanListener);
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).scanLeDevice(true, sParentActivity);
                } else {
                    boolean isOK = CourtUnitBluetoothInstance.getInstance(sParentActivity).startScan(sParentActivity);
                    if (!isOK) {
                        return;
                    }
                }
                mScanning = true;
                mScanBtn.start();
                mBleList.clear();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        if (IS_BLE) {
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).setLeScanListener(null);
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).scanLeDevice(false, sParentActivity);
                        } else {
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).stopScan(sParentActivity);
                        }
                        mScanBtn.stop();
                    }
                }, 15000);
            }

        });

        IntentFilter bleIntentFilter = new IntentFilter();
        bleIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        sParentActivity.registerReceiver(mBleFoundReceiver, bleIntentFilter);

    }

    @Override
    protected void initView(View view) {
        LogUtils.i("initView");
        mScanBtn = view.findViewById(R.id.scan_btn);
        LinearLayoutManager layoutManager = new LinearLayoutManager(sParentActivity);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerView = view.findViewById(R.id.device_list);
        mRecyclerView.setLayoutManager(layoutManager);
        mBleDeviceItemAdapter = new BleDeviceItemAdapter();
        mRecyclerView.setAdapter(mBleDeviceItemAdapter);

        DividerItemDecoration divider = new DividerItemDecoration(sParentActivity, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(sParentActivity, R.drawable.common_divider));
        mRecyclerView.addItemDecoration(divider);

    }

    @Override
    protected void handleUIMessage(Message msg) {

    }

    @Override
    protected void handleNonUIMessage(Message msg) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.court_fragment;
    }

    private class BleDeviceItemAdapter extends RecyclerView.Adapter<BleDeviceItemAdapter.BleDeviceVH> {

        public class BleDeviceVH extends RecyclerView.ViewHolder{
            public TextView BleName;
            public TextView BleSigStrength;
            public TextView BleMac;
            public BleDeviceVH(View v) {
                super(v);
                BleName = (TextView) v.findViewById(R.id.ble_name);
                BleSigStrength = (TextView) v.findViewById(R.id.ble_sig_strength);
                BleMac = (TextView) v.findViewById(R.id.ble_mac);
            }
        }

        @Override
        public int getItemCount() {
            LogUtils.i("getItemCount, data size: " + mBleList.size());
            return mBleList.size();
        }

        @Override
        public BleDeviceVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.ble_device_item_layout, parent, false);
            return new BleDeviceVH(v);
        }

        @Override
        public void onBindViewHolder(BleDeviceVH holder, final int position) {
            LogUtils.i("onBindViewHolder, position: " + position);
            holder.BleName.setText(TextUtils.isEmpty(
                    mBleList.get(position).bluetoothDevice.getName()) ? "Default" : mBleList.get(position).bluetoothDevice.getName());
            holder.BleSigStrength.setText(mBleList.get(position).rssi + "dBm");
            holder.BleMac.setText(mBleList.get(position).bluetoothDevice.getAddress());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogUtils.i("onBindView, setOnClickListener, onClick, position: " + position);
                    BluetoothDevice device = mBleList.get(position).bluetoothDevice;
                    if (device == null) return;
                    //final Intent intent = new Intent(BluetoothDeviceActivity.this, DeviceMenuActivity.class);
                    //intent.putExtra(BluetoothInstance.EXTRAS_DEVICE_NAME, device.getName());
                    //intent.putExtra(BluetoothInstance.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    if (mScanning) {
                        mScanning = false;
                        if (IS_BLE) {
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).scanLeDevice(false, sParentActivity);
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).setLeScanListener(null);
                        } else {
                            CourtUnitBluetoothInstance.getInstance(sParentActivity).stopScan(sParentActivity);
                        }
                        mScanBtn.stop();
                    }
                    //startActivity(intent);
                    LogUtils.i("onClick, device: " + device.getName() + ", address: " + device.getAddress());
                }
            });
        }

    }


}
