package com.example.gefen.myapplication;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;


import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String AUDIO_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/recorded_audio.wav";
    private BluetoothGattCallback ConnectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // CALL THIS METHOD TO BEGIN DISCOVER SERVICES
                gatt.discoverServices();
            }
        }
        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            byte[] strBytes = "1".getBytes();

            //gatt.notifyCharacteristicChanged(device, characteristic, false);
            BluetoothGattService TriangleService = gatt.getServices().get(3);
            // enable notifications
            BluetoothGattCharacteristic charGetKey = TriangleService.getCharacteristics().get(3);
            //charGetKey.setValue(strBytes);
            BluetoothGattDescriptor descriptor =
                    charGetKey.getDescriptor(convertFromInteger(0x2902));
            descriptor.setValue(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            gatt.writeDescriptor(descriptor);
            gatt.setCharacteristicNotification(TriangleService.getCharacteristics().get(3), true);

            //gatt.readCharacteristic(LockService.getCharacteristics().get(4));
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            BluetoothGattCharacteristic charGetChallenge = gatt.getServices().get(3).getCharacteristics().get(2);
            byte arr[] = {1};
            charGetChallenge.setValue(arr);
            gatt.writeCharacteristic(charGetChallenge);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte key[] = characteristic.getValue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //gatt.readCharacteristic(characteristic);
            byte challenge[] = characteristic.getValue();
            BluetoothGattCharacteristic charSetResponse = gatt.getServices().get(3).getCharacteristics().get(0);
            charSetResponse.setValue("1");
            gatt.writeCharacteristic(charSetResponse);


        }
    };


    private ScanCallback mscanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice().getName()!= null && result.getDevice().getName().equals("SimpleBLEPeripheral")) {
                Toast.makeText(getApplicationContext(), "Found ONE!!", Toast.LENGTH_LONG).show();
                mTextMessage.setText("found ONE!");
                BluetoothDevice triangle_device_one = result.getDevice();
                triangle_device_one.connectGatt(getApplicationContext(), false, ConnectCallback);
                Thread t = new Thread();
                try {
                    t.sleep(11000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (result.getDevice().getName()!= null && result.getDevice().getName().equals("SimpleBLEPeripherak")) {
                Toast.makeText(getApplicationContext(), "Found TWO!!", Toast.LENGTH_LONG).show();
                mTextMessage.setText("found TWO!");
                BluetoothDevice triangle_device_two = result.getDevice();
                triangle_device_two.connectGatt(getApplicationContext(), false, ConnectCallback);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void recordAudio(View v) {
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(AUDIO_FILE_PATH)
                .setColor(ContextCompat.getColor(this, R.color.recorder_bg))
                .setRequestCode(REQUEST_RECORD_AUDIO)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                .setAutoStart(false)
                .setKeepDisplayOn(true)

                // Start recording
                .record();
    }

    private TextView mTextMessage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UtilPremission.requestPermission(this, Manifest.permission.RECORD_AUDIO);
        UtilPremission.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        mTextMessage = (TextView) findViewById(R.id.message);

        BluetoothAdapter bluetoothAdapter;

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        int perm_check = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (perm_check != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){

            }
            else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();

        new Thread(new Runnable() {
            public void run() {
                // a potentially  time consuming task
                recordAudio(null);
            }
        }).start();
        scanner.startScan(mscanCallback);

    }



    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
