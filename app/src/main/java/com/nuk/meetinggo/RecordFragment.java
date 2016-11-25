package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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
import java.util.Map;

import static com.nuk.meetinggo.DataUtils.NEW_RECORD_REQUEST;
import static com.nuk.meetinggo.DataUtils.RECORDS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.RECORD_BODY;
import static com.nuk.meetinggo.DataUtils.RECORD_FAVOURED;
import static com.nuk.meetinggo.DataUtils.RECORD_REFERENCE;
import static com.nuk.meetinggo.DataUtils.RECORD_TITLE;
import static com.nuk.meetinggo.DataUtils.deleteRecords;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class RecordFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath;

    // Layout components
    private static ListView listView;
    private ImageButton newRecord;
    private TextView noRecords;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray records; // Main records array
    private static RecordAdapter adapter; // Custom ListView records adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched records

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newRecordButtonBaseYCoordinate; // Base Y coordinate of newRecord button

    private AlertDialog addRecordDialog, viewRecordDialog, editRecordDialog;

    private DetachableResultReceiver mReceiver;

    private LinkCloudTask linkTask;

    private static int GET_RECORDS = 60001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get record file from cloud
        linkTask = new LinkCloudTask(GET_RECORDS, "");
        linkTask.execute((Void) null);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + RECORDS_FILE_NAME);

        // Init records array
        records = new JSONArray();

        // Retrieve from local path
        JSONArray tempRecords = retrieveData(localPath);

        // If not null -> equal main records to retrieved records
        if (tempRecords != null)
            records = tempRecords;

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.listView);
        newRecord = (ImageButton) view.findViewById(R.id.newRecord);
        noRecords = (TextView) view.findViewById(R.id.noRecords);

        if (toolbar != null)
            initToolbar();

        newRecordButtonBaseYCoordinate = newRecord.getY();

        // Initialize RecordAdapter with records array
        adapter = new RecordAdapter(getContext(), records);
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

                // If scrolled up -> hide newRecord button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newRecordButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newRecord button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newRecordButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // If newRecord button clicked -> Start EditRecordFragment intent with NEW_RECORD_REQUEST as request
        newRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecordDialog.show();
            }
        });

        // If no records -> show 'Press + to add new record' text, invisible otherwise
        if (records.length() == 0)
            noRecords.setVisibility(View.VISIBLE);

        else
            noRecords.setVisibility(View.INVISIBLE);

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
                                    newRecordButtonVisibility(false);
                                    // Disable long-click on listView to prevent deletion
                                    listView.setLongClickable(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < records.length(); i++)
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
         * Add record dialog
         *  If not sure -> dismiss
         *  If yes -> check if record title length > 0
         *    If yes -> save current record
         */
        LayoutInflater inflater = LayoutInflater.from(context);
        final View addView = inflater.inflate(R.layout.dialog_add_record, null);

        addRecordDialog = new AlertDialog.Builder(context)
                .setTitle("新增記錄")
                .setView(addView)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If record field not empty -> continue
                        EditText bodyText = (EditText) addView.findViewById(R.id.recordBody);

                        String record = bodyText.getText().toString();
                        if(TextUtils.isEmpty(record))
                            Toast.makeText(context, getString(R.string.error_field_required), Toast.LENGTH_LONG).show();
                        else {
                            linkTask = new LinkCloudTask(NEW_RECORD_REQUEST, record);
                            linkTask.execute((Void) null);
                        }
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    /**
     * If item clicked in list view -> Start viewRecordDialog with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO change to show dialog
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View recordView = inflater.inflate(R.layout.dialog_view_record, null);

        try {
            JSONObject object = records.getJSONObject(position);

            TextView bodyView = (TextView) recordView.findViewById(R.id.recordBody);
            bodyView.setText(object.getString(RECORD_BODY));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        viewRecordDialog = new AlertDialog.Builder(getContext())
                .setTitle("會議記錄")
                .setView(recordView)
                .create();
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
                            // Pass records and checked items for deletion array to 'deleteRecords'
                            records = deleteRecords(records, checkedArray);

                            // Create and set new adapter with new records array
                            adapter = new RecordAdapter(getContext(), records);
                            listView.setAdapter(adapter);

                            // Attempt to save records to local file
                            Boolean saveSuccessful = saveData(localPath, records);

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

                            // If no records -> show 'Press + to add new record' text, invisible otherwise
                            if (records.length() == 0)
                                noRecords.setVisibility(View.VISIBLE);

                            else
                                noRecords.setVisibility(View.INVISIBLE);

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
        newRecordButtonVisibility(false); // Hide newRecord button
        adapter.notifyDataSetChanged(); // Notify adapter to hide favourite buttons

        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        deleteActive = false; // Set deleteActive to false as we finished delete mode
        newRecordButtonVisibility(true); // Show newRecord button
        adapter.notifyDataSetChanged(); // Notify adapter to show favourite buttons
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the newRecord button
     * @param isVisible true to show button, false to hide
     */
    protected void newRecordButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newRecord.animate().cancel();
            newRecord.animate().translationY(newRecordButtonBaseYCoordinate);
        } else {
            newRecord.animate().cancel();
            newRecord.animate().translationY(newRecordButtonBaseYCoordinate + 500);
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
            JSONArray recordsFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main records list
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = null;

                // Get record at position i
                try {
                    record = records.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If record not null and title/body contain query text
                // -> Put in new records array and add i to realIndexes array
                if (record != null) {
                    try {
                        if (record.getString(RECORD_TITLE).toLowerCase().contains(s) ||
                                record.getString(RECORD_REFERENCE).toLowerCase().contains(s)) {

                            recordsFound.put(record);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with recordsFound to refresh ListView
            RecordAdapter searchAdapter = new RecordAdapter(getContext(), recordsFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < records.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new RecordAdapter(getContext(), records);
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
     * and show newRecord button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new RecordAdapter(getContext(), records);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newRecordButtonVisibility(true);
    }

    /**
     * Favourite or un-favourite the record at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of record
     */
    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        // Get record at position and store in newFavourite
        try {
            newFavourite = records.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(RECORD_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If favoured record is not at position 0
                // Sort records array so favoured record is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Copy contents to new sorted array without favoured element
                    for (int i = 0; i < records.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(records.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main records array with new sorted array and reset adapter
                    records = newArray;
                    adapter = new RecordAdapter(context, records);
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // If favoured record was first -> just update object in records array and notify adapter
                else {
                    try {
                        records.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            // If record not favourite -> set favoured to false and notify adapter
            else {
                try {
                    newFavourite.put(RECORD_FAVOURED, false);
                    records.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        }

        // Save records to local file
        saveData(localPath, records);
    }

    /**
     * Change the record at position
     * @param context application context
     * @param record true to favourite, false to un-favourite
     * @param position position of record
     */
    public static void updateRecord(Context context, String record, int position) {
        JSONObject newRecord = null;

        // Get record at position and store in newFavourite
        try {
            newRecord = records.getJSONObject(position);

            if (!record.equals(newRecord.getString(RECORD_BODY))) {
                newRecord.put(RECORD_BODY, record);
                records.put(position, newRecord);
                adapter.notifyDataSetChanged();;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Save records to local file
        saveData(localPath, records);
    }

    /**
     * Callback method when EditRecordFragment finished adding new record or editing existing record
     * @param requestCode requestCode for intent sent, in our case either NEW_RECORD_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from EditRecordFragment
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();
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

        private final String URL_RECORDS = LinkCloud.DOC_INFO + MeetingInfo.meetingID;

        private int requestCode;
        private String mRecord;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(int request, String record) {
            requestCode = request;
            mRecord = record;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (requestCode == GET_RECORDS) {
                try {
                    JSONObject object = LinkCloud.request(URL_RECORDS);
                    Map<String, String> links = LinkCloud.getLink(object);

                    for (String key : links.keySet())
                        Log.i("[DF]", key + " " + links.get(key));

                    // TODO i don't know how to get
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
            else if (requestCode == NEW_RECORD_REQUEST) {
                // Link to cloud
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if (requestCode == NEW_RECORD_REQUEST) {
                    Log.i("[RF]", "New record");
                    JSONObject newRecordObject = null;
                    
                    try {
                        // Add new record to array
                        newRecordObject = new JSONObject();
                        newRecordObject.put(RECORD_TITLE, "");
                        newRecordObject.put(RECORD_BODY, mRecord);
                        newRecordObject.put(RECORD_REFERENCE, "");
                        newRecordObject.put(RECORD_FAVOURED, false);
                        
                        records.put(newRecordObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newRecordObject not null -> notify adapter
                    if (newRecordObject != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, records);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getContext(),
                                    getResources().getString(R.string.toast_new_record),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        // If no records -> show 'Press + to add new record' text, invisible otherwise
                        if (records.length() == 0)
                            noRecords.setVisibility(View.VISIBLE);

                        else
                            noRecords.setVisibility(View.INVISIBLE);
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
