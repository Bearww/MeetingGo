package com.nuk.meetinggo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class MeetingActivity extends AppCompatActivity {

    private static Toolbar toolbar;
    private static TabLayout tabLayout;
    Fragment currentFragment = null;

    private static float tabLayoutBaseYCoordinate; // Base Y coordinate of tab layout

    public static String meetingInfoLink;
    public static Map<String, String> meetingLinks = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android version >= 18 -> set orientation fullUser
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

            // Android version < 18 -> set orientation fullSensor
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // TODO temp setting
        MeetingInfo.controller = MemberInfo.memberID;
        MeetingInfo.presenter = MemberInfo.memberID;
        MeetingInfo.chairman = MemberInfo.memberID;

        setContentView(R.layout.activity_meeting);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("議程"));
        tabLayout.addTab(tabLayout.newTab().setText("成員"));
        tabLayout.addTab(tabLayout.newTab().setText("文件"));
        tabLayout.addTab(tabLayout.newTab().setText("測試"));
        tabLayout.addTab(tabLayout.newTab().setText("測試"));
        tabLayout.addTab(tabLayout.newTab().setText("測試"));
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
                currentFragment = adapter.getItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            try {
                Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

                if(connectCloud) {
                    String connectData = bundle.getString(Constants.TAG_LINK_DATA);

                    Log.i("[MA]", "Loading cloud data");
                    JSONObject content = LinkCloud.getJSON(connectData);
                    meetingLinks = LinkCloud.getLink(content);

                    // Fetch meeting id from connectData, set as static variable
                    if(meetingLinks.containsKey("2")) {
                        meetingID = getMeetingID(meetingLinks.get("2"));
                        meetingInfoLink = LinkCloud.MEETING_INFO + meetingID;
                    }
                    else
                        Log.i("[MA]", "No contain key");
                }

                // TODO remove debug msg
                for(String key : meetingLinks.keySet()) {
                    Log.i("[MA]map", key + " " + meetingLinks.get(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i("[MA]", "No cloud data");
        }
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

    private int getMeetingID(String url) {
        String target = "meeting_id=";
        int lastIndex = url.lastIndexOf(target);
        String id = url.substring(lastIndex + target.length());
        Log.i("[MA]meeting id", id);
        return Integer.parseInt(id);
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
}
