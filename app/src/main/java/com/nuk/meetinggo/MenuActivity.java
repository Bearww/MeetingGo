package com.nuk.meetinggo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private MeetingJoinTask mJoinTask = null;

    private EditText meetingID;

    private Map<String, String> meetingInfo = new HashMap<>();

    private static String LINK_CREATE_MEETING = "1";
    private static String LINK_GROUP_LIST = "1";
    private static String LINK_UPLOAD_SPACE = "2";
    private static String LINK_GROUP_UPLOAD_CENTER = "3";
    private static String LINK_LOGOUT = "4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        meetingID = (EditText) findViewById(R.id.editText);

        Button meetingCreateButton = (Button) findViewById(R.id.createButton);
        meetingCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreate();
            }
        });

        Button meetingJoinButton = (Button) findViewById(R.id.joinButton);
        meetingJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptJoin();
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
                    meetingInfo = LinkCloud.getLink(content);
                }

                // TODO remove debug msg
                for(String key : meetingInfo.keySet()) {
                    Log.i("[MA]map", key + " " + meetingInfo.get(key));
                }
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
            // Handle the meeting create/join action
        } else if (id == R.id.nav_meeting_list) {

        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void attemptCreate() {
        if(mJoinTask != null) {
            return;
        }

        Intent intent = new Intent(this, CreateMeetingActivity.class);

        // Put link to get create meeting info
        String link = meetingInfo.get(LINK_CREATE_MEETING);
        Log.i("[MA]", "Next link " + link);
        intent.putExtra(Constants.TAG_LINK, link);

        startActivity(intent);
    }

    private void attemptJoin() {
        if(mJoinTask != null) {
            return;
        }

        // Reset errors.
        meetingID.setError(null);

        // Store values at the time of the login attempt.
        String meeting = meetingID.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid meeting id.
        if (TextUtils.isEmpty(meeting)) {
            meetingID.setError(getString(R.string.error_field_required));
            focusView = meetingID;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt join and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user join attempt.
            //showProgress(true);
            mJoinTask = new MeetingJoinTask(meeting);
            mJoinTask.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous join task
     */
    public class MeetingJoinTask extends AsyncTask<Void, Void, Boolean> {

        private final String mMeetingID;

        private Boolean mLinkSuccess;
        private String mLinkData;

        MeetingJoinTask(String meetingID) {
            mMeetingID = meetingID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                Map<String, String> form = new HashMap<>();
                form.put("meeting_id", mMeetingID);

                mLinkData = LinkCloud.submitFormPost(form, LinkCloud.JOIN_MEETING);

                Log.i("[MA]Create", "received: " + mLinkData);

                // Simulate network access.
                Thread.sleep(2000);

                if (mLinkSuccess = LinkCloud.hasData())
                    return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mJoinTask = null;
            //showProgress(false);

            if (success) {
                finish();
                Intent intent = new Intent(MenuActivity.this, MeetingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                // Put link to MeetingActivity
                Log.i("[MA]", "Send to Cloud " + (mLinkSuccess ? "Success" : "Fail"));
                intent.putExtra(Constants.TAG_CONNECTION, mLinkSuccess);
                intent.putExtra(Constants.TAG_LINK_DATA, mLinkData);

                startActivity(intent);
            } else {
                //mPasswordView.setError(getString(R.string.error_incorrect_password));
                //mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mJoinTask = null;
            //showProgress(false);
        }
    }
}
