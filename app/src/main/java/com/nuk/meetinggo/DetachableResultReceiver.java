package com.nuk.meetinggo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

public class DetachableResultReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public DetachableResultReceiver(Handler handler) {
        super(handler);
    }

    public static final Parcelable.Creator<DetachableResultReceiver> CREATOR
            = new Parcelable.Creator<DetachableResultReceiver>() {
        @Override
        public DetachableResultReceiver createFromParcel(Parcel in) {
            return new DetachableResultReceiver(new Handler());
        }

        @Override
        public DetachableResultReceiver[] newArray(int size) {
            return new DetachableResultReceiver[size];
        }
    };

    public void clearReceiver() {
        mReceiver = null;
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        void onReceiveResult(int requestCode, int resultCode, Bundle resultData);
    }

    protected void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(requestCode, resultCode, resultData);
        }
    }
}
