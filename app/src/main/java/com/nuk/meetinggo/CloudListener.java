package com.nuk.meetinggo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private DetachableResultReceiver mReceiver;

    public static String mUrl = "";

    public Boolean listenStop = false;

    public Boolean listenTopic = false;

    private Boolean fragmentAdded = false;

    private int requestsPerSecond = 1;

    private JSONObject newObject, oldObject;

    private Map<String, String> form;

    public CloudListener(Handler handler) {
        mHandler = handler;
        mReceiver = new DetachableResultReceiver(mHandler);
    }

    public void setTopic(int id) {
        listenTopic = true;

        form = new HashMap<>();

        form.put("topic_id", String.valueOf(id));
    }

    public void run() {
        int requests = 3000 / requestsPerSecond;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(linkCloudTask, 0, requests);
    }

    private TimerTask linkCloudTask = new TimerTask() {
        @Override
        public void run() {

            if (listenStop)
                return;

            if (!fragmentAdded) {
                Log.i("[CL]", "Waiting activity add fragment " + mFragment.toString());
                fragmentAdded = mFragment.isAdded();
                return;
            }

            try {
                if (!TextUtils.isEmpty(mUrl)) {
                    Log.i("[CL]" + mFragment.toString(), "Request:" + mUrl);
                    if (listenTopic)
                        newObject = LinkCloud.getJSON(LinkCloud.submitFormPost(form, mUrl));

                    else
                        newObject = LinkCloud.request(mUrl);

                    if (newObject == null) {
                        Log.i("[CL]" + mFragment.toString(), "Link error: " + mUrl);
                        return;
                    }
                    Log.i("[CL]" + mFragment.toString(), "Receive:" + newObject.toString());

                    if (oldObject == null
                            || (newObject.toString().length() > 5)) {
                        if (oldObject != null && newObject.toString().equals(oldObject.toString()))
                            return;

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

        fragmentAdded = mFragment.isAdded();

        changeLink();
    }

    private void changeLink() {
        if (mFragment instanceof MainFragment)
            mUrl = GET_MEETING_TOPIC;
        else if (mFragment instanceof MemberFragment)
            mUrl = GET_MEETING_MEMBER;
        else if (mFragment instanceof DocumentFragment)
            mUrl = GET_MEETING_DOCUMENT;
        else if (mFragment instanceof QuestionFragment)
            mUrl = GET_MEETING_QUESTION;
        else if (mFragment instanceof PollFragment)
            mUrl = GET_MEETING_POLL;
        else if (mFragment instanceof RecordFragment)
            mUrl = GET_MEETING_RECORD;
        else
            mUrl = "";

        if (mFragment instanceof RemoteControlFragment)
            listenStop = true;
        else
            listenStop = false;
    }
}
