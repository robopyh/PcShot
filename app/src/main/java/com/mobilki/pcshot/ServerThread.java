package com.mobilki.pcshot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ServerThread implements Runnable {
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private DisplayMetrics metrics;
    private MediaProjection mediaProjection;

    private Activity activity;
    private Handler handler;
    
    ServerThread(BluetoothAdapter bluetoothAdapter, MediaProjection mediaProjection, Activity activity) {
        this.mediaProjection = mediaProjection;
        this.activity = activity;

        //setting up adapter
        //cancel discovery to increase speed
        bluetoothAdapter.cancelDiscovery();
        try {
            bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AndroidServer", UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler();
                Looper.loop();
            }
        }.start();
    }
    
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        while (true) {
            try {
                //listening connections
                socket = bluetoothServerSocket.accept();
                //get input&output streams
                outputStream = new DataOutputStream(socket.getOutputStream());
                inputStream = new DataInputStream(socket.getInputStream());
                if (socket != null) {
                    sendScreenshot();
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void sendScreenshot(){
        //creating virtual display
        metrics = activity.getResources().getDisplayMetrics();
        ImageReader imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);
        mediaProjection.createVirtualDisplay("screenshot", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.getSurface(), null, null);

        //send image when available
        imageReader.setOnImageAvailableListener(new ServerThread.ImageAvailable(), handler);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class ImageAvailable implements ImageReader.OnImageAvailableListener {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image;
            Bitmap bitmap;

            try {
                if(inputStream.readInt() != 1)
                    return;
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * metrics.widthPixels;

                    //create bitmap
                    bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    //image is ready to transfer
                    outputStream.writeInt(1);

                    //write bitmap to a file
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    Log.d("Server Thread", "Send Screen");
                    image.close();
                }
                else
                    Log.d("Server Thread", "Error during sending screen");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
