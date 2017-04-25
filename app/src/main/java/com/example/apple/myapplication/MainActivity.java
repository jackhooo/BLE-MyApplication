package com.example.apple.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    //    其大概Layout如上圖，兩個Button分別為開始搜尋BLE Device與停止搜尋，下面為一個ListView，將搜尋到的BLE添加到ListView顯示。
    private TextView textView;
    private ListView scanList;
    private ArrayList<String> deviceName;
    private ArrayList<String> deviceScanRec;
    private ArrayList<String> devicesMessage;
    private ArrayList<String> adItem;
    private ListAdapter listAdapter;
    private int mScanningMode = 3;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_TIME = 60000;
    private static final int AD2_TIME = 5000;
    private static final int AD3_TIME = 10000;
    private static final int STOP_TIME = 500;
    private static final int STOPAD_TIME = 1000;
    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();
    private Handler mHandler; //該Handler用來搜尋Devices10秒後，自動停止搜尋

    private static String ALGORITHM = "RSA/ECB/PKCS1Padding";

    private KeyPairGenerator keygen;
    private SecureRandom random;
    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        在一開始onCreate當中，需檢查手機本身是否支持BLE? 以及BL，分別為下面兩式：
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(getPackageManager().FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getBaseContext(), R.string.No_sup_ble, Toast.LENGTH_SHORT).show();
            finish();
        }//利用getPackageManager().hasSystemFeature()檢查手機是否支援BLE設備，否則利用finish()關閉程式。

        //試著取得BluetoothAdapter，如果BluetoothAdapter==null，則該手機不支援Bluetooth
        //取得Adapter之前，需先使用BluetoothManager，此為系統層級需使用getSystemService
        mBluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), R.string.No_sup_Bluetooth, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }//如果==null，利用finish()取消程式。

        textView = (TextView) findViewById(R.id.textViewID);
        scanList = (ListView) findViewById(R.id.scanlistID);

        deviceScanRec = new ArrayList<String>();
        deviceName = new ArrayList<String>();   //此ArrayList屬性為String，用來裝Devices Name
        devicesMessage = new ArrayList<>();
        adItem = new ArrayList<String>();

        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);//ListView使用的Adapter，
        scanList.setAdapter(listAdapter);//將listView綁上Adapter

        scanList.setOnItemClickListener(new onItemClickListener()); //綁上OnItemClickListener，設定ListView點擊觸發事件
        mHandler = new Handler();

        try {
            keygen = KeyPairGenerator.getInstance("RSA");
            random = new SecureRandom();
            random.setSeed("CS".getBytes());
            keygen.initialize(512, random);// TODO Change length may cause the result incorrect.
            keyPair = keygen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] rsaEncode(String plainText) {

        Encryption encryption = new Encryption();

        byte[] encryptedResult = "0".getBytes();

        try {
            // Encrypt
            encryptedResult = encryption.cryptByRSA(plainText.getBytes("UTF-8"), publicKey, ALGORITHM, Cipher.ENCRYPT_MODE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedResult;
    }

    public String rsaDecode(byte[] result) throws UnsupportedEncodingException {

        Encryption encryption = new Encryption();

        byte[] decryptResult = "0".getBytes();

        try {
            decryptResult = encryption.cryptByRSA(result, privateKey, ALGORITHM, Cipher.DECRYPT_MODE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String(decryptResult, "UTF-8");
    }

    //需要注意的是，需加入一個stopLeScan在onPause()中，當按返回鍵或關閉程式時，需停止搜尋BLE
    //否則下次開啟程式時會影響到搜尋BLE device
    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause():Stop Scan");
        mScanningMode = 3;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        Intent ServiceIntent = new Intent(MainActivity.this, AdvertiserService.class);
        stopService(ServiceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //一般來說，只要使用到mBluetoothAdapter.isEnabled()就可以將BL開啟了，但此部分添加一個Result Intent
        //跳出詢問視窗是否開啟BL，因此該Intent為BluetoothAdapter.ACTION.REQUEST_ENABLE
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT); //再利用startActivityForResult啟動該Intent
        }

        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);//ListView使用的Adapter，
        scanList.setAdapter(listAdapter);//將listView綁上Adapter

        ScanFunction(true); //使用ScanFunction(true) 開啟BLE搜尋功能，該Function在下面部分
    }

    //這個Override Function是因為在onResume中使用了ActivityForResult，當使用者按了取消或確定鍵時，結果會
    //返回到此onActivityResult中，在判別requestCode判別是否==RESULT_CANCELED，如果是則finish()程式
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (REQUEST_ENABLE_BT == 1 && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //此為ScanFunction，輸入函數為boolean，如果true則開始搜尋，false則停止搜尋
    private void ScanFunction(boolean enable) {
        if (enable) {

            mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {

                    if (mScanningMode != 3) {
                        ScanFunction(false);
                        Log.d(TAG, "ScanFunction():Stop Scan");
                    }

                }
            }, SCAN_TIME); //SCAN_TIME為 1分鐘 後要執行此Runnable

            mScanningMode = 1; //搜尋旗標設為true
            mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
            textView.setText("Scanning");
            Log.d(TAG, "Start Scan");

        } else {

            mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {

                    if (mScanningMode != 3) {
                        ScanFunction(true);
                        Log.d(TAG, "ScanFunction():Start Scan");
                    }

                }
            }, STOP_TIME); //STOP_TIME為 0.5秒 後要執行此Runnable

            mScanningMode = 2;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            textView.setText("Stop Scan");
        }
    }

//注意，在此enable==true中的Runnable是在10秒後才會執行，因此是先startLeScan，10秒後才會執行Runnable內的stopLeScan
//在BLE Devices Scan中，使用的方法為startLeScan()與stopLeScan()，兩個方法都需填入callback，當搜尋到設備時，都會跳到
//callback的方法中

    //建立一個BLAdapter的Callback，當使用startLeScan或stopLeScan時，每搜尋到一次設備都會跳到此callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                //使用runOnUiThread方法，其功能等同於WorkThread透過Handler將資訊傳到MainThread(UiThread)中，
                //詳細可進到runOnUiThread中觀察
                @Override
                public void run() {
                    if (!mBluetoothDevices.contains(device)) { //利用contains判斷是否有搜尋到重複的device
                        mBluetoothDevices.add(device);//如沒重複則添加到bluetoothDevices中

                        Toast.makeText(getBaseContext(), "新設備", Toast.LENGTH_SHORT).show();

                        int manufacturerIDStart = 5;

                        if (device.getName() != null) {
                            manufacturerIDStart += device.getName().length() + 2;
                        }

                        String scanMessage = convertHexToString(bytesToHexString(scanRecord));
                        String manufacturerID = scanMessage.substring(manufacturerIDStart, manufacturerIDStart + 2);
                        int messageStart = manufacturerIDStart + 2;
                        String manufacturerMessage = scanMessage.substring(messageStart);

                        if (manufacturerID.equals("CS")) {
                            adItem.add(manufacturerMessage);
                        }

                        deviceScanRec.add(bytesToHexString(scanRecord));
                        devicesMessage.add(manufacturerID + "  " + manufacturerMessage);
                        deviceName.add(manufacturerID + " rssi:" + rssi + "\r\n" + device.getAddress()); //將device的Name、rssi、address裝到此ArrayList<String>中

                        ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                    }
                }
            });
        }
    };


    //分別按下搜尋予停止搜尋button時的功能，分別為開始搜尋與停止搜尋
    public void btnClick(View v) {
        switch (v.getId()) {
            case R.id.scanbtnID:
                ScanFunction(true);
                break;
            case R.id.stopbtnID:
                mScanningMode = 3;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                textView.setText("Stop Scan");
                break;
        }
    }

    //分別按下搜尋予停止搜尋button時的功能，分別為開始與停止AD
    public void adBtnClick(View v) throws UnsupportedEncodingException {

        final Intent ServiceIntent = new Intent(MainActivity.this, AdvertiserService.class);

        final byte[] encodeData = rsaEncode("Dada Good");
        String encodeDataToHex = bytesToHexString(encodeData);

        byte[] decodeFrom = hexStringToByteArray(encodeDataToHex);
        final String decodeResult = rsaDecode(decodeFrom);

        switch (v.getId()) {
            case R.id.StartAdbutID:

                mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                    @Override
                    public void run() {

                        stopService(ServiceIntent);
                        ServiceIntent.putExtra(AdvertiserService.INPUT, decodeResult + "  2");
                        startService(ServiceIntent);

                    }
                }, AD2_TIME); //5秒後要執行

                mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                    @Override
                    public void run() {

                        stopService(ServiceIntent);
                        ServiceIntent.putExtra(AdvertiserService.INPUT, decodeResult + "  3");
                        startService(ServiceIntent);

                    }
                }, AD3_TIME); //10秒後要執行

                //ServiceIntent.putExtra(AdvertiserService.INPUT, input.getText().toString());
                //Toast.makeText(getBaseContext(), decodeResult , Toast.LENGTH_SHORT).show();
                ServiceIntent.putExtra(AdvertiserService.INPUT, decodeResult + "  1");
                startService(ServiceIntent);
                break;

            case R.id.StopAdbutID:
                stopService(ServiceIntent);
                break;
        }
    }

    public void clickAdItem(View view) {
        Intent intent = new Intent(MainActivity.this, Main3Activity.class);
        intent.putExtra(Main3Activity.AD_LIST, adItem);
        startActivity(intent);
    }

    //以下為ListView ItemClick的Listener，當按下Item時，將該Item的BLE Name與Address包起來，將送到另一
    //Activity中建立連線
    private class onItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            //mBluetoothDevices為一個陣列資料ArrayList<BluetoothDevices>，使用.get(position)取得
            //Item位置上的BluetoothDevice
            final BluetoothDevice mBluetoothDevice = mBluetoothDevices.get(position);

            //建立一個Intent，將從此Activity進到ControlActivity中
            //在ControlActivity中將與BLE Device連線，並互相溝通
            Intent goControlIntent = new Intent(MainActivity.this, Main2Activity.class);

            //將device Name與address存到ControlActivity的DEVICE_NAME與ADDRESS，以供ControlActivity使用

            if (mBluetoothDevice.getName() == null) {
                goControlIntent.putExtra(Main2Activity.DEVICE_NAME, mBluetoothDevice.getName());
            } else {
                goControlIntent.putExtra(Main2Activity.DEVICE_NAME, mBluetoothDevice.getName() + "  Hex: " + asciiToHex(mBluetoothDevice.getName()));
            }

            goControlIntent.putExtra(Main2Activity.DEVICE_ADDRESS, mBluetoothDevice.getAddress());
            goControlIntent.putExtra(Main2Activity.DEVICE_REC, deviceScanRec.get(position));
            goControlIntent.putExtra(Main2Activity.DEVICE_MESSAGE, devicesMessage.get(position));

            if (mScanningMode == 1) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanningMode = 3;
            }

            startActivity(goControlIntent);
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            buffer.append(String.format("%02x", bytes[i]));
        }
        return buffer.toString();
    }

    private static String asciiToHex(String asciiValue) {
        char[] chars = asciiValue.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }

    public String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }
}
