package com.example.apple.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class Main3Activity extends AppCompatActivity {

    public static final String AD_LIST = "AD_LIST";

    private ListView adItemList;
    private ListAdapter listAdapter;
    private ArrayList<String> adData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adData =  new ArrayList<String>();

        final Intent intent = getIntent();
        adData = intent.getStringArrayListExtra(AD_LIST);

        adItemList = (ListView) findViewById(R.id.adItemListID);

        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, adData);//ListView使用的Adapter，
        adItemList.setAdapter(listAdapter);//將listView綁上Adapter

        ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
    }

}
