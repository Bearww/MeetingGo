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
import android.widget.Toast;

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
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_SETTING;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_TOPIC;
import static com.nuk.meetinggo.MeetingInfo.meetingID;
import static com.nuk.meetinggo.MeetingInfo.topicID;
import static com.nuk.meetinggo.DataUtils.NOTE_ID;
import static com.nuk.meetinggo.DataUtils.NOTE_BODY;

public class MeetingActivity extends AppCompatActivity implements ActivityCommunicator {

    private static Context context;
    private PagerAdapter adapter;

    private CloudListener mListener;
    private Thread mThread;

    private static Toolbar toolbar;
    private static TabLayout tabLayout;
    private int currentFragment = -1;

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

        topicID = 0;

        setContentView(R.layout.activity_meeting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        tabLayout.addTab(tabLayout.newTab().setText("議題").setTag(TAG_TAB_TOPIC));
        tabLayout.addTab(tabLayout.newTab().setText("成員").setTag(TAG_TAB_MEMBER));
        tabLayout.addTab(tabLayout.newTab().setText("文件").setTag(TAG_TAB_DOCUMENT));
        tabLayout.addTab(tabLayout.newTab().setText("問題").setTag(TAG_TAB_QUESTION));
        tabLayout.addTab(tabLayout.newTab().setText("投票").setTag(TAG_TAB_POLL));
        tabLayout.addTab(tabLayout.newTab().setText("設定").setTag(TAG_TAB_SETTING));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayoutBaseYCoordinate = tabLayout.getY();

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                currentFragment = tab.getPosition();
                mListener.fragmentChanged(adapter.getItem(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        currentFragment = 0;

        Boolean inited = false;
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

            if(connectCloud) {
                inited = bundle.getBoolean(Constants.TAG_INITIALIZED);
                GET_MEETING_INFO = LinkCloud.filterLink(bundle.getString(Constants.TAG_LINK));
                meetingID = LinkCloud.getMeetingID(GET_MEETING_INFO);
                Log.i("[MA]", "Loading cloud data " + GET_MEETING_INFO);
            }
        }
        else {
            Log.i("[MA]", "No cloud data");
        }

        mListener = new CloudListener(new Handler());

        linkTask = new LinkCloudTask(inited, GET_MEETING_INFO);
        linkTask.execute((Void) null);
    }

    @Override
    protected void onPause() {
        mListener.listenStop = true;

        super.onPause();
    }

    @Override
    protected void onResume() {
        topicID = 0;
        mListener.listenStop = false;

        if (currentFragment != -1)
            mListener.fragmentChanged(adapter.getItem(currentFragment));

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
        Fragment fragment = adapter.getItem(currentFragment);

        if(fragment instanceof IOnFocusListenable) {
            ((IOnFocusListenable) fragment).onWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void passDataToActivity(Bundle bundle) {
        Intent intent = new Intent(this, RemoteActivity.class);

        if (bundle != null) {
            intent.putExtra(NOTE_ID, bundle.getInt(NOTE_ID));
            intent.putExtra(NOTE_BODY, bundle.getString(NOTE_BODY));
        }

        startActivity(intent);
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

        private Boolean mInit;
        private String mUrl;
        private JSONObject mContent;
        private JSONObject mObject;
        private JSONObject mLink;
        private JSONObject mForm;

        private int mTimes;
        private int MAX_RETRY_TIMES = 5;
        private int LINK_SUCCESS = 6000;

        LinkCloudTask(Boolean init, String url) {
            mInit = init;
            mUrl = url;
            mTimes = 0;
        }

        LinkCloudTask(String url, int times) {
            mInit = true;
            mUrl = url;
            mTimes = times;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (!TextUtils.isEmpty(mUrl)) {
                try {
                    mObject = LinkCloud.request(mUrl);

                    if (mObject != null && mObject.has(CONTENT_LINK) && mObject.has(CONTENT_FORM)) {
                        if (!mInit) mContent = mObject.getJSONObject("contents");
                        mLink = mObject.getJSONObject(CONTENT_LINK);
                        mForm = mObject.getJSONObject(CONTENT_FORM);
                        return true;
                    } else
                        Log.i("[MA]", "No content "
                                + (mObject.has(CONTENT_LINK) ? "" : CONTENT_LINK) + " "
                                + (mObject.has(CONTENT_FORM) ? "" : CONTENT_FORM));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;
            Boolean finish = true;
            if (success) {
                if (!mInit && mContent != null) {
                    try {
                        MeetingInfo.controller = mContent.getString("moderator");
                        MeetingInfo.meetingDate = mContent.getString("date");
                        MeetingInfo.meetingTime = mContent.getString("time");
                    } catch (JSONException e) {
                        Log.i("[MA]", "Fail to get meeting info");
                        e.printStackTrace();
                    }
                }
                if (mForm != null) {
                    try {
                        if (mForm.has(CONTENT_ANSWER)) {
                            mObject = mForm.getJSONObject(CONTENT_ANSWER);
                            GET_MEETING_ANSWER = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_ANSWER);
                            GET_MEETING_ANSWER = "";
                            finish = false;
                        }

                        if (mForm.has(CONTENT_TOPIC_BODY)) {
                            mObject = mForm.getJSONObject(CONTENT_TOPIC_BODY);
                            GET_TOPIC_BODY = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_TOPIC_BODY);
                            GET_TOPIC_BODY = "";
                            finish = false;
                        }

                        if (mForm.has(CONTENT_TOPIC_DOCUMENT)) {
                            mObject = mForm.getJSONObject(CONTENT_TOPIC_DOCUMENT);
                            GET_TOPIC_DOCUMENT = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_TOPIC_DOCUMENT);
                            GET_TOPIC_DOCUMENT = "";
                            finish = false;
                        }

                        if (mForm.has(CONTENT_POLL)) {
                            mObject = mForm.getJSONObject(CONTENT_POLL);
                            GET_MEETING_POLL = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_POLL);
                            GET_MEETING_POLL = "";
                            finish = false;
                        }

                        if (mForm.has(CONTENT_QUESTION)) {
                            mObject = mForm.getJSONObject(CONTENT_QUESTION);
                            GET_MEETING_QUESTION = (!mObject.has(CONTENT_ADDRESS) ? "" : mObject.getString(CONTENT_ADDRESS));
                        }
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_QUESTION);
                            GET_MEETING_QUESTION = "";
                            finish = false;
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
                            finish = false;
                        }

                        if (mLink.has(CONTENT_TOPIC))
                            GET_MEETING_TOPIC = mLink.getString(CONTENT_TOPIC);
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_TOPIC);
                            GET_MEETING_TOPIC = "";
                            finish = false;
                        }

                        if (mLink.has(CONTENT_DOCUMENT))
                            GET_MEETING_DOCUMENT = mLink.getString(CONTENT_DOCUMENT);
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_DOCUMENT);
                            GET_MEETING_DOCUMENT = "";
                            finish = false;
                        }

                        if (mLink.has(CONTENT_MEMBER))
                            GET_MEETING_MEMBER = mLink.getString(CONTENT_MEMBER);
                        else {
                            Log.i("[MA]", "Fail to fetch link " + CONTENT_MEMBER);
                            GET_MEETING_MEMBER = "";
                            finish = false;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (finish) {
                    mTimes = LINK_SUCCESS;
                    Toast.makeText(getApplicationContext(), "連結成功", Toast.LENGTH_LONG).show();
                    mListener.fragmentChanged(adapter.getItem(currentFragment));

                    mThread = new Thread(mListener);
                    mThread.start();
                }
            }
            else {
                Log.i("[MA]", "Fail to fetch meeting update link");
            }

            if (mTimes < MAX_RETRY_TIMES) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                linkTask = new LinkCloudTask(mUrl, mTimes + 1);
                linkTask.execute();
            }
            else if (mTimes != LINK_SUCCESS) {
                Toast.makeText(getApplicationContext(), "連結失敗，請稍後嘗試", Toast.LENGTH_LONG).show();
            }
        }
    }
}
