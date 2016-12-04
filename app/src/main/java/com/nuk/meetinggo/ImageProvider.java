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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.nuk.meetinggo.RemoteActivity.isRunning;
import static com.nuk.meetinggo.RemoteControlFragment.MODE_SHARE;

public class ImageProvider implements Runnable {

    private FragmentActivity main;
    private Handler mHandler;
    private String serverAddr = LinkCloud.SERVER_IP;
    private int imagePort = Constants.SERVER_RECVIMAGE;
    private Socket mSocket, image;
    private OutputStream out;
    private PrintWriter mOut; // Main server socket

    WebView screenImage;

    private int framesPerSecond = 1;
    public static boolean isConnected = false;

    public ImageProvider(Socket socket, FragmentActivity activity, Handler handler) {
        main = activity;
        mSocket = socket;
        mHandler = handler;
        screenImage = null;
    }

    public void run() {

        if (mSocket == null)
            isConnected = false;
        else
            isConnected = true;

        try {
            if (isConnected) {
                mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket
                        .getOutputStream())), true); // Create output stream to send data to server

                // Send connection message
                mOut.println("" + Constants.PROVIDEIMAGE);
                //mOut.flush();
                //RemoteControlFragment.sendMessage("" + Constants.PROVIDEIMAGE);

                Timer timer = new Timer();
                int frames = 1000 / framesPerSecond;

                timer.scheduleAtFixedRate(sendImageTask, 0, frames);
            }
        }
        catch (IOException e) {
            Log.e("remotedroid", "Error while creating OutWriter", e);
            e.printStackTrace();
            isConnected = false;
        }
    }

    private TimerTask sendImageTask = new TimerTask() {
        @Override
        public void run() {
            if (isRunning) {
                Bitmap screenShot = getScreenShot();

                byte[] img = Constants.createBitmapMessage(screenShot);
                sendMessage(img);
                previewImage(img);
            }
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

    // Turn full screen to bitmap
    private Bitmap getScreenShot()
    {

        // Cache full screen and put in view
        View view = main.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap fullBitmap = view.getDrawingCache();

        // Get the height of system bar
        Rect rect = new Rect();
        main.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;

        // Get device width and height
        Display display = main.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int phoneWidth = size.x;
        int phoneHeight = size.y;

        // Create new bitmap and remove system bar
        Bitmap bitmap = Bitmap.createBitmap(fullBitmap, 0, statusBarHeight, phoneWidth, phoneHeight - statusBarHeight);
        // Remove screen cache
        view.destroyDrawingCache();

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
            bundle.putInt("Mode", MODE_SHARE);
            uiMessage.setData(bundle);
            uiMessage.what = Constants.DO_UI_IMAGE;
            mHandler.sendMessage(uiMessage);
        }
    }
}