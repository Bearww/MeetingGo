package com.nuk.meetinggo;

import android.graphics.Bitmap;
import android.graphics.Point;

import java.io.ByteArrayOutputStream;

public class Constants {
    //public static final String SERVER_IP = "169.254.156.204";
    //public static final String SERVER_IP = "192.168.137.74";
    public static final String SERVER_IP = "10.0.109.146";
    public static final int SERVER_PORT = 6060;
    public static final int LISTEN_PORT = 6080;
    public static final int SERVER_RECVIMAGE = 6090;

    public static final int DO_UI_IMAGE = 60001;
    public static final int DO_UI_TEXT = 60002;

    public static final String ROW_ID = "row_id";
    public static final String DOC_NAME = "name";
    public static final String DOC_LINK = "link";
    public static final String TAG_CONNECTION = "connection";
    public static final String TAG_LINK_DATA = "link_data";
    public static final String TAG_LINK = "cloud_link";
    public static final String TAG_INITIALIZED = "initialized";

    public static final int FRAMES_PER_SECOND = 1;

    public static final String MOUSE_LEFT_CLICK = "left_click";

    public static final char LEFTMOUSEDOWN = 'a';
    public static final char LEFTMOUSEUP = 'b';

    public static final char RIGHTMOUSEDOWN = 'c';
    public static final char RIGHTMOUSEUP = 'd';

    public static final char LEFTCLICK = 'e';

    public static final char SCROLLUP = 'h';
    public static final char SCROLLDOWN = 'i';

    public static final char ARROW = 'm';
    public static final char BRUSH = 'n';
    public static final char ERASER = 'o';

    public static final char BEFOREPAGE = 'q';
    public static final char NEXTPAGE = 'r';

    public static final char KEYBOARD = 'k';
    public static final char KEYCODE = 'l';

    public static final char DELIMITER = '/';

    public static final char MOVEMOUSE = 'p';

    public static final char REQUESTIMAGE = 'I';
    public static final char PROVIDEIMAGE = 'P';

    /*
        *  Returns a string in the format that can be laster parsed
        *  format: MOVEMOUSEintxDELIMITERinty
        *  ex: 	p5/6
        */
    public static String createMoveMouseMessage(float x, float y) {
        int intx = Math.round(x);
        int inty = Math.round(y);
        return "" + MOVEMOUSE + intx + DELIMITER + inty;
        //return intx + "," + inty;
    }

    public static Point parseMoveMouseMessage(String message) {
        String[] tokens = message.substring(1).split("" + Constants.DELIMITER);
        return new Point(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
    }

    public static byte[] createBitmapMessage(Bitmap map) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        map.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        return stream.toByteArray();
    }
}
