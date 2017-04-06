package com.mobilki.pcshot;


import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


class ClientThread extends AsyncTask<Void, Void, Void>{
    private static final String MAC_ADDRESS = "9C:AD:97:23:1A:86";
    private static final UUID UUID = java.util.UUID.fromString("446118F0-8B1E-11E2-9E96-0800200C9A66");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ImageAdapter imageAdapter;
    private ProgressDialog progressDialog;
    private Activity activity;

    private int imageLength;

    ClientThread(BluetoothAdapter bluetoothAdapter, ImageAdapter imageAdapter, Activity activity){
        this.bluetoothAdapter = bluetoothAdapter;
        this.imageAdapter = imageAdapter;
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //show progress dialog
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        try {
            bluetoothSocket.close();
            imageAdapter.refresh();
            progressDialog.dismiss();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        //close previous dialog and open new one
        progressDialog.dismiss();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIndeterminate(false);
        progressDialog.setMessage("Receiving screenshot...");
        progressDialog.setMax(imageLength);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            try {
                //check socket
                if (bluetoothSocket == null) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(UUID);
                }
                //check connection
                if (!bluetoothSocket.isConnected()) {
                    bluetoothSocket.connect();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //get input stream from the socket
            DataInputStream inputStream = new DataInputStream(bluetoothSocket.getInputStream());
            TimeUnit.SECONDS.sleep(3);
            byte[] bytes;

            //buffer for input bytes
            bytes = new byte[1000000];

            //create new file
            Date date = new Date();
            File imageFile = new File(MainActivity.DIR + new SimpleDateFormat("dd-MM-yyyy_hh-mm").format(date) + ".png");
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);

            //Length of input image allows us to show user a progress
            imageLength = inputStream.readInt();

            //calls onProgressUpdate method
            publishProgress();

            int len;
            int progress = 0;
            //read bytes and write it into file
            while (inputStream.available() > 0){
                len = inputStream.read(bytes);
                progressDialog.setProgress(progress += len);
                TimeUnit.SECONDS.sleep(3);
                fileOutputStream.write(bytes, 0, len);
                Log.d("Client Thread", "bytes: " + len);
            }
            progressDialog.setProgress(100);
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.d("Client Thread", "receive image");
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
