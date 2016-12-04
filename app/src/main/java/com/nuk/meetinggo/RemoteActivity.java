package com.nuk.meetinggo;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import static com.nuk.meetinggo.DataUtils.NOTE_BODY;
import static com.nuk.meetinggo.DataUtils.NOTE_ID;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_CONTROL;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_DOCUMENT;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_POLL;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_QUESTION;
import static com.nuk.meetinggo.MeetingInfo.TAG_TAB_RECORD;
import static com.nuk.meetinggo.MeetingInfo.topicID;

public class RemoteActivity extends AppCompatActivity {

    public static Boolean isRunning = false;
    private RemoteAdapter adapter;

    private CloudListener mListener;
    private Thread mThread;

    private static Toolbar toolbar;
    private static TabLayout tabLayout;
    private int currentFragment = -1;

    private static float tabLayoutBaseYCoordinate; // Base Y coordinate of tab layout

    public static String topicBody = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android version >= 18 -> set orientation fullUser
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

            // Android version < 18 -> set orientation fullSensor
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        setContentView(R.layout.activity_meeting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("控制").setTag(TAG_TAB_CONTROL));
        tabLayout.addTab(tabLayout.newTab().setText("文件").setTag(TAG_TAB_DOCUMENT));
        tabLayout.addTab(tabLayout.newTab().setText("提問").setTag(TAG_TAB_QUESTION));
        tabLayout.addTab(tabLayout.newTab().setText("投票").setTag(TAG_TAB_POLL));
        tabLayout.addTab(tabLayout.newTab().setText("記錄").setTag(TAG_TAB_RECORD));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        tabLayoutBaseYCoordinate = tabLayout.getY();

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        adapter = new RemoteAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
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

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            topicID = bundle.getInt(NOTE_ID);
            topicBody = bundle.getString(NOTE_BODY);
        }
        else {
            Log.i("[RA]", "No topic data");
        }

        mListener = new CloudListener(new Handler());

        Log.i("[RA]" + topicID, "Start cloud listening thread");
        mListener.setTopic(topicID);
        mListener.fragmentChanged(adapter.getItem(currentFragment));

        mThread = new Thread(mListener);
        mThread.start();
    }

    @Override
    protected void onPause() {
        isRunning = false;
        mListener.listenStop = true;

        super.onPause();
    }

    @Override
    protected void onResume() {
        isRunning = true;
        mListener.listenStop = false;
        if (currentFragment != -1)
            mListener.fragmentChanged(adapter.getItem(currentFragment));

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isRunning = false;
        if (mThread != null)
            mThread.interrupt();
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

    @Override
    public void onBackPressed() {

        tabLayoutVisibility(true);
        Log.i("[RA]Fragment", "onBackPressed");
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            Log.i("[RA]Fragment", "nothing on backstack");
            super.onBackPressed();
            // Additional code
        } else {
            Log.i("[RA]Fragment", "popping backstack");
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
}
