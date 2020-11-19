package com.example.bluetoothfactest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rk.commonlib.CommonBaseFragment;
import com.rk.commonlib.NoBleBaseFragmentActivity;
import com.rk.commonlib.bluetooth.BluetoothDeviceHolder;
import com.rk.commonlib.bluetooth.CourtUnitBluetoothInstance;
import com.rk.commonlib.files.FileSaveHelp;
import com.rk.commonlib.util.LogUtils;
import com.rk.commonlib.widge.RappleButtonWidge;
import com.rk.commonmodule.protocol.protocol3761.Protocol3761;
import com.rk.commonmodule.protocol.protocol3761.Protocol3761Helper;
import com.rk.commonmodule.utils.DataConvertUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CourtMgrUnitFragment extends CommonBaseFragment {

    private RappleButtonWidge mScanBtn;
    private EditText mTestBleName;
    private RecyclerView mRecyclerView;
    private BleDeviceItemAdapter mBleDeviceItemAdapter;
    private ArrayList<BluetoothDeviceHolder> mBleList = new ArrayList<>();

    private String mCurrentDeviceName = null;
    private String mCurrentDeviceAddr = null;
    private boolean mScanning = false;

    private static final String SPACE = "    ";
    private static final String FILE_SUFFIX = "txt";

    private static boolean IS_BLE = true;

    private static final int NON_UI_SEND_CONFIRM_MSG = 0;
    private static final int NON_UI_RECORD_LOG_MSG = 1;
    private static final int NON_UI_DELETE_FILE_MSG = 2;

    private static final int UI_UPDATE_LIST_MSG = SHOW_TOAST_MSG + 1;
    private static final int UI_DISCONNECT_BLE_MSG = SHOW_TOAST_MSG + 2;
    private static final int UI_STOP_BLE_SCAN_MSG = SHOW_TOAST_MSG + 3;

    private CourtUnitBluetoothInstance.ILeScanVallback mLeScanListener = new CourtUnitBluetoothInstance.ILeScanVallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            LogUtils.i("onLeScan, device: " + device.getName() + ", device mac: " + device.getAddress());
            if (device != null && !TextUtils.isEmpty(device.getName()) && device.getName().startsWith("TMU")
                    && !containDevice(device) && device.getName().startsWith(mCurrentDeviceName)) {
                //LogUtils.i("onLeScan, device: " + device.getName() + ", device mac: " + device.getAddress());
                mBleList.add(new BluetoothDeviceHolder(device, rssi));
                mUIHandler.removeMessages(UI_UPDATE_LIST_MSG);
                mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_UPDATE_LIST_MSG));
                mUIHandler.removeMessages(UI_STOP_BLE_SCAN_MSG);
                mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_STOP_BLE_SCAN_MSG));
            }
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
                LogUtils.i("onReceive, device name: " + device.getName() + ", device mac: " + device.getAddress());
                if (device != null && /*!containDevice(device)*/ !TextUtils.isEmpty(device.getName()) && device.getName().startsWith("TMU")) {
                    //LogUtils.i("onReceive, device: " + device.getName() + ", device mac: " + device.getAddress());
                    mBleList.add(new BluetoothDeviceHolder(device, rssi));
                    mUIHandler.removeMessages(UI_UPDATE_LIST_MSG);
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_UPDATE_LIST_MSG));
                }

            }
        }
    };

    private BroadcastReceiver mBleHandlerReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.i("onReceive, action: " + action);
            if (CourtUnitBluetoothInstance.ACTION_GATT_CONNECTED.equals(action)) {
                CourtUnitBluetoothInstance.getInstance(sParentActivity).discoveryGattServices();
            } else if (CourtUnitBluetoothInstance.ACTION_GATT_DISCONNECTED.equals(action)) {
                ((NoBleBaseFragmentActivity)sParentActivity).setLoadingVisible(false);
                CourtUnitBluetoothInstance.getInstance(sParentActivity).close();
                int status = intent.getIntExtra("status", -1);
                LogUtils.i("status: " + status);
                if (status == 19) {
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).connect(mCurrentDeviceAddr);
                } else if (status != 0) {
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "蓝牙设备忙，请重测试！"));
                    String content = "连接设备[" + mCurrentDeviceName + "]忙，请重连";
                    mNonUIHandler.removeMessages(NON_UI_RECORD_LOG_MSG);
                    mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_RECORD_LOG_MSG, content));
                }
            } else if (CourtUnitBluetoothInstance.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                CourtUnitBluetoothInstance.getInstance(sParentActivity).getServicesAndCharacteristics();
                CourtUnitBluetoothInstance.getInstance(sParentActivity).enableCharacteristicNotify();
            } else if (CourtUnitBluetoothInstance.ACTION_GATT_DESCRIPTOR_WRITE.equals(action)) {
                mNonUIHandler.removeMessages(NON_UI_SEND_CONFIRM_MSG);
                mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_SEND_CONFIRM_MSG));
            } else if (CourtUnitBluetoothInstance.ACTION_GATT_CHARACTERISTIC_WRITE.equals(action)) {

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
                mCurrentDeviceName = mTestBleName.getText().toString();
                if (TextUtils.isEmpty(mCurrentDeviceName)) {
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "请输入测试蓝牙设备名称！"));
                    return;
                }
                mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_DELETE_FILE_MSG, mCurrentDeviceName));
                startScan();
            }

        });

        IntentFilter bleIntentFilter = new IntentFilter();
        bleIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        sParentActivity.registerReceiver(mBleFoundReceiver, bleIntentFilter);

        IntentFilter bleHandlerIntentFilter = new IntentFilter();
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_CONNECTED);
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_DISCONNECTED);
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_SERVICES_DISCOVERED);
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_DESCRIPTOR_WRITE);
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_CHARACTERISTIC_WRITE);
        bleHandlerIntentFilter.addAction(CourtUnitBluetoothInstance.ACTION_GATT_DESCRIPTOR_WRITE);
        sParentActivity.registerReceiver(mBleHandlerReceiver, bleHandlerIntentFilter);

    }

    private void startScan() {
        if (mScanning) {
            return;
        }
        CourtUnitBluetoothInstance.getInstance(sParentActivity).close();
        if (IS_BLE) {
            CourtUnitBluetoothInstance.getInstance(sParentActivity).setLeScanListener(mLeScanListener);
            boolean isOK = CourtUnitBluetoothInstance.getInstance(sParentActivity).scanLeDevice(true, sParentActivity);
            if (!isOK) {
                return;
            }
        } else {
            boolean isOK = CourtUnitBluetoothInstance.getInstance(sParentActivity).startScan(sParentActivity);
            if (!isOK) {
                return;
            }
        }
        mScanning = true;
        mScanBtn.start();
        mBleList.clear();

        mUIHandler.sendMessageDelayed(mUIHandler.obtainMessage(UI_STOP_BLE_SCAN_MSG), 20000);
    }

    @Override
    protected void initView(View view) {
        LogUtils.i("initView");
        mScanBtn = view.findViewById(R.id.scan_btn);
        mTestBleName = view.findViewById(R.id.ble_name);
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
        switch (msg.what) {
            case UI_UPDATE_LIST_MSG:
                if (mBleDeviceItemAdapter != null) {
                    LogUtils.i("onReceive, data size : " + mBleDeviceItemAdapter.getItemCount());
                    mBleDeviceItemAdapter.notifyDataSetChanged();
                }
                break;
            case UI_DISCONNECT_BLE_MSG:
                CourtUnitBluetoothInstance.getInstance(sParentActivity).disconnect();
                break;
            case UI_STOP_BLE_SCAN_MSG:
                mScanning = false;
                if (IS_BLE) {
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).setLeScanListener(null);
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).scanLeDevice(false, sParentActivity);
                } else {
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).stopScan(sParentActivity);
                }
                mScanBtn.stop();


                if (mBleList != null && mBleList.size() > 0) {
                    for (int i = 0; i < mBleList.size(); i++) {
                        BluetoothDeviceHolder item = mBleList.get(i);
                        if (item.bluetoothDevice.getName().startsWith(mCurrentDeviceName)) {
                            //TODO: delete old;
                            StringBuilder sb = new StringBuilder();
                            sb.append("[").append(mCurrentDeviceName).append("]:").append("已发现设备");
                            mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_RECORD_LOG_MSG, sb.toString()));
                            mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "发现设备"));
                            break;
                        }
                    }
                } else {
                    //TODO:
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(mCurrentDeviceName).append("]:").append("未发现设备");
                    mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_RECORD_LOG_MSG, sb.toString()));
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "未发现设备，检测失败"));
                }
                break;
        }

    }

    @Override
    protected void handleNonUIMessage(Message msg) {
        ((NoBleBaseFragmentActivity)sParentActivity).setLoadingVisible(true);
        switch (msg.what) {
            case NON_UI_SEND_CONFIRM_MSG:
                byte[] recv = CourtUnitBluetoothInstance.getInstance(sParentActivity).sendConfirm();
                if (recv == null) {
                    String content = "检测设备[" + mCurrentDeviceName + "]失败";
                    recordLog(content);
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_DISCONNECT_BLE_MSG));
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "检测失败"));
                } else {
                    recv = CourtUnitBluetoothInstance.getInstance(sParentActivity).sendEncryptInfo(recv);
                    if (recv == null) {
                        String content = "检测设备[" + mCurrentDeviceName + "]失败";
                        recordLog(content);
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_DISCONNECT_BLE_MSG));
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "检测失败"));
                    } else {
                        //CourtUnitBluetoothInstance.getInstance(sParentActivity).disconnect();
                        String content = "成功连接设备[" + mCurrentDeviceName + "]";
                        recordLog(content);
                        byte[] sendFrame = Protocol3761Helper.makeGetTerminalVersionInfoFrame();
                        LogUtils.i("send frame: " + DataConvertUtils.convertByteArrayToString(sendFrame, false));
                        content = "发送读取版本信息报文>>>>>>>>：" + DataConvertUtils.convertByteArrayToString(sendFrame, false);
                        recordLog(content);
                        if (sendFrame != null) {
                            recv = CourtUnitBluetoothInstance.getInstance(sParentActivity).sendAndReceiveSync(sendFrame);
                            LogUtils.i("recv frame: " + DataConvertUtils.convertByteArrayToString(recv, false));
                            content = "接收版本信息回复报文<<<<<<<<：" + DataConvertUtils.convertByteArrayToString(recv, false);
                            recordLog(content);

                            content = "检测设备[" + mCurrentDeviceName + "]成功";
                            recordLog(content);
                        }
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(UI_DISCONNECT_BLE_MSG));
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(SHOW_TOAST_MSG, "检测成功"));
                    }
                }
                break;
            case NON_UI_RECORD_LOG_MSG:
                String content = (String) msg.obj;
                if (TextUtils.isEmpty(content) || TextUtils.isEmpty(mCurrentDeviceName)) {
                    return;
                }
                recordLog(content);
                break;
            case NON_UI_DELETE_FILE_MSG:
                String fileName = (String) msg.obj;
                if (TextUtils.isEmpty(fileName)) {
                    return;
                }
                String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + getString(R.string.rk_file)+ File.separator + fileName + "." + FILE_SUFFIX;
                //LogUtils.i("delete file path: " + filePath);
                File logFile = new File(filePath);
                try {
                    if (logFile.exists()) {
                        LogUtils.i("delete file:" + filePath);
                        logFile.delete();
                    }
                    //logFile.createNewFile();
                } catch (Exception e) {
                    //TODO:
                    LogUtils.i("delete fail: " + e.getMessage());
                }
                FileSaveHelp.FILE_SAVE_HELP.scanFile(sParentActivity, fileName, FILE_SUFFIX, sParentActivity);
                break;
        }
        ((NoBleBaseFragmentActivity)sParentActivity).setLoadingVisible(false);
    }

    private void recordLog(String content) {
        LogUtils.i("content: " + content);
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String timeStamp = simpleDateFormat.format(date);
        StringBuilder sb = new StringBuilder();
        sb.append(timeStamp).append(SPACE).append(content);
        FileSaveHelp.FILE_SAVE_HELP.saveToTxtFile(sParentActivity, sb.toString(), mCurrentDeviceName, FILE_SUFFIX, sParentActivity);
        FileSaveHelp.FILE_SAVE_HELP.scanFile(sParentActivity, mCurrentDeviceName, FILE_SUFFIX, sParentActivity);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.court_fragment;
    }

    @Override
    public void onDestroy() {
        if (mScanning) {
            mScanBtn.stop();
            CourtUnitBluetoothInstance.getInstance(sParentActivity).stopScan(sParentActivity);
            mScanning = false;
        }
        CourtUnitBluetoothInstance.getInstance(sParentActivity).disconnect();
        CourtUnitBluetoothInstance.getInstance(sParentActivity).close();
        //CourtUnitBluetoothInstance.getInstance(sParentActivity).refreshDeviceCache();
        if (mBleFoundReceiver != null) {
            sParentActivity.unregisterReceiver(mBleFoundReceiver);
            mBleFoundReceiver = null;
        }
        if (mBleHandlerReceiver != null) {
            sParentActivity.unregisterReceiver(mBleHandlerReceiver);
            mBleHandlerReceiver = null;
        }
        super.onDestroy();
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
                    if (mBleList == null || mBleList.size() <= 0) {
                        return;
                    }
                    BluetoothDevice device = mBleList.get(position).bluetoothDevice;
                    if (device == null) return;
                    //final Intent intent = new Intent(BluetoothDeviceActivity.this, DeviceMenuActivity.class);
                    //intent.putExtra(BluetoothInstance.EXTRAS_DEVICE_NAME, device.getName());
                    //intent.putExtra(BluetoothInstance.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    ((NoBleBaseFragmentActivity)sParentActivity).setLoadingVisible(true);
                    //CourtUnitBluetoothInstance.getInstance(sParentActivity).close();
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
                    //mCurrentDeviceName = device.getName();
                    mCurrentDeviceAddr = device.getAddress();
                    String content = "开始连接设备[" + mCurrentDeviceName + "]";
                    mNonUIHandler.removeMessages(NON_UI_RECORD_LOG_MSG);
                    mNonUIHandler.sendMessage(mNonUIHandler.obtainMessage(NON_UI_RECORD_LOG_MSG, content));
                    CourtUnitBluetoothInstance.getInstance(sParentActivity).connect(device.getAddress());
                    //startActivity(intent);
                    LogUtils.i("onClick, device: " + device.getName() + ", address: " + device.getAddress());
                }
            });
        }

    }


}
