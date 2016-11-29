package com.nuk.meetinggo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class MenuFragment extends Fragment {

    private View view;
    private EditText meetingID;

    private MeetingJoinTask mJoinTask = null;

    public MenuFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_menu, container, false);

        meetingID = (EditText) view.findViewById(R.id.editText);

        Button meetingCreateButton = (Button) view.findViewById(R.id.createButton);
        meetingCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCreate();
            }
        });

        Button meetingJoinButton = (Button) view.findViewById(R.id.joinButton);
        meetingJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptJoin();
            }
        });

        return view;
    }

    public void attemptCreate() {
        if(mJoinTask != null) {
            return;
        }

        Intent intent = new Intent(getActivity(), CreateMeetingActivity.class);

        Bundle bundle = getActivity().getIntent().getExtras();
        if(bundle != null) {
            Boolean connectCloud = bundle.getBoolean(Constants.TAG_CONNECTION);

            if(connectCloud) {
                Log.i("[MF]", "Loading cloud data");
                intent.putExtra(Constants.TAG_LINK_DATA, bundle.getString(Constants.TAG_LINK_DATA));
                startActivity(intent);
            }
            else
                Log.i("[MF]", "Can't link cloud");
        }
        else
            Log.i("[MF]", "No cloud data");

    }

    public void attemptJoin() {
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

        private Boolean mLinkSuccess = false;
        private String mLink = "";

        MeetingJoinTask(String meetingID) {
            mMeetingID = meetingID;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                Map<String, String> form = new HashMap<>();
                form.put("meeting_id", mMeetingID);

                mLink = LinkCloud.submitFormPost(form, LinkCloud.JOIN_MEETING);

                Log.i("[MenuF]Create", "received: " + mLink);

                // Simulate network access.
                Thread.sleep(2000);

                if (mLinkSuccess = LinkCloud.hasData()) {
                    // Check meeting id is valid or not
                    int id = LinkCloud.getMeetingID(LinkCloud.filterLink(mLink));

                    if (id < 0) {
                        Log.i("[MA]", "Invalid meeting id");
                        return false;
                    }
                    return true;
                }
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
                Intent intent = new Intent(getActivity(), MeetingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                // Put link to MeetingActivity
                Log.i("[MenuF]", "Send to Cloud " + (mLinkSuccess ? "Success" : "Fail"));
                intent.putExtra(Constants.TAG_CONNECTION, mLinkSuccess);
                intent.putExtra(Constants.TAG_LINK, mLink);

                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "加入會議失敗", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mJoinTask = null;
            //showProgress(false);
        }
    }
}
