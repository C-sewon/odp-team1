package com.example.datacollect1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private BluetoothAdapter bleAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private Button btnScan, btnStop, btnSave;
    private TextView tvScanResults, tvLog;

    private StringBuilder resultsBuilder = new StringBuilder();
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = findViewById(R.id.btnScan);
        btnStop = findViewById(R.id.btnStop);
        btnSave = findViewById(R.id.btnSave);
        tvScanResults = findViewById(R.id.tvScanResults);
        tvLog = findViewById(R.id.tvLog);

        bleAdapter = BluetoothAdapter.getDefaultAdapter();

        requestBlePermissions();

        btnScan.setOnClickListener(v -> startBleScan());
        btnStop.setOnClickListener(v -> stopBleScan());
        btnSave.setOnClickListener(v -> addLog("데이터는 스캔 중 자동으로 저장됩니다."));
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void startBleScan() {
        if (bleAdapter == null) {
            addLog("이 기기는 블루투스를 지원하지 않습니다.");
            return;
        }
        if (!bleAdapter.isEnabled()) {
            addLog("블루투스를 켜주세요.");
            return;
        }
        if (!hasScanPermission()) {
            addLog("권한이 없습니다. 권한을 허용해주세요.");
            requestBlePermissions();
            return;
        }

        bluetoothLeScanner = bleAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        addLog("스캔 시작");
    }

    private void stopBleScan() {
        if (bluetoothLeScanner != null && hasScanPermission()) {
            bluetoothLeScanner.stopScan(scanCallback);
            addLog("스캔 중지");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            int rssi = result.getRssi();

            String deviceName = "unknown";
            if (hasScanPermission() && device.getName() != null) {
                deviceName = device.getName();
            }
            String deviceAddress = device.getAddress();

            byte[] scanRecordBytes = scanRecord != null ? scanRecord.getBytes() : null;

            String entry = deviceName + " | " + deviceAddress + " | RSSI: " + rssi + "\n";
            resultsBuilder.insert(0, entry);
            tvScanResults.setText(resultsBuilder.toString());

            saveToCsv(deviceName, deviceAddress, rssi);
        }

        @Override
        public void onScanFailed(int errorCode) {
            addLog("스캔 실패. Error Code: " + errorCode);
        }
    };

    private void saveToCsv(String name, String address, int rssi) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            File dir = getExternalFilesDir(null);
            File file = new File(dir, "ble_data.csv");
            boolean fileExists = file.exists();

            FileWriter fw = new FileWriter(file, true);
            if (!fileExists) {
                fw.append("timestamp,device_name,device_address,rssi\n");
            }
            fw.append(timestamp).append(",")
                    .append(name).append(",")
                    .append(address).append(",")
                    .append(String.valueOf(rssi)).append("\n");
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addLog(String message) {
        logBuilder.insert(0, message + "\n");
        tvLog.setText(logBuilder.toString());
    }
}