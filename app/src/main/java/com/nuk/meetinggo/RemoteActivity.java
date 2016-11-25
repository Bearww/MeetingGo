package com.nuk.meetinggo;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import static com.nuk.meetinggo.MeetingInfo.topicID;

public class RemoteActivity extends ActionBarActivity {

    private CloudListener mListener;
    private Thread mThread;

    private static Toolbar toolbar;
    private static TabLayout tabLayout;
    private Fragment currentFragment = null;

    private static float tabLayoutBaseYCoordinate; // Base Y coordinate of tab layout

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
        tabLayout.addTab(tabLayout.newTab().setText("控制"));
        tabLayout.addTab(tabLayout.newTab().setText("文件"));
        tabLayout.addTab(tabLayout.newTab().setText("提問"));
        tabLayout.addTab(tabLayout.newTab().setText("投票"));
        tabLayout.addTab(tabLayout.newTab().setText("記錄"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final RemoteAdapter adapter = new RemoteAdapter
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

        mListener = new CloudListener(new Handler());

        Log.i("[RA]" + topicID, "Start cloud listening thread");
        mListener.setTopic(topicID);
        mListener.fragmentChanged(currentFragment);

        mThread = new Thread(mListener);
        mThread.start();
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
}
