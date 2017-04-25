package com.example.apple.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class Main2Activity extends AppCompatActivity {

    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_REC = "DEVICE_REC";
    public static final String DEVICE_MESSAGE = "DEVICE_MESSAGE";
    private TextView nametextView;
    private TextView adtextView;
    private TextView rectextView;
    private TextView messagetextView;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRec;
    private String mDevicemessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(DEVICE_ADDRESS);
        mDeviceRec = intent.getStringExtra(DEVICE_REC);
        mDevicemessage = intent.getStringExtra(DEVICE_MESSAGE);

        nametextView = (TextView) findViewById(R.id.nametextView);
        adtextView = (TextView) findViewById(R.id.adtextView);
        rectextView = (TextView) findViewById(R.id.rectextView);
        messagetextView = (TextView) findViewById(R.id.messagetextView);

        nametextView.setText("name: " + mDeviceName);
        adtextView.setText("address: " + mDeviceAddress);
        rectextView.setText("record: " +  mDeviceRec);
        messagetextView.setText("message: " + mDevicemessage);
    }

}
