package com.mobilki.pcshot;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_MP = 2;
    public static final String DIR = Environment.getExternalStorageDirectory() + "/PcShot/images/";

    BluetoothAdapter bluetoothAdapter;
    MediaProjectionManager manager;
    MediaProjection mediaProjection;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_MP){
            //getting media projection instance
            if (resultCode == RESULT_OK) {
                mediaProjection = manager.getMediaProjection(resultCode, data);
            }
            else {
                Toast.makeText(this, "Can't send screenshots without permissions", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "Can't start app without Bluetooth enabled", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        if (bluetoothAdapter.isEnabled()) {
            ServerThread serverThread = new ServerThread(bluetoothAdapter, mediaProjection, this);
            new Thread(serverThread).start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get media projection manager
        manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_ENABLE_MP);

        //check directory
        File directory = new File(DIR);
        if (!directory.exists())
            directory.mkdirs();

        //setting up images adapter
        final ImageAdapter adapter = new ImageAdapter(this);
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new MyOnClickListener());

        //check bt status
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        else {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        //set click listener
        //start client connection on click
        Button button = (Button) findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ClientThread clientThread = new ClientThread(bluetoothAdapter, adapter, MainActivity.this);
                    clientThread.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class MyOnClickListener implements GridView.OnItemClickListener {
        //show fullscreen images with new activity
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getApplicationContext(), FullImageActivity.class);
            intent.putExtra("id", position);
            startActivity(intent);
        }
    }
}
