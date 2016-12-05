package com.nuk.meetinggo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Thread mThread;

    LinkCloudTask linkTask;

    private Map<String, String> meetingInfo = new HashMap<>();

    public static Boolean isStop = false;

    MenuFragment menuFragment = new MenuFragment();
    MenuListFragment menuListFragment = new MenuListFragment();
    MenuSettingFragment menuSettingFragment = new MenuSettingFragment();

    public static String LINK_DATA;
    public static String LINK_CREATE_MEETING;
    public static String LINK_BEGIN_MEETING;
    public static String LINK_OLD_MEETING;
    public static String LINK_FUTURE_MEETING;

    private final static String CONTENT_BEGIN = "2";
    private final static String CONTENT_OLD = "3";
    private final static String CONTENT_FUTURE = "4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //Set the fragment initially
        MenuFragment fragment = new MenuFragment();
        android.support.v4.app.FragmentTransaction fragmentTransaction =
                getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            try {
                Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

                if(connectCloud) {
                    LINK_DATA = bundle.getString(Constants.TAG_LINK_DATA);
                    Log.i("[MA]", "Loading cloud data");
                    JSONObject content = LinkCloud.getJSON(LINK_DATA);
                    meetingInfo = LinkCloud.getLink(content);
                }

                // TODO remove debug msg
                for(String key : meetingInfo.keySet()) {
                    Log.i("[MA]map", key + " " + meetingInfo.get(key));
                }

                LINK_BEGIN_MEETING = meetingInfo.get(CONTENT_BEGIN);
                LINK_OLD_MEETING = meetingInfo.get(CONTENT_OLD);
                LINK_FUTURE_MEETING = meetingInfo.get(CONTENT_FUTURE);

                linkTask = new LinkCloudTask(LINK_OLD_MEETING);
                linkTask.execute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            // TODO read from local json/sql
            Log.i("[MA]", "No cloud data");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStop = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_meeting_join) {
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, menuFragment);
            fragmentTransaction.commit();
        } else if (id == R.id.nav_meeting_list) {
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, menuListFragment);
            fragmentTransaction.commit();
        } else if (id == R.id.nav_settings) {
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, menuSettingFragment);
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private String mUrl;

        private JSONObject mObject;

        LinkCloudTask(String url) {
            mUrl = url;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mObject = LinkCloud.request(mUrl);
                Log.i("[MA]", mObject.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            linkTask = null;
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
