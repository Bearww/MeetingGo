package com.nuk.meetinggo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class GroupActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    TextView mySpaceTextView;
    TextView groupSpaceTextView;
    TextView groupChatTextView;
    TextView addMemberTextView;
    TextView dateText;
    TextView timeText;

    Button createButton;

    LinkCloudTask linkTask = null;

    Bundle bundle;
    private Map<String, String> groupLink = new HashMap<>();
    private Map<String, String> groupForm = new HashMap<>();

    private static int CLOUD_ADD_MEMBER = 6001;
    private static int CLOUD_CREATE_MEETING = 6002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        mySpaceTextView = (TextView) findViewById(R.id.mySpaceTextView);
        groupSpaceTextView = (TextView) findViewById(R.id.groupSpaceTextView);
        groupChatTextView = (TextView) findViewById(R.id.groupChatTextView);
        addMemberTextView = (TextView) findViewById(R.id.addMemberTextView);

        dateText = (TextView) findViewById(R.id.dateText);
        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDateDialog();
            }
        });

        timeText = (TextView) findViewById(R.id.timeText);
        timeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTimeDialog();
            }
        });

        createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupActivity.this, MeetingActivity.class);
                startActivity(intent);
            }
        });

        bundle = getIntent().getExtras();
        if(bundle != null) {
            try {
                Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

                if(connectCloud) {
                    String connectData = bundle.getString(Constants.TAG_LINK_DATA);
                    Log.i("[GA]", "Loading cloud data");
                    JSONObject content = LinkCloud.getJSON(connectData);
                    groupLink = LinkCloud.getLink(content);
                    groupForm = LinkCloud.getForm(content);
                }

                for(String key : groupLink.keySet()) {
                    Log.i("[GA]link map", key + " " + groupLink.get(key));
                }

                for(String key : groupForm.keySet()) {
                    Log.i("[GA]form map", key + " " + groupLink.get(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            // TODO read from local json/sql
            Log.i("[GA]", "No cloud data");
        }

        mySpaceTextView.setText(groupLink.get("1"));
        groupSpaceTextView.setText(groupLink.get("2"));
        groupChatTextView.setText(groupLink.get("3"));
        addMemberTextView.setText(groupForm.get("post_link1"));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // Display chosed date
        dateText.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
    }

    private void displayDateDialog() {
        final Calendar calendar = Calendar.getInstance();
        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dateDialog = new DatePickerDialog(this, this, mYear, mMonth, mDay);
        dateDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Display chosed time
        timeText.setText(hourOfDay + ":" + minute);
    }

    private void displayTimeDialog() {
        final Calendar calendar = Calendar.getInstance();
        int mHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(this, this, mHour, mMinute, false);
        timeDialog.show();
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     * the user.
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private Map<String, String> mForm;
        private int mRequestCode;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(Map<String, String> form, int requestCode) {
            mForm = form;
            mRequestCode = requestCode;
            mLinkSuccess = false;
            mLinkData = "";
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {
                if(mRequestCode == CLOUD_ADD_MEMBER) {
                    if(groupForm.containsKey("post_link0")) {
                        mLinkData = LinkCloud.submitFormPost(mForm, groupForm.get("post_link0"));
                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    }
                }
                else if(mRequestCode == CLOUD_CREATE_MEETING) {
                    if(groupForm.containsKey("post_link1")) {
                        mLinkData = LinkCloud.submitFormPost(mForm, groupForm.get("post_link1"));
                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    }
                }
                else
                    return false;

                // Simulate network access.
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if(success) {
                    if(mRequestCode == CLOUD_CREATE_MEETING) {
                        Intent intent = new Intent(GroupActivity.this, MenuActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        if(bundle != null)
                            intent.putExtra("member_list", bundle.getStringArrayList("member_list"));

                        Log.i("[CMA]", "Send to Cloud " + (mLinkSuccess ? "Success" : "Fail"));
                        intent.putExtra(Constants.TAG_CONNECTION, mLinkSuccess);
                        intent.putExtra(Constants.TAG_LINK_DATA, mLinkData);

                        startActivity(intent);
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
