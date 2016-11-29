package com.nuk.meetinggo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import org.json.JSONException;

import java.io.IOException;
import java.util.Calendar;

import static com.nuk.meetinggo.MeetingInfo.GET_MEETING_START;

public class SettingFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    EditText meetingTitle, meetingLocation;
    TextView meetingDate, meetingTime;
    Button meetingStart;

    LinkCloudTask linkTask = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        meetingTitle = (EditText) view.findViewById(R.id.titleText);
        meetingLocation = (EditText) view.findViewById(R.id.locationText);
        meetingDate = (TextView) view.findViewById(R.id.dateText);
        meetingTime = (TextView) view.findViewById(R.id.timeText);
        meetingStart = (Button) view.findViewById(R.id.startButton);

        meetingDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDateDialog();
            }
        });

        meetingTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTimeDialog();
            }
        });

        meetingStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setMessage("開始會議？")
                        .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (linkTask != null) {
                                    // If 'Yes' clicked -> inform cloud to start
                                    linkTask = new LinkCloudTask();
                                    linkTask.execute();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            }
        });

        return view;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // Display chosed date
        meetingDate.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
    }

    private void displayDateDialog() {
        final Calendar calendar = Calendar.getInstance();
        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dateDialog = new DatePickerDialog(getContext(), this, mYear, mMonth, mDay);
        dateDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Display chosed time
        meetingTime.setText(hourOfDay + ":" + minute);
    }

    private void displayTimeDialog() {
        final Calendar calendar = Calendar.getInstance();
        int mHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(getContext(), this, mHour, mMinute, false);
        timeDialog.show();
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                LinkCloud.request(GET_MEETING_START);
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
