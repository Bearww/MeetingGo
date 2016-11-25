package com.nuk.meetinggo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_CODE;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_MEMBER;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_POLL;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_QUESTION;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_RECORD;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_TOPIC;

public class CloudListener implements Runnable {

    private Handler mHandler;
    private Fragment mFragment;
    private FragmentManager mManager;
    private DetachableResultReceiver mReceiver;

    public static String mUrl = "";

    public static Boolean activityStopped = false;

    private int requestsPerSecond = 2;

    private JSONObject newObject, oldObject;

    public CloudListener(Handler handler, FragmentManager manager) {
        mHandler = handler;
        mManager = manager;
        mReceiver = new DetachableResultReceiver(mHandler);
    }

    public void run() {
        int requests = 10000 / requestsPerSecond;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(linkCloudTask, 0, requests);
    }

    private TimerTask linkCloudTask = new TimerTask() {
        @Override
        public void run() {

            if (activityStopped)
                return;
            try {
                if (!TextUtils.isEmpty(mUrl)) {
                    Log.i("[CL]" + mFragment.toString(), "Request:" + mUrl);
                    newObject = LinkCloud.request(mUrl);
                    if (newObject == null) {
                        Log.i("[CL]" + mFragment.toString(), "Link error: " + mUrl);
                        return;
                    }
                    Log.i("[CL]" + mFragment.toString(), "Receive:" + newObject.toString());

                    if (oldObject == null
                            || (!newObject.toString().equals(oldObject.toString()))) {
                        // Cloud data change, inform current fragment change view
                        Bundle bundle = new Bundle();
                        bundle.putString(CLOUD_UPDATE_CODE, newObject.toString());

                        if (mReceiver != null) {
                            Log.i("[CL]" + mFragment.toString(), "Update information");
                            mReceiver.onReceiveResult(CLOUD_UPDATE, Activity.RESULT_OK, bundle);
                        }

                        oldObject = newObject;
                    }
                }
                else {
                    changeLink();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public void fragmentChanged(Fragment fragment) {
        if (fragment == null)
            return;

        mFragment = fragment;

        if (mFragment instanceof MainFragment)
            mReceiver.setReceiver((MainFragment) mFragment);
        if (mFragment instanceof MemberFragment)
            mReceiver.setReceiver((MemberFragment) mFragment);
        if (mFragment instanceof DocumentFragment)
            mReceiver.setReceiver((DocumentFragment) mFragment);
        if (mFragment instanceof QuestionFragment)
            mReceiver.setReceiver((QuestionFragment) mFragment);
        if (mFragment instanceof PollFragment)
            mReceiver.setReceiver((PollFragment) mFragment);
        if (mFragment instanceof RecordFragment)
            mReceiver.setReceiver((RecordFragment) mFragment);

        changeLink();
    }

    private void changeLink() {
        if (mFragment instanceof MainFragment)
            mUrl = GET_MEETING_TOPIC;
        if (mFragment instanceof MemberFragment)
            mUrl = GET_MEETING_MEMBER;
        if (mFragment instanceof DocumentFragment)
            mUrl = GET_MEETING_DOCUMENT;
        if (mFragment instanceof QuestionFragment)
            mUrl = GET_MEETING_QUESTION;
        if (mFragment instanceof PollFragment)
            mUrl = GET_MEETING_POLL;
        if (mFragment instanceof RecordFragment)
            mUrl = GET_MEETING_RECORD;
    }
}
