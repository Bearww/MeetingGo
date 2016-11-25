package com.nuk.meetinggo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreateMeetingActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    EditText meetingNameText;
    TextView dateTextView;
    TextView timeTextView;
    Button createButton;
    CheckBox checkBox;
    Spinner timeSpinner;

    String[] timeType = new String[] {"10分鐘", "30分鐘", "1小時", "2小時"};

    String meetingName;
    String meetingDate;
    String meetingTime;
    Boolean enableTime;
    String informTime;
    LinkCloudTask linkTask = null;

    Map<String, String> createForm = new LinkedHashMap<>();

    private static int CLOUD_REQUEST = 6001;
    private static int CLOUD_CREATE = 6002;
    private static int CLOUD_FIND = 6003;
    private static int CLOUD_SEND = 6004;

    private static String GROUP_NAME = "group_name";
    private static String MEETING_NAME = "meeting_title";
    private static String MEETING_TIME = "meeting_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_meeting);

        meetingNameText = (EditText) findViewById(R.id.meetingNameText);

        dateTextView = (TextView) findViewById(R.id.dateTextView);
        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDateDialog();
            }
        });

        timeTextView = (TextView) findViewById(R.id.timeTextView);
        timeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTimeDialog();
            }
        });

        createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                meetingName = meetingNameText.getText().toString();
                meetingDate = dateTextView.getText().toString();
                meetingTime = timeTextView.getText().toString();
                boolean cancel = false;

                if(TextUtils.isEmpty(meetingName)) {
                    meetingNameText.setError(getString(R.string.error_field_required));
                    meetingNameText.requestFocus();
                    cancel = true;
                }
                if(TextUtils.isEmpty(meetingDate)) {
                    dateTextView.setError(getString(R.string.error_field_required));
                    dateTextView.requestFocus();
                    cancel = true;
                }
                if(TextUtils.isEmpty(meetingTime)) {
                    timeTextView.setError(getString(R.string.error_field_required));
                    timeTextView.requestFocus();
                    cancel = true;
                }

                if(!cancel) {
                    // TODO change to CLOUD_CREATE, if yang's link work
                    // Link to create meeting
                    linkTask = new LinkCloudTask("", CLOUD_SEND);
                    linkTask.execute((Void) null);
                }
            }
        });

        enableTime = true;
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableTime = isChecked;
                if(isChecked)
                    timeSpinner.setVisibility(View.VISIBLE);
                else
                    timeSpinner.setVisibility(View.GONE);
            }
        });

        timeSpinner = (Spinner) findViewById(R.id.timeSpinner);
        ArrayAdapter spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timeType);
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), "您選擇" + id, Toast.LENGTH_LONG).show();
                informTime = timeType[(int) id];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(getApplicationContext(), "您沒有選擇任何項目", Toast.LENGTH_LONG).show();
            }
        };
        timeSpinner.setAdapter(spinnerAdapter);
        timeSpinner.setOnItemSelectedListener(spinnerListener);

        // Get cloud link
        Intent intent = getIntent();
        if(intent != null) {
            String linkData = intent.getStringExtra(Constants.TAG_LINK_DATA);
            linkTask = new LinkCloudTask(linkData, CLOUD_REQUEST);
            linkTask.execute((Void) null);
        }
        else
            Log.i("[CMA]", "No link data");
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private String mLink;
        private int mRequestCode;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(String linkData, int requestCode) {
            mLink = "";
            mLinkData = linkData;
            mRequestCode = requestCode;
            mLinkSuccess = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {
                if(mRequestCode == CLOUD_REQUEST) {
                    Log.i("[CMA]Request", "Create form " + mLinkData);
                    JSONObject object = LinkCloud.request(LinkCloud.filterLink(mLinkData));

                    // Simulate network access.
                    Thread.sleep(2000);

                    createForm = LinkCloud.getForm(object);

                    for(String key : createForm.keySet())
                        Log.i("[CMA]Request form", key);

                    return true;
                }
                else if(mRequestCode == CLOUD_CREATE) {
                    Log.i("[CMA]Create", "send " + meetingName);
                    Map<String, String> form = new HashMap<>();
                    form.put(GROUP_NAME, meetingName);

                    if (createForm.containsKey("post_link0")) {
                        Log.i("[CMA]Create", "send to " + createForm.get("post_link0"));
                        mLinkData = LinkCloud.submitFormPost(form, createForm.get("post_link0"));
                        Log.i("[CMA]Create", "received: " + mLinkData);

                        // Simulate network access.
                        Thread.sleep(2000);

                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    }
                    else
                        Log.i("[CMA]Create Form", "Not contain key value");

                    Log.i("[CMA]Create", "fail");
                    return false;
                }
                else if(mRequestCode == CLOUD_FIND) {
                    Log.i("[CMA]Find", "send");
                    createForm = LinkCloud.getForm(LinkCloud.getJSON(mLink));

                    for(String key : createForm.keySet())
                        Log.i("[CMA]Find form", key);
                    return true;
                }
                else if(mRequestCode == CLOUD_SEND) {
                    Log.i("[CMA]Send", "send");
                    Map<String, String> form = new HashMap<>();
                    form.put(MEETING_NAME, meetingName);
                    form.put(MEETING_TIME, meetingDate + " " + meetingTime);
                    form.put("moderator_id", MemberInfo.memberID);

                    // TODO add field to sql
                    //if(enableTime)
                    //    form.put("inform_time", informTime);
                    //else
                    //    form.put("inform_time", "none");

                    mLink = LinkCloud.submitFormPost(form, createForm.get("post_link0"));
                    if (mLinkSuccess = LinkCloud.hasData())
                        return true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if(mRequestCode == CLOUD_CREATE || mRequestCode == CLOUD_FIND) {
                    try {
                        createForm = LinkCloud.getForm(LinkCloud.getJSON(mLinkData));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    for(String key : createForm.keySet())
                        Log.i("[CMA]form", key);

                    if(mRequestCode == CLOUD_CREATE)
                        linkTask = new LinkCloudTask("", CLOUD_FIND);
                    else
                        linkTask = new LinkCloudTask("", CLOUD_SEND);

                    linkTask.execute((Void) null);
                }
                else if(mRequestCode == CLOUD_SEND) {
                    Intent intent = new Intent(CreateMeetingActivity.this, MeetingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                    Log.i("[CMA]", "Send to Cloud " + (mLinkSuccess ? "Success" : "Fail"));
                    intent.putExtra(Constants.TAG_CONNECTION, mLinkSuccess);
                    intent.putExtra(Constants.TAG_LINK, mLink);

                    startActivity(intent);
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // Display chosed date
        dateTextView.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
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
        timeTextView.setText(hourOfDay + ":" + minute);
    }

    private void displayTimeDialog() {
        final Calendar calendar = Calendar.getInstance();
        int mHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(this, this, mHour, mMinute, false);
        timeDialog.show();
    }
}
