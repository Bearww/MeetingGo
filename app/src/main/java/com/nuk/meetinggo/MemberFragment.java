package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.ImageButton;
import android.widget.ListView;
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
import static com.nuk.meetinggo.DataUtils.LIST_MEMBER_REQUEST;
import static com.nuk.meetinggo.DataUtils.MEMBERS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.MEMBER_COLOUR;
import static com.nuk.meetinggo.DataUtils.MEMBER_EMAIL;
import static com.nuk.meetinggo.DataUtils.MEMBER_ID;
import static com.nuk.meetinggo.DataUtils.MEMBER_NAME;
import static com.nuk.meetinggo.DataUtils.NEW_MEMBER_REQUEST;
import static com.nuk.meetinggo.DataUtils.deleteMembers;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MeetingInfo.getControllable;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class MemberFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath, backupPath;

    // Layout components
    private static ListView listView;
    private ImageButton newMember;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray members; // Main members array
    private static JSONArray friends; // Friends array
    private static MemberAdapter adapter; // Custom ListView members adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched members

    public static boolean controlActive = false;
    private MenuItem menuEnableControl;

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newMemberButtonBaseYCoordinate; // Base Y coordinate of newMember button

    private AlertDialog addMemberDialog;

    private DetachableResultReceiver mReceiver;

    private String addMemberLink = "back_end/meeting/set_info/set_meeting_topic.php?meeting_id=" + meetingID;

    private LinkCloudTask linkTask;

    // TODO change predefine user name
    //String[] members = { "bearww", "member0", "member1", "member2", "member3", "member4", "member5" };
    String[] questions = {};
    String selectedQuestion = "";

    int selectedOption = -1;

    AskEvent askEvent = new AskEvent();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + MEMBERS_FILE_NAME);

        // Init members array
        members = new JSONArray();

        // Init friends array
        friends = new JSONArray();

        // TODO Remove temp friends data
        for(int i = 1; i < 10; i++) {
            JSONObject object = new JSONObject();

            try {
                object.put(MEMBER_ID, "abc" + i);
                object.put(MEMBER_NAME, "bonjour" + i);
                object.put(MEMBER_EMAIL, i + "@mail");
                object.put(MEMBER_COLOUR, "#FFFFFF");

                friends.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Retrieve from local path
        JSONArray tempMembers = retrieveData(localPath);

        // If not null -> equal main members to retrieved members
        if (tempMembers != null)
            members = tempMembers;
        else {
            JSONObject object = new JSONObject();

            try {
                object.put(MEMBER_ID, MemberInfo.memberID);
                object.put(MEMBER_NAME, MemberInfo.memberName);
                object.put(MEMBER_EMAIL, MemberInfo.memberEmail);
                object.put(MEMBER_COLOUR, "#FF4081");

                members.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_member, container, false);
        
        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.listView);
        newMember = (ImageButton) view.findViewById(R.id.newMember);
        
        if (toolbar != null)
            initToolbar();

        newMemberButtonBaseYCoordinate = newMember.getY();

        // Initialize MemberAdapter with members array
        adapter = new MemberAdapter(getContext(), members);
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

                // If scrolled up -> hide newMember button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newMemberButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newMember button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newMemberButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // If newMember button clicked -> Start EditMemberFragment intent with NEW_MEMBER_REQUEST as request
        newMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMemberDialog.show();
            }
        });

        initDialogs(getContext());

        return view;
    }

    /**
     * Initialize toolbar with required components such as
     * - title, menu/OnMenuItemClickListener and searchView -
     */
    protected void initToolbar() {
        toolbar.setTitle("");

        // Inflate menu_main to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_member);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            // Get 'Control' menu item
            menuEnableControl = menu.findItem(R.id.action_control);

            // TODO not work
            if(getControllable(MemberInfo.memberID))
                menuEnableControl.setVisible(true);
            else
                menuEnableControl.setVisible(false);

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
                                    newMemberButtonVisibility(false);
                                    // Disable long-click on listView to prevent deletion
                                    listView.setLongClickable(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < members.length(); i++)
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
     * -addMemberDialog -
     * @param context The Activity context of the dialogs; in this case MainFragment context
     */
    protected void initDialogs(final Context context) {
        /*
         * Add member dialog
         *  If not sure -> dismiss
         *  If yes -> check if member title length > 0
         *    If yes -> save current member
         */
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_member, null);

        // TODO get friends
        linkTask = new LinkCloudTask(LIST_MEMBER_REQUEST, null);
        linkTask.execute((Void) null);

        final boolean[] checked = new boolean[friends.length()];

        addMemberDialog = new AlertDialog.Builder(context)
                .setTitle("新增成員")
                .setCustomTitle(view)
                .setMultiChoiceItems(getStringArray(friends), checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If member array not empty -> continue
                        // Check question field is not empty
                        // TODO show member list and select to add
                        linkTask = new LinkCloudTask(NEW_MEMBER_REQUEST, checked);
                        linkTask.execute((Void) null);
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
     * If item clicked in list view -> Do something
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO do something when click member in list
        Log.i("[MF]", position + " " + id);
    }

    /**
     * Item clicked in Toolbar menu callback method
     * @param menuItem Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        int id = menuItem.getItemId();

        // 'Control' pressed -> show control list
        if (id == R.id.action_control) {
            // If controlActive false -> set to true and change menu item text to 'Hide control button in list'
            if (!controlActive) {
                toolbar.setTitle("控制權轉換");

                controlActive = true;
                menuEnableControl.setTitle(R.string.action_disable_control);

                newMemberButtonVisibility(false);

                // Toast control button will be shown
                Toast toast = Toast.makeText(getContext(),
                        getResources().getString(R.string.toast_member_control_enable),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            // If controlActive true -> set to false and change menu item text to 'Show control button in list'
            else {
                toolbar.setTitle("");

                controlActive = false;
                menuEnableControl.setTitle(R.string.action_enable_control);

                newMemberButtonVisibility(true);

                // Toast control button will be hidden
                Toast toast = Toast.makeText(getContext(),
                        getResources().getString(R.string.toast_member_control_disable),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
            adapter.notifyDataSetChanged();

            return true;
        }

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
                            // Pass members and checked items for deletion array to 'deleteMembers'
                            members = deleteMembers(members, checkedArray);

                            // Create and set new adapter with new members array
                            adapter = new MemberAdapter(getContext(), members);
                            listView.setAdapter(adapter);

                            // Attempt to save members to local file
                            Boolean saveSuccessful = saveData(localPath, members);

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
        newMemberButtonVisibility(false); // Hide newMember button
        adapter.notifyDataSetChanged(); // Notify adapter to hide favourite buttons

        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        deleteActive = false; // Set deleteActive to false as we finished delete mode
        newMemberButtonVisibility(true); // Show newMember button
        adapter.notifyDataSetChanged(); // Notify adapter to show favourite buttons
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the newMember button
     * @param isVisible true to show button, false to hide
     */
    protected void newMemberButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newMember.animate().cancel();
            newMember.animate().translationY(newMemberButtonBaseYCoordinate);
        } else {
            newMember.animate().cancel();
            newMember.animate().translationY(newMemberButtonBaseYCoordinate + 500);
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
            JSONArray membersFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main members list
            for (int i = 0; i < members.length(); i++) {
                JSONObject member = null;

                // Get member at position i
                try {
                    member = members.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If member not null and title/body contain query text
                // -> Put in new members array and add i to realIndexes array
                if (member != null) {
                    try {
                        if (member.getString(MEMBER_ID).toLowerCase().contains(s) ||
                                member.getString(MEMBER_NAME).toLowerCase().contains(s)) {

                            membersFound.put(member);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with membersFound to refresh ListView
            MemberAdapter searchAdapter = new MemberAdapter(getContext(), membersFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < members.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new MemberAdapter(getContext(), members);
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
     * and show newMember button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new MemberAdapter(getContext(), members);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newMemberButtonVisibility(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String[] getStringArray(JSONArray jsonArray) {
        String[] strings = new String[jsonArray.length()];
        JSONObject object = null;

        for(int i = 0; i < jsonArray.length(); i++) {
            try {
                object = jsonArray.getJSONObject(i);
                strings[i] = object.getString(MEMBER_NAME);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return strings;
    }

    /**
     * Callback method when MeetingActivity finished adding new member or updating existing member
     * @param requestCode requestCode for intent sent, in our case either NEW_MEMBER_REQUEST or position
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
                Log.i("[MF]", "do something");
                linkTask = new LinkCloudTask(requestCode, resultCode, resultData);
                linkTask.execute((Void) null);
            }
        }
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private int requestCode;
        private int resultCode;
        private Bundle resultData;
        private boolean[] checkArray;
        private ArrayList<Integer> selected = new ArrayList<>();

        private Boolean mLinkSuccess;
        private String mLinkData;

        private final String CONTENT_OBJECT = "obj_meeting_member_list";
        private final String CONTENT_NAME = "name";
        private final String CONTENT_MAIL = "mail";
        private final String CONTENT_ID = "member_id";
        private final String CONTENT_ONLINE = "online";
        private final String CONTNET_ONLINE_NUMBER = "now_meeting_member";

        LinkCloudTask(int request, boolean[] checked) {
            requestCode = request;
            checkArray = checked;
        }

        LinkCloudTask(int request, int result, Bundle data) {
            requestCode = request;
            resultCode = result;
            resultData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            // Cloud member data
            if (requestCode == CLOUD_UPDATE) {
                JSONObject request = null;
                try {
                    request = new JSONObject(resultData.getString(CLOUD_UPDATE_CODE));

                    JSONObject info = LinkCloud.getContent(request);
                    Log.i("[MF]", "info:" + info.toString());

                    JSONObject object = null;

                    if (info.has(CONTENT_OBJECT)) {
                        object = info.getJSONObject(CONTENT_OBJECT);

                        JSONArray name = null;
                        JSONArray id = null;
                        JSONArray mail = null;
                        JSONArray online = null;

                        if (object.has(CONTENT_NAME))
                            name = object.getJSONArray(CONTENT_NAME);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_NAME);

                        if (object.has(CONTENT_ID))
                            id = object.getJSONArray(CONTENT_ID);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_ID);

                        if (object.has(CONTENT_MAIL))
                            mail = object.getJSONArray(CONTENT_MAIL);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_MAIL);

                        if (object.has(CONTENT_ONLINE))
                            online = object.getJSONArray(CONTENT_ONLINE);
                        else
                            Log.i("[MF]", "Fail to fetch field " + CONTENT_ONLINE);

                        if (name != null && id != null && mail != null && online != null) {
                            if (name.length() == id.length()) {
                                // Update members, check member id is either existed or not
                                // Yes -> update data, no -> add new member
                                for(int i = 0; i < id.length(); i++) {
                                    int position = -1;
                                    for(int j = 0; j < members.length(); j++) {
                                        if(members.getJSONObject(j).has(MEMBER_ID)
                                                && id.getString(i).equals(members.getJSONObject(j).getString(MEMBER_ID))) {
                                            position = j;
                                            break;
                                        }
                                    }

                                    JSONObject member = null;
                                    // Add new member
                                    if (position < 0) {
                                        member = new JSONObject();

                                        // TODO add body
                                        member.put(MEMBER_ID, id.getString(i));
                                        member.put(MEMBER_NAME, name.getString(i));
                                        member.put(MEMBER_EMAIL, mail.getString(i));

                                        if (online.getInt(i) > 0)
                                            member.put(MEMBER_COLOUR, "#FFFFFF");
                                        else
                                            member.put(MEMBER_COLOUR, "#F56545");

                                        members.put(member);
                                    }
                                    // Update existed member
                                    else {
                                        member = members.getJSONObject(position);

                                        member.put(MEMBER_NAME, name.getString(i));
                                        member.put(MEMBER_EMAIL, mail.getString(i));

                                        if (online.getInt(i) > 0)
                                            member.put(MEMBER_COLOUR, "#FFFFFF");
                                        else
                                            member.put(MEMBER_COLOUR, "#F56545");


                                        members.put(position, member);
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
                        Log.i("[MF]", "No content key " + CONTENT_OBJECT);

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == NEW_MEMBER_REQUEST) {
                if (friends != null) {
                    // Link cloud to save, if success than add to array
                    try {
                        Log.i("[MF]", "create member form");
                        Map<String, String> form = new HashMap<>();

                        JSONObject object = null;

                        for (int i = 0; i < checkArray.length; i++) {
                            if (checkArray[i]) {
                                object = friends.getJSONObject(i);
                                form.put(MEMBER_ID, object.getString(MEMBER_ID));

                                // TODO Save new member, insert to database
                                mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_POLL);
                                if (mLinkSuccess = LinkCloud.hasData())
                                    selected.add(i);
                            }
                        }

                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (requestCode == LIST_MEMBER_REQUEST) {

            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if (requestCode == CLOUD_UPDATE) {
                    // Update member list view
                    adapter.notifyDataSetChanged();
                }
                // If new member was saved
                else if (requestCode == NEW_MEMBER_REQUEST) {
                    JSONObject newMemberObject = null;
                    JSONObject friendObject = new JSONObject();

                    try {
                        for(int i = 0; i < selected.size(); i++) {
                            friendObject = friends.getJSONObject(selected.get(i));

                            // Add new member to array
                            newMemberObject = new JSONObject();
                            newMemberObject.put(MEMBER_ID, friendObject.getString(MEMBER_ID));
                            newMemberObject.put(MEMBER_NAME, friendObject.getString(MEMBER_NAME));
                            newMemberObject.put(MEMBER_EMAIL, friendObject.getString(MEMBER_EMAIL));
                            newMemberObject.put(MEMBER_COLOUR, "#FFFFFF");

                            members.put(newMemberObject);
                        }

                        friends = deleteMembers(friends, selected);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newMemberObject not null -> notify adapter
                    if (newMemberObject != null) {
                        Log.i("[MF]", "save");
                        adapter.notifyDataSetChanged();
                        initDialogs(getContext());

                        Boolean saveSuccessful = saveData(localPath, members);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getContext(),
                                    getResources().getString(R.string.toast_new_member),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            }
            else {
                Log.i("MF", "Add member fail");
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
