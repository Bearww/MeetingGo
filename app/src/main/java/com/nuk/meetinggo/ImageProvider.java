package com.nuk.meetinggo;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.WebView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ImageProvider implements Runnable {

    private FragmentActivity main;
    private Handler mHandler;
    private InetAddress serverAddr;
    private int serverPort = Constants.SERVER_PORT;
    private int imagePort;
    private Socket socket, image;
    private OutputStream out;
    private PrintWriter mOut; // Main server socket

    WebView screenImage;

    private int framesPerSecond = 1;
    public boolean isConnected = false;

    public ImageProvider(int port, FragmentActivity activity, Handler handler) {
        main = activity;
        mHandler = handler;
        screenImage = null;

        try {
            serverAddr = InetAddress.getByName(Constants.SERVER_IP);
        } catch (Exception e) {
            Log.e("ClientProvider", "C: Error", e);
        }
        imagePort = port;
    }

    public ImageProvider(int port, WebView view, FragmentActivity activity) {
        main = activity;
        screenImage = view;

        try {
            serverAddr = InetAddress.getByName(Constants.SERVER_IP);
        } catch (Exception e) {
            Log.e("ClientProvider", "C: Error", e);
        }
        imagePort = port;
    }

    public void run() {
        try {
            isConnected = true;
            socket = new Socket(serverAddr, serverPort); // Open socket on server IP and port
        } catch (Exception e) {
            Log.e("ClientActivity", "Client Connection Error", e);
            isConnected = false;
        }

        try {
            if (isConnected) {
                mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                        .getOutputStream())), true); // Create output stream to send data to server

                // Send connection message
                mOut.println("" + Constants.PROVIDEIMAGE);
                mOut.flush();

                // Simulate network delay.
                //Thread.sleep(2000);

                Timer timer = new Timer();
                int frames = 2000 / framesPerSecond;

                timer.scheduleAtFixedRate(sendImageTask, 0, frames);
            }
        } catch (IOException e) {
            Log.e("remotedroid", "Error while creating OutWriter", e);
        //} catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TimerTask sendImageTask = new TimerTask() {
        @Override
        public void run() {
            Bitmap screenShot = getScreenShot();
            // TODO Check image in storage
            //saveImage(screenShot);
            byte[] img = Constants.createBitmapMessage(screenShot);
            sendMessage(img);
            previewImage(img);
        }
    };

    private void sendMessage(byte[] message) {
        if (isConnected) {
            if(out == null) {
                try {
                    image = new Socket(serverAddr, imagePort);
                    out = image.getOutputStream(); // Create output stream to send image to server
                } catch (IOException e) {
                    e.printStackTrace();
                    isConnected = false;
                }
            }

            if(out != null) {
                // Send message to server
                try {
                    byte[] msgLength = ByteBuffer.allocate(4).putInt(message.length).array();
                    out.write(msgLength);
                    out.write(message);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //將全螢幕畫面轉換成Bitmap
    private Bitmap getScreenShot()
    {

        //藉由View來Cache全螢幕畫面後放入Bitmap
        View view = main.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap fullBitmap = view.getDrawingCache();

        //取得系統狀態列高度
        Rect rect = new Rect();
        main.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;

        //取得手機螢幕長寬尺寸
        Display display = main.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int phoneWidth = size.x;
        int phoneHeight = size.y;

        //將狀態列的部分移除並建立新的Bitmap
        Bitmap bitmap = Bitmap.createBitmap(fullBitmap, 0, statusBarHeight, phoneWidth, phoneHeight - statusBarHeight);
        //將Cache的畫面清除
        view.destroyDrawingCache();

        //screenImage.setDrawingCacheEnabled(true);
        //Bitmap bitmap = Bitmap.createBitmap(screenImage.getDrawingCache());

        return bitmap;
    }

    private void saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File(myDir, fname);
        if (file.exists ()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void previewImage(final byte[] img) {
        if(mHandler != null) {
            Bundle bundle = new Bundle();
            bundle.putByteArray("Image", img);

            Message uiMessage = new Message();
            uiMessage.setData(bundle);
            uiMessage.what = Constants.DO_UI_IMAGE;
            mHandler.sendMessage(uiMessage);
        }
    }
}