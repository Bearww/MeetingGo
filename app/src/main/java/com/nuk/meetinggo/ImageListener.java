package com.nuk.meetinggo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ImageListener implements Runnable {

    private Handler mHandler;
    private InetAddress serverAddr;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private InputStream in;

    private int framesPerSecond = 1;
    public boolean isConnected = false;

    public static int DeviceWidth = 100;
    public static int DeviceHeight = 100;

    public ImageListener(int port, int fps, Handler handler) {
        framesPerSecond = fps;
        mHandler = handler;

        try {
            serverAddr = InetAddress.getByName(Constants.SERVER_IP);
        } catch (Exception e) {
            Log.e("ClientListener", "C: Error", e);
        }
        serverPort = port;
    }

    public void run() {
        try {
            isConnected = true;
            socket = new Socket(serverAddr, serverPort); // Open socket on server IP and port

            Timer timer = new Timer();
            int frames = 1000 / framesPerSecond;

            timer.scheduleAtFixedRate(getImageTask, 0, frames);
        } catch (Exception e) {
            Log.e("ClientActivity", "Client Connection Error", e);
            isConnected = false;
        }

        try {
            if (isConnected) {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                        .getOutputStream())), true); // Create output stream to send data to server
                in = socket.getInputStream();

                listen();
            }
        } catch (IOException e) {
            Log.e("remotedroid", "Error while creating OutWriter", e);
        }
    }

    private TimerTask getImageTask = new TimerTask() {
        @Override
        public void run() {
            String message = "" +
                    Constants.REQUESTIMAGE +
                    Constants.DELIMITER +
                    DeviceWidth +
                    Constants.DELIMITER +
                    DeviceHeight;

            sendMessage(message);
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
            try {
                //int msgLength = in.readInt();
                //main.leftButton.setText(msgLength);
                //main.testMsg("" + msgLength);

                byte[] lengthMsg = new byte[4];
                in.read(lengthMsg);

                int length = ByteBuffer.wrap(lengthMsg).asIntBuffer().get();

                byte[] buf = new byte[length];
                //in.read(buf);
                for(int i = 0; i < length; i++) {
                    buf[i] = (byte) in.read();
                }

                Bundle bundle = new Bundle();
                bundle.putByteArray("Image", buf);

                Message uiMessage = new Message();
                uiMessage.setData(bundle);
                uiMessage.what = Constants.DO_UI_IMAGE;
                mHandler.sendMessage(uiMessage);

                //Bitmap bm = BitmapFactory.decodeByteArray(buf, 0, length);

                //saveImage(bm);

                //Log.e("REQUESTINGSIZE", "SIZERECV: " + bm.getWidth() + bm.getHeight());
                //main.setImage(bm);
                //sendMessage("App Test");
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