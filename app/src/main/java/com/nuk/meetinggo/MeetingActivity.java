package com.nuk.meetinggo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.nuk.meetinggo.MeetingInfo.CONTENT_ADDRESS;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_ANSWER;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_FORM;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_LINK;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_MEMBER;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_POLL;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_QUESTION;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_START;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_TOPIC;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_TOPIC_BODY;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_TOPIC_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_ANSWER;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_INFO;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_MEMBER;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_POLL;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_QUESTION;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_START;
import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_TOPIC;
import static com.nuk.meetinggo.MeetingInfo.GET_TOPIC_BODY;
import static com.nuk.meetinggo.MeetingInfo.GET_TOPIC_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_MEMBER;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_POLL;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_QUESTION;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_RECORD;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_TOPIC;
import static com.nuk.meetinggo.MeetingInfo.meetingID;
import static com.nuk.meetinggo.MeetingInfo.topicID;

public class MeetingActivity extends AppCompatActivity {

    private static Context context;

    private CloudListener mListener;
    private Thread mThread;

    private static Toolbar toolbar;
    private static TabLayout tabLayout;
    private Fragment currentFragment = null;

    private static float tabLayoutBaseYCoordinate; // Base Y coordinate of tab layout

    LinkCloudTask linkTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        // Android version >= 18 -> set orientation fullUser
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

            // Android version < 18 -> set orientation fullSensor
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // TODO temp setting
        topicID = 0;
        MeetingInfo.controller = MemberInfo.memberID;
        MeetingInfo.presenter = MemberInfo.memberID;
        MeetingInfo.chairman = MemberInfo.memberID;

        setContentView(R.layout.activity_meeting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("議題").setTag(TAG_TAB_TOPIC));
        tabLayout.addTab(tabLayout.newTab().setText("成員").setTag(TAG_TAB_MEMBER));
        tabLayout.addTab(tabLayout.newTab().setText("文件").setTag(TAG_TAB_DOCUMENT));
        tabLayout.addTab(tabLayout.newTab().setText("問題").setTag(TAG_TAB_QUESTION));
        tabLayout.addTab(tabLayout.newTab().setText("投票").setTag(TAG_TAB_POLL));
        tabLayout.addTab(tabLayout.newTab().setText("記錄").setTag(TAG_TAB_RECORD));
        tabLayout.addTab(tabLayout.newTab().setText("測試"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayoutBaseYCoordinate = tabLayout.getY();

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                mListener.fragmentChanged(adapter.getItem(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        currentFragment = adapter.getItem(0);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

            if(connectCloud) {
                GET_MEETING_INFO = LinkCloud.filterLink(bundle.getString(Constants.TAG_LINK));
                meetingID = LinkCloud.getMeetingID(GET_MEETING_INFO);
                Log.i("[MA]", "Loading cloud data" + GET_MEETING_INFO);
            }
        }
        else {
            Log.i("[MA]", "No cloud data");
        }

        mListener = new CloudListener(new Handler());

        linkTask = new LinkCloudTask(GET_MEETING_INFO);
        linkTask.execute((Void) null);
    }

    @Override
    protected void onPause() {
        mListener.listenStop = true;

        super.onPause();
    }

    @Override
    protected void onResume() {
        mListener.listenStop = false;

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mThread != null)
            mThread.interrupt();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_remote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {

        tabLayoutVisibility(true);
        Log.i("[MA]Fragment", "onBackPressed");
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            Log.i("[MA]Fragment", "nothing on backstack");
            super.onBackPressed();
            //additional code
        } else {
            Log.i("[MA]Fragment", "popping backstack");
            getFragmentManager().popBackStack();
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(currentFragment instanceof IOnFocusListenable) {
            ((IOnFocusListenable) currentFragment).onWindowFocusChanged(hasFocus);
        }
    }

    /**
     * Method to show and hide the tab layout
     * @param isVisible true to show tab, false to hide
     */
    public static void tabLayoutVisibility(boolean isVisible) {
        if (isVisible) {
            toolbar.setVisibility(View.VISIBLE);
            tabLayout.animate().cancel();
            tabLayout.animate().translationY(tabLayoutBaseYCoordinate);
        } else {
            toolbar.setVisibility(View.INVISIBLE);
            tabLayout.animate().cancel();
            tabLayout.animate().translationY(tabLayoutBaseYCoordinate + 500);
        }
    }

    /**
     * Method to show different control color
     * @param isVisible true to show control view, false to normal view
     */
    public static void controlViewVisibility(boolean isVisible) {
        if (isVisible) {
            toolbar.setBackgroundColor(context.getResources().getColor(R.color.theme_primary));
            tabLayout.setBackgroundColor(context.getResources().getColor(R.color.theme_primary));
        } else {
            toolbar.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
            tabLayout.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        }
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private String mUrl;
        private JSONObject mObject;
        private JSONObject mLink;
        private JSONObject mForm;

        LinkCloudTask(String url) {
            mUrl = url;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!TextUtils.isEmpty(mUrl)) {
                try {
                    mObject = LinkCloud.request(mUrl);

                    for (int i = 1; i <= 5; i++) {
                        Thread.sleep(500);

                        if (mObject.has(CONTENT_LINK) && mObject.has(CONTENT_FORM)) {
                            mLink = mObject.getJSONObject(CONTENT_LINK);
                            mForm = mObject.getJSONObject(CONTENT_FORM);
                            return true;
                        } else
                            Log.i("[MA]", "Test " + i + ": No content "
                                    + (mObject.has(CONTENT_LINK) ? "" : CONTENT_LINK) + " "
                                    + (mObject.has(CONTENT_FORM) ? "" : CONTENT_FORM));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;
            if (success) {
                if (mForm != null) {
                    try {
                        if (mForm.has(CONTENT_ANSWER)) {
                            mObject = mForm.getJSONObject(CONTENT_ANSWER);
                            GET_MEETING_ANSWER = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_ANSWER);
                            GET_MEETING_ANSWER = "";
                        }

                        if (mForm.has(CONTENT_TOPIC_BODY)) {
                            mObject = mForm.getJSONObject(CONTENT_TOPIC_BODY);
                            GET_TOPIC_BODY = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_TOPIC_BODY);
                            GET_TOPIC_BODY = "";
                        }

                        if (mForm.has(CONTENT_TOPIC_DOCUMENT)) {
                            mObject = mForm.getJSONObject(CONTENT_TOPIC_DOCUMENT);
                            GET_TOPIC_DOCUMENT = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link" + CONTENT_TOPIC_DOCUMENT);
                            GET_TOPIC_DOCUMENT = "";
                        }

                        if (mForm.has(CONTENT_POLL)) {
                            mObject = mForm.getJSONObject(CONTENT_POLL);
                            GET_MEETING_POLL = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link" + CONTENT_POLL);
                            GET_MEETING_POLL = "";
                        }

                        if (mForm.has(CONTENT_QUESTION)) {
                            mObject = mForm.getJSONObject(CONTENT_QUESTION);
                            GET_MEETING_QUESTION = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link" + CONTENT_QUESTION);
                            GET_MEETING_QUESTION = "";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (mLink != null) {
                    try {
                        if (mLink.has(CONTENT_START))
                            GET_MEETING_START = mLink.getString(CONTENT_START);
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_START);
                            GET_MEETING_START = "";
                        }

                        if (mLink.has(CONTENT_TOPIC))
                            GET_MEETING_TOPIC = mLink.getString(CONTENT_TOPIC);
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_TOPIC);
                            GET_MEETING_TOPIC = "";
                        }

                        if (mLink.has(CONTENT_DOCUMENT))
                            GET_MEETING_DOCUMENT = mLink.getString(CONTENT_DOCUMENT);
                        else {
                            Log.i("[MA]", "Fail to fetch link" + CONTENT_DOCUMENT);
                            GET_MEETING_DOCUMENT = "";
                        }

                        if (mLink.has(CONTENT_MEMBER))
                            GET_MEETING_MEMBER = mLink.getString(CONTENT_MEMBER);
                        else {
                            Log.i("[MA]", "Fail to fetch link" + CONTENT_MEMBER);
                            GET_MEETING_MEMBER = "";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Log.i("[MA]", "Start cloud listening thread");
                mListener.fragmentChanged(currentFragment);

                mThread = new Thread(mListener);
                mThread.start();
            }
            else
                Log.i("[MA]", "Fail to fetch meeting update link");
        }
    }
}
