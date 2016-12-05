package com.nuk.meetinggo;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_CODE;
import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_TYPE;
import static com.nuk.meetinggo.DataUtils.MEETING_CHAIRMAN;
import static com.nuk.meetinggo.DataUtils.MEETING_DATE;
import static com.nuk.meetinggo.DataUtils.MEETING_FAVOURED;
import static com.nuk.meetinggo.DataUtils.MEETING_ID;
import static com.nuk.meetinggo.DataUtils.MEETING_TIME;
import static com.nuk.meetinggo.DataUtils.MEETING_TITLE;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MenuActivity.LINK_BEGIN_MEETING;
import static com.nuk.meetinggo.MenuActivity.LINK_DATA;
import static com.nuk.meetinggo.MenuActivity.LINK_FUTURE_MEETING;
import static com.nuk.meetinggo.MenuActivity.isStop;


/**
 * A simple {@link Fragment} subclass.
 */
public class FutureMeetingFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, SearchView.OnQueryTextListener,
        DetachableResultReceiver.Receiver {

    private static File localPath, backupPath;

    // Layout components
    private static ListView beginList;
    private static ListView futureList;
    private ImageButton newMeeting;
    private TextView noMeetings;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray beginMeetings; // Begining meetings array
    private static MeetingInfoAdapter beginAdapter; // Custom ListView meetings adapter
    private static JSONArray futureMeetings; // Future meetings array
    private static MeetingInfoAdapter futureAdapter; // Custom ListView meetings adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched meetings

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newMeetingButtonBaseYCoordinate; // Base Y coordinate of newMeeting button

    private final static int UPDATE_PERIOD = 5000;
    private final static int TYPE_BEGIN = 6001;
    private final static int TYPE_FUTURE = 6002;
    private JSONObject newObject, oldBegin, oldFuture;
    
    private LinkCloudTask linkTask;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //mCommunicator = (ActivityCommunicator) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        //localPath = new File(getContext().getFilesDir() + "/" + meetingID + MEETINGS_FILE_NAME);
        
        beginMeetings = new JSONArray();
        futureMeetings = new JSONArray();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_future_meeting, container, false);

        // Init layout components
        beginList = (ListView) view.findViewById(R.id.beginList);
        futureList = (ListView) view.findViewById(R.id.futureList);
        newMeeting = (ImageButton) view.findViewById(R.id.newMeeting);
        noMeetings = (TextView) view.findViewById(R.id.noMeetings);

        if (toolbar != null)
            initToolbar();

        newMeetingButtonBaseYCoordinate = newMeeting.getY();

        // Initialize MeetingInfoAdapter with beginning meetings array
        beginAdapter = new MeetingInfoAdapter(getContext(), beginMeetings);
        beginList.setAdapter(beginAdapter);

        // Set item click, multi choice and scroll listeners
        beginList.setOnItemClickListener(this);
        
        // Initialize MeetingInfoAdapter with future meetings array
        futureAdapter = new MeetingInfoAdapter(getContext(), futureMeetings);
        futureList.setAdapter(futureAdapter);

        // Set item click, multi choice and scroll listeners
        futureList.setOnItemClickListener(this);
        futureList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // If scrolled up -> hide newMeeting button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newMeetingButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newMeeting button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newMeetingButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });

        // If newMeeting button clicked -> Start CreateMeetingActivity intent with LINK_DATA as request
        newMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateMeetingActivity.class);
                intent.putExtra(Constants.TAG_LINK_DATA, LINK_DATA);
                startActivity(intent);
            }
        });

        // If no meetings -> show 'Press + to add new meeting' text, invisible otherwise
        if (futureMeetings.length() == 0)
            noMeetings.setVisibility(View.VISIBLE);

        else
            noMeetings.setVisibility(View.INVISIBLE);

        initThread();

        return view;
    }

    /**
     * Initialize toolbar with required components such as
     * - title, menu/OnMenuItemClickListener and searchView -
     */
    protected void initToolbar() {
        toolbar.setTitle(R.string.app_name);

        // Inflate menu_main to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_main);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            // Get 'Search' menu item
            searchMenu = menu.findItem(R.id.action_search);

            if (searchMenu != null) {
                // If the item menu not null -> get it's support action view
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);

                if (searchView != null) {
                    // If searchView not null -> set query hint and open/query/close listeners
                    searchView.setQueryHint(getString(R.string.action_search));
                    searchView.setOnQueryTextListener(this);

                    MenuItemCompat.setOnActionExpandListener(searchMenu,
                            new MenuItemCompat.OnActionExpandListener() {

                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                    searchActive = true;
                                    newMeetingButtonVisibility(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < futureMeetings.length(); i++)
                                        realIndexesOfSearchResults.add(i);

                                    futureAdapter.notifyDataSetChanged();

                                    return true;
                                }

                                @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                    searchEnded();
                                    return true;
                                }
                            });
                }
            }
        }
    }

    // Init cloud listener in background
    protected void initThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (isStop) return;
                        if (!isAdded()) return;

                        try {
                            newObject = LinkCloud.request(LINK_FUTURE_MEETING);

                            if (newObject == null) {
                                Log.i("[FMF]", "Link error: " + LINK_FUTURE_MEETING);
                                return;
                            }
                            Log.i("[FMF]", "Receive:" + newObject.toString());

                            if (oldFuture == null
                                    || (!newObject.toString().equals(oldFuture.toString()))) {
                                // Cloud data change, inform current fragment change view
                                Bundle bundle = new Bundle();
                                bundle.putInt(CLOUD_UPDATE_TYPE, TYPE_FUTURE);
                                bundle.putString(CLOUD_UPDATE_CODE, newObject.toString());

                                Log.i("[FMF]", "Update future information");
                                onReceiveResult(CLOUD_UPDATE, Activity.RESULT_OK, bundle);

                                oldFuture = newObject;
                            }
                            
                            newObject = LinkCloud.request(LINK_BEGIN_MEETING);

                            if (newObject == null) {
                                Log.i("[FMF]", "Link error: " + LINK_BEGIN_MEETING);
                                return;
                            }
                            Log.i("[FMF]", "Receive:" + newObject.toString());

                            if (oldBegin == null
                                    || (!newObject.toString().equals(oldBegin.toString()))) {
                                // Cloud data change, inform current fragment change view
                                Bundle bundle = new Bundle();
                                bundle.putInt(CLOUD_UPDATE_TYPE, TYPE_BEGIN);
                                bundle.putString(CLOUD_UPDATE_CODE, newObject.toString());

                                Log.i("[FMF]", "Update begin information");
                                onReceiveResult(CLOUD_UPDATE, Activity.RESULT_OK, bundle);

                                oldBegin = newObject;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, UPDATE_PERIOD);
            }
        }).start();
    }

    /**
     * If item clicked in list view -> Start MeetingActivity intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        JSONArray meetings;
        if(parent.getAdapter().equals(beginAdapter))
            meetings = beginMeetings;
        else
            meetings = futureMeetings;

        // Create activity and give it an argument
        Intent intent = new Intent(getActivity(), MeetingActivity.class);

        Bundle args = new Bundle();

        // If search is active -> use position from realIndexesOfSearchResults for MeetingActivity
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                // Package selected meeting content and send to MeetingActivity
                args.putBoolean(Constants.TAG_INITIALIZED, true);
                args.putBoolean(Constants.TAG_CONNECTION, true);
                args.putString(Constants.TAG_LINK, LinkCloud.MEETING_INFO + meetings.getJSONObject(newPosition).getInt(MEETING_ID));

                MeetingInfo.meetingTitle = meetings.getJSONObject(newPosition).getString(MEETING_TITLE);
                MeetingInfo.meetingDate = meetings.getJSONObject(newPosition).getString(MEETING_DATE);
                MeetingInfo.meetingTime = meetings.getJSONObject(newPosition).getString(MEETING_TIME);
                //MeetingInfo.meetingPosition = meetings.getJSONObject(newPosition).getString(MEETING_POSITION);
                MeetingInfo.controller = meetings.getJSONObject(newPosition).getString(MEETING_CHAIRMAN);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // If search is not active -> use normal position for MeetingActivity
        else {
            try {
                // Package selected meeting content and send to MeetingActivity
                args.putBoolean(Constants.TAG_INITIALIZED, true);
                args.putBoolean(Constants.TAG_CONNECTION, true);
                args.putString(Constants.TAG_LINK, LinkCloud.MEETING_INFO + meetings.getJSONObject(position).getInt(MEETING_ID));

                MeetingInfo.meetingTitle = meetings.getJSONObject(position).getString(MEETING_TITLE);
                MeetingInfo.meetingDate = meetings.getJSONObject(position).getString(MEETING_DATE);
                MeetingInfo.meetingTime = meetings.getJSONObject(position).getString(MEETING_TIME);
                //MeetingInfo.meetingPosition = meetings.getJSONObject(position).getString(MEETING_POSITION);
                MeetingInfo.controller = meetings.getJSONObject(position).getString(MEETING_CHAIRMAN);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        intent.putExtras(args);

        startActivity(intent);
    }

    /**
     * Item clicked in Toolbar menu callback method
     * @param menuItem Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    /**
     * Method to show and hide the newMeeting button
     * @param isVisible true to show button, false to hide
     */
    protected void newMeetingButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newMeeting.animate().cancel();
            newMeeting.animate().translationY(newMeetingButtonBaseYCoordinate);
        } else {
            newMeeting.animate().cancel();
            newMeeting.animate().translationY(newMeetingButtonBaseYCoordinate + 500);
        }
    }

    /**
     * Callback method for 'searchView' menu item widget text change
     * @param s String which changed
     * @return true if text changed and logic finished, false otherwise
     */
    @Override
    public boolean onQueryTextChange(String s) {
        s = s.toLowerCase(); // Turn string into lowercase

        // If query text length longer than 0
        if (s.length() > 0) {
            // Create new JSONArray and reset realIndexes array
            JSONArray meetingsFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main meetings list
            for (int i = 0; i < futureMeetings.length(); i++) {
                JSONObject meeting = null;

                // Get meeting at position i
                try {
                    meeting = futureMeetings.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If meeting not null and title/body contain query text
                // -> Put in new meetings array and add i to realIndexes array
                if (meeting != null) {
                    try {
                        if (meeting.getString(MEETING_TITLE).toLowerCase().contains(s)) {

                            meetingsFound.put(meeting);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with meetingsFound to refresh ListView
            MeetingInfoAdapter searchAdapter = new MeetingInfoAdapter(getContext(), meetingsFound);
            futureList.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < futureMeetings.length(); i++)
                realIndexesOfSearchResults.add(i);

            futureAdapter = new MeetingInfoAdapter(getContext(), futureMeetings);
            futureList.setAdapter(futureAdapter);
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }


    /**
     * When search mode is finished
     * Collapse searchView widget, searchActive to false, reset adapter, enable listView long clicks
     * and show newMeeting button
     */
    protected void searchEnded() {
        searchActive = false;
        futureAdapter = new MeetingInfoAdapter(getContext(), futureMeetings);
        futureList.setAdapter(futureAdapter);
        futureList.setLongClickable(true);
        newMeetingButtonVisibility(true);
    }

    /**
     * Favourite or un-favourite the meeting at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of meeting
     */
    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        // Get meeting at position and store in newFavourite
        try {
            newFavourite = futureMeetings.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            // Set favoured
            try {
                newFavourite.put(MEETING_FAVOURED, true);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                futureMeetings.put(position, newFavourite);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            futureAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Callback method when MeetingActivity finished adding new meeting or editing existing meeting
     * @param requestCode requestCode for intent sent, in our case either NEW_MEETING_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from MeetingActivity
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();

            if (resultData != null) {
                linkTask = new LinkCloudTask(requestCode, resultCode, resultData);
                linkTask.execute((Void) null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private int requestCode;
        private int resultCode;
        private Bundle resultData;

        private int mType;
        private Boolean mLinkSuccess;
        private String mLinkData;

        private final String CONTENT_BEGIN_OBJECT = "obj_meeting_now_list";
        private final String CONTENT_FUTURE_OBJECT = "obj_meeting_list";
        private final String CONTENT_TITLE = "topic";
        private final String CONTENT_DATE = "meeting_day";
        private final String CONTENT_TIME = "meeting_time";
        private final String CONTENT_CHAIRMAN = "moderator";
        private final String CONTENT_ID = "meeting_id";

        LinkCloudTask(int request, int result, Bundle data) {
            requestCode = request;
            resultCode = result;
            resultData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Link cloud to save, if success than add to array
            try {
                // Cloud meeting data
                if (requestCode == CLOUD_UPDATE) {
                    mType = resultData.getInt(CLOUD_UPDATE_TYPE);
                    JSONObject request = new JSONObject(resultData.getString(CLOUD_UPDATE_CODE));

                    JSONObject info = LinkCloud.getContent(request);
                    Log.i("[MF]", "info:" + info.toString());

                    JSONObject object = null;

                    if (info.has(CONTENT_BEGIN_OBJECT) || info.has(CONTENT_FUTURE_OBJECT)) {
                        object = info.has(CONTENT_BEGIN_OBJECT) ?
                                info.getJSONObject(CONTENT_BEGIN_OBJECT) : info.getJSONObject(CONTENT_FUTURE_OBJECT);

                        JSONArray title = null;
                        JSONArray date = null;
                        JSONArray time = null;
                        JSONArray chairman = null;
                        JSONArray id = null;
                        if (object.has(CONTENT_TITLE))
                            title = object.getJSONArray(CONTENT_TITLE);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_TITLE);

                        if (object.has(CONTENT_DATE))
                            date = object.getJSONArray(CONTENT_DATE);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_DATE);

                        if (object.has(CONTENT_TIME))
                            time = object.getJSONArray(CONTENT_TIME);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_TIME);

                        if (object.has(CONTENT_CHAIRMAN))
                            chairman = object.getJSONArray(CONTENT_CHAIRMAN);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_CHAIRMAN);

                        if (object.has(CONTENT_ID))
                            id = object.getJSONArray(CONTENT_ID);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_ID);

                        if (title != null && id != null && date != null && time != null && chairman != null) {
                            if (title.length() == id.length()) {
                                JSONArray meetings = null;

                                if (mType == TYPE_BEGIN)
                                    meetings = beginMeetings;
                                else
                                    meetings = futureMeetings;

                                // Update meetings, check meeting id is either existed or not
                                // Yes -> update data, no -> add new meeting
                                for(int i = 0; i < id.length(); i++) {
                                    int position = -1;
                                    for(int j = 0; j < meetings.length(); j++) {
                                        if(meetings.getJSONObject(j).has(MEETING_ID)
                                                && id.getInt(i) == meetings.getJSONObject(j).getInt(MEETING_ID)) {
                                            position = j;
                                            break;
                                        }
                                    }

                                    JSONObject meeting = null;
                                    // Add new meeting
                                    if (position < 0) {
                                        meeting = new JSONObject();

                                        // TODO add body
                                        meeting.put(MEETING_ID, id.getInt(i));
                                        meeting.put(MEETING_TITLE, title.getString(i));
                                        meeting.put(MEETING_DATE, date.getString(i));
                                        meeting.put(MEETING_TIME, time.getString(i));
                                        meeting.put(MEETING_CHAIRMAN, chairman.getString(i));
                                        meeting.put(MEETING_FAVOURED, false);

                                        meetings.put(meeting);
                                    }
                                    // Update existed meeting
                                    else {
                                        meeting = meetings.getJSONObject(position);

                                        meeting.put(MEETING_TITLE, title.getString(i));
                                        meeting.put(MEETING_DATE, date.getString(i));
                                        meeting.put(MEETING_TIME, time.getString(i));
                                        meeting.put(MEETING_CHAIRMAN, chairman.getString(i));

                                        meetings.put(position, meeting);
                                    }
                                }

                                Thread.sleep(2000);

                                return true;
                            }
                            else
                                Log.i("[MF]", "Field length aren't the same in array");
                        }
                        else
                            Log.i("[MF]", "Loading object content error");
                    }
                    else
                        Log.i("[MF]", "No content key " + CONTENT_BEGIN_OBJECT + " and " + CONTENT_FUTURE_OBJECT);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if (requestCode == CLOUD_UPDATE) {
                    // Update meeting list view
                    if (mType == TYPE_BEGIN)
                        beginAdapter.notifyDataSetChanged();
                    else
                        futureAdapter.notifyDataSetChanged();

                    if (futureMeetings.length() == 0)
                        noMeetings.setVisibility(View.VISIBLE);
                    else
                        noMeetings.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
