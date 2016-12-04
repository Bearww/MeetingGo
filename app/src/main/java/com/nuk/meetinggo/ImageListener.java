package com.nuk.meetinggo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.nuk.meetinggo.RemoteActivity.isRunning;
import static com.nuk.meetinggo.RemoteControlFragment.MODE_CONTROL;

public class ImageListener implements Runnable {

    private Handler mHandler;
    private Socket mSocket;
    private PrintWriter out;
    private InputStream in;

    private int framesPerSecond = 1;
    public static boolean isConnected = false;

    public static int DeviceWidth = 100;
    public static int DeviceHeight = 100;

    public ImageListener(InputStream in, int fps, Handler handler) {
        this.in = in;
        framesPerSecond = fps;
        mHandler = handler;
    }

    public void run() {
        try {
            isConnected = true;
            mSocket = new Socket(LinkCloud.SERVER_IP, Constants.LISTEN_PORT); // Open socket on server IP and port

            Timer timer = new Timer();
            int frames = 1000 / framesPerSecond;

            timer.scheduleAtFixedRate(getImageTask, 0, frames);
        } catch (Exception e) {
            Log.e("[IL]", "Client Connection Error", e);
            isConnected = false;
        }

        listen();
/*
        try {
            if (isConnected) {
                //out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket
                //        .getOutputStream())), true); // Create output stream to send data to server
                //in = mSocket.getInputStream();


            }
        } catch (IOException e) {
            Log.e("remotedroid", "Error while creating OutWriter", e);
        }
*/
    }

    private TimerTask getImageTask = new TimerTask() {
        @Override
        public void run() {
            if (isRunning) {
                String message = "" +
                        Constants.REQUESTIMAGE +
                        Constants.DELIMITER +
                        DeviceWidth +
                        Constants.DELIMITER +
                        DeviceHeight;

                RemoteControlFragment.sendMessage(message);
            }
        }
    };

    private void sendMessage(String message) {
        if (isConnected && out != null) {
            // Send message to server
            out.println(message);
        }
    }

    private void listen() {
        while (isConnected) {
            if (!isRunning)
                continue;

            try {

                byte[] lengthMsg = new byte[4];
                in.read(lengthMsg);

                int length = ByteBuffer.wrap(lengthMsg).asIntBuffer().get();

                byte[] buf = new byte[length];

                for(int i = 0; i < length; i++)
                    buf[i] = (byte) in.read();

                Bundle bundle = new Bundle();
                bundle.putInt("Mode", MODE_CONTROL);
                bundle.putByteArray("Image", buf);

                Message uiMessage = new Message();
                uiMessage.setData(bundle);
                uiMessage.what = Constants.DO_UI_IMAGE;
                mHandler.sendMessage(uiMessage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}