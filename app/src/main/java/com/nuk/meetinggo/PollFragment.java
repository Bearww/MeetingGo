package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_CODE;
import static com.nuk.meetinggo.DataUtils.NEW_POLL_REQUEST;
import static com.nuk.meetinggo.DataUtils.POLLS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.POLL_BODY;
import static com.nuk.meetinggo.DataUtils.POLL_COLOUR;
import static com.nuk.meetinggo.DataUtils.POLL_ENABLED;
import static com.nuk.meetinggo.DataUtils.POLL_FAVOURED;
import static com.nuk.meetinggo.DataUtils.POLL_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.POLL_HIDE_BODY;
import static com.nuk.meetinggo.DataUtils.POLL_ID;
import static com.nuk.meetinggo.DataUtils.POLL_RECEIVER;
import static com.nuk.meetinggo.DataUtils.POLL_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.POLL_TITLE;
import static com.nuk.meetinggo.DataUtils.deletePolls;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class PollFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath;

    // Layout components
    private static ListView listView;
    private ImageButton newPoll;
    private TextView noPolls;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray polls; // Main polls array
    private static PollAdapter adapter; // Custom ListView polls adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched polls

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newPollButtonBaseYCoordinate; // Base Y coordinate of newPoll button

    private AlertDialog addPollDialog;

    private DetachableResultReceiver mReceiver;

    private LinkCloudTask linkTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + POLLS_FILE_NAME);

        // Init polls array
        polls = new JSONArray();

        // Retrieve from local path
        JSONArray tempPolls = retrieveData(localPath);

        // If not null -> equal main polls to retrieved polls
        if (tempPolls != null)
            polls = tempPolls;

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poll, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.listView);
        newPoll = (ImageButton) view.findViewById(R.id.newPoll);
        noPolls = (TextView) view.findViewById(R.id.noPolls);

        if (toolbar != null)
            initToolbar();

        newPollButtonBaseYCoordinate = newPoll.getY();

        // Initialize PollAdapter with polls array
        adapter = new PollAdapter(getContext(), polls);
        listView.setAdapter(adapter);

        // Set item click, multi choice and scroll listeners
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // If scrolled up -> hide newPoll button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newPollButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newPoll button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newPollButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // If newPoll button clicked -> Start EditPollFragment intent with NEW_POLL_REQUEST as request
        newPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPollDialog.show();
            }
        });

        // If no polls -> show 'Press + to add new poll' text, invisible otherwise
        if (polls.length() == 0)
            noPolls.setVisibility(View.VISIBLE);

        else
            noPolls.setVisibility(View.INVISIBLE);

        initDialogs(getContext());

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
                                    newPollButtonVisibility(false);
                                    // Disable long-click on listView to prevent deletion
                                    listView.setLongClickable(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < polls.length(); i++)
                                        realIndexesOfSearchResults.add(i);

                                    adapter.notifyDataSetChanged();

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

    /**
     * Implementation of AlertDialogs such as
     * - backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog -
     * @param context The Activity context of the dialogs; in this case MainFragment context
     */
    protected void initDialogs(final Context context) {
        /*
         * Add poll dialog
         *  If not sure -> dismiss
         *  If yes -> check if poll title length > 0
         *    If yes -> save current poll
         */
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_add_poll, null);

        addPollDialog = new AlertDialog.Builder(context)
                .setTitle("新增投票")
                .setView(view)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If poll array not empty -> continue
                        // Check poll field is not empty
                        EditText pollTitle = (EditText) view.findViewById(R.id.pollTitle);
                        EditText pollBody = (EditText) view.findViewById(R.id.pollBody);

                        if(pollTitle.getText().length() == 0) {
                            pollTitle.requestFocus();
                        }
                        else {
                            JSONObject newPollObject = null;

                            try {
                                // Add new poll to array
                                newPollObject = new JSONObject();
                                newPollObject.put(POLL_TITLE, pollTitle.getText().toString());
                                newPollObject.put(POLL_BODY, pollBody.getText().toString());
                                newPollObject.put(POLL_COLOUR, "#FFFFFF");
                                newPollObject.put(POLL_FAVOURED, false);
                                newPollObject.put(POLL_FONT_SIZE, 18);
                                newPollObject.put(POLL_HIDE_BODY, false);
                                newPollObject.put(POLL_ENABLED, false);

                                polls.put(newPollObject);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            // TODO send to server

                            // Update poll list
                            if (newPollObject != null) {
                                adapter.notifyDataSetChanged();

                                Boolean saveSuccessful = saveData(localPath, polls);

                                if (saveSuccessful) {
                                    Toast toast = Toast.makeText(getContext(),
                                            getResources().getString(R.string.toast_new_poll),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                }

                                // If no polls -> show 'Press + to add new poll' text, invisible otherwise
                                if (polls.length() == 0)
                                    noPolls.setVisibility(View.VISIBLE);

                                else
                                    noPolls.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO ask for save modified, then load saved data next time(maybe)
                        dialog.dismiss();
                    }
                })
                .create();
    }

    /**
     * If item clicked in list view -> Start EditPollFragment intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Create fragment and give it an argument
        EditPollFragment nextFragment = new EditPollFragment();
        Bundle args = new Bundle();
        args.putInt(POLL_REQUEST_CODE, NEW_POLL_REQUEST);
        args.putParcelable(POLL_RECEIVER, mReceiver);
        Log.i("[PF]", "Put receiver " + mReceiver.toString());

        // If search is active -> use position from realIndexesOfSearchResults for EditPollFragment
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                // Package selected poll content and send to EditPollFragment
                args.putString(POLL_TITLE, polls.getJSONObject(newPosition).getString(POLL_TITLE));
                args.putString(POLL_BODY, polls.getJSONObject(newPosition).getString(POLL_BODY));
                args.putString(POLL_COLOUR, polls.getJSONObject(newPosition).getString(POLL_COLOUR));
                args.putInt(POLL_FONT_SIZE, polls.getJSONObject(newPosition).getInt(POLL_FONT_SIZE));

                if (polls.getJSONObject(newPosition).has(POLL_HIDE_BODY)) {
                    args.putBoolean(POLL_HIDE_BODY,
                            polls.getJSONObject(newPosition).getBoolean(POLL_HIDE_BODY));
                }

                else
                    args.putBoolean(POLL_HIDE_BODY, false);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(POLL_REQUEST_CODE, newPosition);
        }

        // If search is not active -> use normal position for EditPollFragment
        else {
            try {
                // Package selected poll content and send to EditPollFragment
                args.putString(POLL_TITLE, polls.getJSONObject(position).getString(POLL_TITLE));
                args.putString(POLL_BODY, polls.getJSONObject(position).getString(POLL_BODY));
                args.putString(POLL_COLOUR, polls.getJSONObject(position).getString(POLL_COLOUR));
                args.putInt(POLL_FONT_SIZE, polls.getJSONObject(position).getInt(POLL_FONT_SIZE));

                if (polls.getJSONObject(position).has(POLL_HIDE_BODY)) {
                    args.putBoolean(POLL_HIDE_BODY,
                            polls.getJSONObject(position).getBoolean(POLL_HIDE_BODY));
                }

                else
                    args.putBoolean(POLL_HIDE_BODY, false);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(POLL_REQUEST_CODE, position);
        }

        nextFragment.setArguments(args);

        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.layout_container, nextFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
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
     * During multi-choice menu_delete selection mode, callback method if items checked changed
     * @param mode ActionMode of selection
     * @param position Position checked
     * @param id ID of item, if exists
     * @param checked true if checked, false otherwise
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // If item checked -> add to array
        if (checked)
            checkedArray.add(position);

            // If item unchecked
        else {
            int index = -1;

            // Loop through array and find index of item unchecked
            for (int i = 0; i < checkedArray.size(); i++) {
                if (position == checkedArray.get(i)) {
                    index = i;
                    break;
                }
            }

            // If index was found -> remove the item
            if (index != -1)
                checkedArray.remove(index);
        }

        // Set Toolbar title to 'x Selected'
        mode.setTitle(checkedArray.size() + " " + getString(R.string.action_delete_selected_number));
        adapter.notifyDataSetChanged();
    }

    /**
     * Callback method when 'Delete' icon pressed
     * @param mode ActionMode of selection
     * @param item MenuItem clicked, in our case just action_delete
     * @return true if clicked, false otherwise
     */
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.dialog_delete)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Pass polls and checked items for deletion array to 'deletePolls'
                            polls = deletePolls(polls, checkedArray);

                            // Create and set new adapter with new polls array
                            adapter = new PollAdapter(getContext(), polls);
                            listView.setAdapter(adapter);

                            // Attempt to save polls to local file
                            Boolean saveSuccessful = saveData(localPath, polls);

                            // If save successful -> toast successfully deleted
                            if (saveSuccessful) {
                                Toast toast = Toast.makeText(getContext(),
                                        getResources().getString(R.string.toast_deleted),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            // Smooth scroll to top
                            listView.post(new Runnable() {
                                public void run() {
                                    listView.smoothScrollToPosition(0);
                                }
                            });

                            // If no polls -> show 'Press + to add new poll' text, invisible otherwise
                            if (polls.length() == 0)
                                noPolls.setVisibility(View.VISIBLE);

                            else
                                noPolls.setVisibility(View.INVISIBLE);

                            mode.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }

    // Long click detected on ListView item -> start selection ActionMode (delete mode)
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_delete, menu); // Inflate 'menu_delete' menu
        deleteActive = true; // Set deleteActive to true as we entered delete mode
        newPollButtonVisibility(false); // Hide newPoll button
        adapter.notifyDataSetChanged(); // Notify adapter to hide favourite buttons

        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        deleteActive = false; // Set deleteActive to false as we finished delete mode
        newPollButtonVisibility(true); // Show newPoll button
        adapter.notifyDataSetChanged(); // Notify adapter to show favourite buttons
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the newPoll button
     * @param isVisible true to show button, false to hide
     */
    protected void newPollButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newPoll.animate().cancel();
            newPoll.animate().translationY(newPollButtonBaseYCoordinate);
        } else {
            newPoll.animate().cancel();
            newPoll.animate().translationY(newPollButtonBaseYCoordinate + 500);
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
            JSONArray pollsFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main polls list
            for (int i = 0; i < polls.length(); i++) {
                JSONObject poll = null;

                // Get poll at position i
                try {
                    poll = polls.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If poll not null and title/body contain query text
                // -> Put in new polls array and add i to realIndexes array
                if (poll != null) {
                    try {
                        if (poll.getString(POLL_TITLE).toLowerCase().contains(s) ||
                                poll.getString(POLL_BODY).toLowerCase().contains(s)) {

                            pollsFound.put(poll);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with pollsFound to refresh ListView
            PollAdapter searchAdapter = new PollAdapter(getContext(), pollsFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < polls.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new PollAdapter(getContext(), polls);
            listView.setAdapter(adapter);
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
     * and show newPoll button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new PollAdapter(getContext(), polls);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newPollButtonVisibility(true);
    }

    /**
     * Favourite or un-favourite the poll at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of poll
     */
    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        // Get poll at position and store in newFavourite
        try {
            newFavourite = polls.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(POLL_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If favoured poll is not at position 0
                // Sort polls array so favoured poll is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Copy contents to new sorted array without favoured element
                    for (int i = 0; i < polls.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(polls.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main polls array with new sorted array and reset adapter
                    polls = newArray;
                    adapter = new PollAdapter(context, polls);
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // If favoured poll was first -> just update object in polls array and notify adapter
                else {
                    try {
                        polls.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            // If poll not favourite -> set favoured to false and notify adapter
            else {
                try {
                    newFavourite.put(POLL_FAVOURED, false);
                    polls.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        }

        // Save polls to local file
        saveData(localPath, polls);
    }

    /**
     * On or off the poll at position
     * @param enabled true to display, false to hide
     * @param position position of poll
     */
    public static void setMode(boolean enabled, int position) {
        JSONObject newEnabled = null;

        // Get poll at position and store in newEnabled
        try {
            newEnabled = polls.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newEnabled != null) {
            try {
                newEnabled.put(POLL_ENABLED, enabled);
                polls.put(position, newEnabled);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            adapter.notifyDataSetChanged();

            // TODO inform cloud change poll mode
        }
    }

    /**
     * Callback method when EditPollFragment finished adding new poll or editing existing poll
     * @param requestCode requestCode for intent sent, in our case either NEW_POLL_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from EditPollFragment
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();

            if (resultData != null) {
                Log.i("[PF]", "do something");
                linkTask = new LinkCloudTask(requestCode, resultCode, resultData);
                linkTask.execute((Void) null);
            }
        }

        else if (resultCode == Activity.RESULT_CANCELED) {
            Bundle mBundle = null;

            // If data is not null, has "request" extra and is new poll -> get extras to bundle
            if (resultData != null && requestCode == NEW_POLL_REQUEST) {
                mBundle = resultData;

                // If new poll discarded -> toast empty poll discarded
                if (mBundle.getString("request").equals("discard")) {
                    Toast toast = Toast.makeText(getContext(),
                            getResources().getString(R.string.toast_empty_poll_discarded),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        mReceiver.clearReceiver();
        super.onDestroy();
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private int requestCode;
        private int resultCode;
        private Bundle resultData;

        private Boolean mLinkSuccess;
        private String mLinkData;

        private final String CONTENT_OBJECT = "obj_voting_result";
        private final String CONTENT_TITLE = "head_issue";
        private final String CONTENT_ID = "issue_id";
        private final String CONTENT_COUNT = "member_vote";

        LinkCloudTask(int request, int result, Bundle data) {
            requestCode = request;
            resultCode = result;
            resultData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Link cloud to save, if success than add to array
            try {
                // Cloud poll data
                if (requestCode == CLOUD_UPDATE) {
                    JSONObject request = new JSONObject(resultData.getString(CLOUD_UPDATE_CODE));

                    JSONObject info = LinkCloud.getContent(request);
                    Log.i("[PF]", "info:" + info.toString());

                    JSONObject object = null;

                    if (info.has(CONTENT_OBJECT)) {
                        object = info.getJSONObject(CONTENT_OBJECT);

                        JSONArray title = null;
                        JSONArray id = null;
                        JSONArray count = null;
                        if (object.has(CONTENT_TITLE))
                            title = object.getJSONArray(CONTENT_TITLE);
                        else
                            Log.i("[PF]", "Fail to fetch field " + CONTENT_TITLE);

                        if (object.has(CONTENT_ID))
                            id = object.getJSONArray(CONTENT_ID);
                        else
                            Log.i("[PF]", "Fail to fetch field " + CONTENT_ID);

                        if (object.has(CONTENT_COUNT))
                            count = object.getJSONArray(CONTENT_COUNT);
                        else
                            Log.i("[PF]", "Fail to fetch field " + CONTENT_COUNT);

                        if (title != null && id != null && count != null) {
                            if (title.length() == id.length()) {
                                // Update polls, check poll id is either existed or not
                                // Yes -> update data, no -> add new poll
                                for(int i = 0; i < id.length(); i++) {
                                    int position = -1;
                                    for(int j = 0; j < polls.length(); j++) {
                                        if(polls.getJSONObject(j).has(POLL_ID)
                                                && id.getString(i).equals(polls.getJSONObject(j).getString(POLL_ID))) {
                                            position = j;
                                            break;
                                        }
                                    }

                                    JSONObject poll = null;
                                    // Add new poll
                                    if (position < 0) {
                                        poll = new JSONObject();

                                        // TODO add body
                                        poll.put(POLL_ID, id.getString(i));
                                        poll.put(POLL_TITLE, title.getString(i));
                                        poll.put(POLL_BODY, "");
                                        poll.put(POLL_COLOUR, "#FFFFFF");
                                        poll.put(POLL_FAVOURED, false);
                                        poll.put(POLL_FONT_SIZE, 18);

                                        polls.put(poll);
                                    }
                                    // Update existed poll
                                    else {
                                        poll = polls.getJSONObject(position);

                                        poll.put(POLL_TITLE, title.getString(i));

                                        polls.put(position, poll);
                                    }
                                }

                                Thread.sleep(2000);

                                return true;
                            }
                            else
                                Log.i("[PF]", "Field length aren't the same in array");
                        }
                        else
                            Log.i("[PF]", "Loading object content error");
                    }
                    else
                        Log.i("[PF]", "No content key " + CONTENT_OBJECT);
                }
                
                Log.i("[PF]", "create poll form");
                Map<String, String> form = new HashMap<>();

                // Add new poll to form
                form.put("issue", resultData.getString(POLL_TITLE));
                //form.put("content", resultData.getString(NOTE_BODY));
                //form.put(NOTE_COLOUR, resultData.getString(NOTE_COLOUR));
                //form.put(NOTE_FAVOURED, false);
                //form.put(NOTE_FONT_SIZE, resultData.getInt(NOTE_FONT_SIZE));
                //form.put(NOTE_HIDE_BODY, resultData.getBoolean(NOTE_HIDE_BODY));

                // Save new poll
                if (requestCode == NEW_POLL_REQUEST) {
                    // Insert to database
                    mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_POLL);
                    if (mLinkSuccess = LinkCloud.hasData())
                        return true;
                }
                // Update exsited poll
                else {
                    // TODO change add to update poll in database
                    // Update database
                    mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_POLL);
                    if (mLinkSuccess = LinkCloud.hasData())
                        return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if (requestCode == CLOUD_UPDATE) {
                    // Update poll list view
                    adapter.notifyDataSetChanged();

                    if (polls.length() == 0)
                        noPolls.setVisibility(View.VISIBLE);
                    else
                        noPolls.setVisibility(View.INVISIBLE);
                }

                // If new poll was saved
                else if (requestCode == NEW_POLL_REQUEST) {
                    JSONObject newPollObject = null;

                    try {
                        // Add new poll to array
                        newPollObject = new JSONObject();
                        newPollObject.put(POLL_TITLE, resultData.getString(POLL_TITLE));
                        newPollObject.put(POLL_BODY, resultData.getString(POLL_BODY));
                        newPollObject.put(POLL_COLOUR, resultData.getString(POLL_COLOUR));
                        newPollObject.put(POLL_FAVOURED, false);
                        newPollObject.put(POLL_FONT_SIZE, resultData.getInt(POLL_FONT_SIZE));
                        newPollObject.put(POLL_HIDE_BODY, resultData.getBoolean(POLL_HIDE_BODY));

                        polls.put(newPollObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newPollObject not null -> notify adapter
                    if (newPollObject != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, polls);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getContext(),
                                    getResources().getString(R.string.toast_new_poll),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        // If no polls -> show 'Press + to add new poll' text, invisible otherwise
                        if (polls.length() == 0)
                            noPolls.setVisibility(View.VISIBLE);

                        else
                            noPolls.setVisibility(View.INVISIBLE);
                    }
                }

                // If existing poll was updated (saved)
                else {
                    JSONObject newPollObject = null;

                    try {
                        // Update array item with new poll data
                        newPollObject = polls.getJSONObject(requestCode);
                        newPollObject.put(POLL_TITLE, resultData.getString(POLL_TITLE));
                        newPollObject.put(POLL_BODY, resultData.getString(POLL_BODY));
                        newPollObject.put(POLL_COLOUR, resultData.getString(POLL_COLOUR));
                        newPollObject.put(POLL_FONT_SIZE, resultData.getInt(POLL_FONT_SIZE));
                        newPollObject.put(POLL_HIDE_BODY, resultData.getBoolean(POLL_HIDE_BODY));

                        // Update poll at position 'requestCode'
                        polls.put(requestCode, newPollObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newPollObject not null -> notify adapter
                    if (newPollObject != null) {
                        adapter.notifyDataSetChanged();
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
