package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nuk.meetinggo.ColorPicker.ColorPickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.ColorPicker.ColorPickerSwatch.OnColorSelectedListener;
import static com.nuk.meetinggo.DataUtils.NEW_POLL_REQUEST;
import static com.nuk.meetinggo.DataUtils.OPTION_ARRAY;
import static com.nuk.meetinggo.DataUtils.OPTION_CONTENT;
import static com.nuk.meetinggo.DataUtils.OPTION_ID;
import static com.nuk.meetinggo.DataUtils.OPTION_VOTES;
import static com.nuk.meetinggo.DataUtils.POLL_BODY;
import static com.nuk.meetinggo.DataUtils.POLL_CHECK;
import static com.nuk.meetinggo.DataUtils.POLL_COLOUR;
import static com.nuk.meetinggo.DataUtils.POLL_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.POLL_ID;
import static com.nuk.meetinggo.DataUtils.POLL_RECEIVER;
import static com.nuk.meetinggo.DataUtils.POLL_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.POLL_TITLE;

public class ViewPollFragment extends Fragment implements Toolbar.OnMenuItemClickListener,
        AbsListView.MultiChoiceModeListener, IOnFocusListenable {

    // Layout components
    private TextView titleText;
    private TextView bodyText;
    private ImageButton sendPoll;
    private static ListView listView;
    private RelativeLayout relativeLayoutView;
    private Toolbar toolbar;
    private MenuItem hideMenu;

    private static JSONArray options; // Main options array
    private static OptionAdapter adapter; // Custom ListView options adapter

    // Array of selected positions
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean choseActive = false; // True if chose mode is active, false otherwise
    public static boolean multiChoseEnable = false; // True if multiChose is enable, false otherwise
    public static boolean pollActive = false; // True if poll mode is active, false otherwise

    private InputMethodManager imm;
    private Bundle bundle;
    private DetachableResultReceiver receiver;
    private ActionMode actionMode;

    private String[] colourArr; // Colours string array
    private int[] colourArrResId; // colourArr to resource int array
    private int[] fontSizeArr; // Font sizes int array
    private String[] fontSizeNameArr; // Font size names string array

    // Defaults
    private String pollID = "0"; // id default
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float sendPollButtonBaseYCoordinate; // Base Y coordinate of newPoll button

    private AlertDialog fontDialog, saveChangesDialog;
    private ColorPickerDialog colorPickerDialog;

    LinkCloudTask linkTask;

    private static int SEND_MESSAGE = 60001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize colours and font sizes arrays
        colourArr = getResources().getStringArray(R.array.colours);

        colourArrResId = new int[colourArr.length];
        for (int i = 0; i < colourArr.length; i++)
            colourArrResId[i] = Color.parseColor(colourArr[i]);

        fontSizeArr = new int[]{14, 18, 22}; // 0 for small, 1 for medium, 2 for large
        fontSizeNameArr = getResources().getStringArray(R.array.fontSizeNames);

        // Init options array
        options = new JSONArray();

        //tabLayoutVisibility(false);
        if (MeetingInfo.topicID == 0)
            MeetingActivity.tabLayoutVisibility(false);
        else
            RemoteActivity.tabLayoutVisibility(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poll_view, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarView);
        titleText = (TextView) view.findViewById(R.id.titleText);
        bodyText = (TextView) view.findViewById(R.id.bodyText);
        sendPoll = (ImageButton) view.findViewById(R.id.sendPoll);
        listView = (ListView) view.findViewById(R.id.listView);
        relativeLayoutView = (RelativeLayout) view.findViewById(R.id.relativeLayoutView);

        imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        sendPollButtonBaseYCoordinate = sendPoll.getY();

        // Get data bundle from PoollFragment
        bundle = getArguments();

        if (bundle != null) {
            // If current poll is not new -> initialize colour, font, hideBody and Textviews
            if (bundle.getInt(POLL_REQUEST_CODE) != NEW_POLL_REQUEST) {
                pollID = bundle.getString(POLL_ID);
                colour = bundle.getString(POLL_COLOUR);
                fontSize = bundle.getInt(POLL_FONT_SIZE);

                titleText.setText(bundle.getString(POLL_TITLE));
                bodyText.setText(bundle.getString(POLL_BODY));
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                try {
                    if (bundle.containsKey(POLL_CHECK))
                        pollActive = bundle.getBoolean(POLL_CHECK);
                    else
                        pollActive = false;

                    if (bundle.getString(OPTION_ARRAY) != null)
                        options = new JSONArray(bundle.getString(OPTION_ARRAY));
                    else
                        options = new JSONArray();

                    adapter = new OptionAdapter(getContext(), options);
                    listView.setAdapter(adapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Get receiver
            receiver = bundle.getParcelable(POLL_RECEIVER);
            Log.i("[VPF]", "receiver setting" + receiver.toString());
        }

        // Set item click and multi choice listeners
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });

        // If sendPoll button clicked -> Send poll to cloud and display poll result
        sendPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (linkTask == null) {
                    linkTask = new LinkCloudTask(SEND_MESSAGE);
                    linkTask.execute();
                }
            }
        });
        sendPollButtonVisibility(false);

        initDialogs(getContext());
        return view;
    }


    /**
     * Initialize toolbar with required components such as
     * - title, navigation icon + listener, menu/OnMenuItemClickListener, menuHideBody -
     */
    protected void initToolbar() {
        toolbar.setTitle("");

        // Set a 'Back' navigation icon in the Toolbar and handle the click
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Inflate menu_edit to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_edit);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            // Get 'Control' menu item
            hideMenu = menu.findItem(R.id.action_hide_show_body);
            hideMenu.setVisible(false);
        }
    }


    /**
     * Implementation of AlertDialogs such as
     * - colorPickerDialog, fontDialog and saveChangesDialog -
     * @param context The Activity context of the dialogs; in this case ViewPollFragment context
     */
    protected void initDialogs(Context context) {
        // Colour picker dialog
        colorPickerDialog = ColorPickerDialog.newInstance(R.string.dialog_note_colour,
                colourArrResId, Color.parseColor(colour), 3,
                isTablet(getContext()) ? ColorPickerDialog.SIZE_LARGE : ColorPickerDialog.SIZE_SMALL);

        // Colour picker listener in colour picker dialog
        colorPickerDialog.setOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                // Format selected colour to string
                String selectedColourAsString = String.format("#%06X", (0xFFFFFF & color));

                // Check which colour is it and equal to main colour
                for (String aColour : colourArr)
                    if (aColour.equals(selectedColourAsString))
                        colour = aColour;

                // Re-set background colour
                relativeLayoutView.setBackgroundColor(Color.parseColor(colour));
            }
        });

        // Font size picker dialog
        fontDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_font_size)
                .setItems(fontSizeNameArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Font size updated with new pick
                        fontSize = fontSizeArr[which];
                        bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        // 'Save changes?' dialog
        saveChangesDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_save_changes)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If 'Yes' clicked -> save and go back
                        saveChanges();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().onBackPressed();
                    }
                })
                .create();
    }


    /**
     * Check if current device has tablet screen size or not
     * @param context current application context
     * @return true if device is tablet, false otherwise
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * During multi-choice option selection mode, callback method if items checked changed
     * @param mode ActionMode of selection
     * @param position Position checked
     * @param id ID of item, if exists
     * @param checked true if checked, false otherwise
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // If item checked -> add to array
        if (checked) {
            if (!multiChoseEnable)
                checkedArray = new ArrayList<>();
            checkedArray.add(position);
        }

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
     * Item clicked in Toolbar menu callback method
     * @param item Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        // Note colour menu item clicked -> show colour picker dialog
        if (id == R.id.action_note_colour) {
            colorPickerDialog.show(getActivity().getFragmentManager(), "colourPicker");
            return true;
        }

        // Font size menu item clicked -> show font picker dialog
        if (id == R.id.action_font_size) {
            fontDialog.show();
            return true;
        }

        return false;
    }

    /**
     * Callback method when icon pressed
     * @param mode ActionMode of selection
     * @param item MenuItem clicked
     * @return true if clicked, false otherwise
     */
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        return false;
    }

    // Long click detected on ListView item
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        choseActive = true; // Set choseActive to true as we entered chose mode
        sendPollButtonVisibility(true); // Hide sendPoll button
        actionMode = mode;

        return true;
    }

    // Selection ActionMode finished
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        choseActive = false; // Set choseActive to true as we entered chose mode
        sendPollButtonVisibility(false); // Hide sendPoll button
        actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the sendPoll button
     * @param isVisible true to show button, false to hide
     */
    protected void sendPollButtonVisibility(boolean isVisible) {
        if (isVisible) {
            sendPoll.animate().cancel();
            sendPoll.animate().translationY(sendPollButtonBaseYCoordinate);
        } else {
            sendPoll.animate().cancel();
            sendPoll.animate().translationY(sendPollButtonBaseYCoordinate + 500);
        }
    }

    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to MainFragment
     */
    protected void saveChanges() {
        Bundle changes = new Bundle();

        // Package everything and send back to activity with OK
        changes.putString(POLL_TITLE, titleText.getText().toString());
        changes.putString(POLL_BODY, bodyText.getText().toString());
        changes.putString(POLL_COLOUR, colour);
        changes.putInt(POLL_FONT_SIZE, fontSize);
        changes.putString(OPTION_ARRAY, options.toString());

        //getActivity().setResult(Activity.RESULT_OK, intent);
        receiver.onReceiveResult(bundle.getInt(POLL_REQUEST_CODE), Activity.RESULT_OK, changes);

        imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);

        //getActivity().finish();
        //getActivity().overridePendingTransition(0, 0);
        getActivity().onBackPressed();
    }


    /**
     * Back or navigation '<-' pressed
     */
    public void onBackPressed() {
        // New poll -> show 'Save changes?' dialog
        if (bundle.getInt(POLL_REQUEST_CODE) == NEW_POLL_REQUEST)
            saveChangesDialog.show();

            // Existing poll
        else {
            /*
             * If title is not empty -> Check if poll changed
             *  If yes -> saveChanges
             *  If not -> hide keyboard if showing and finish
             */
            if (!(titleText.getText().toString().equals(bundle.getString(POLL_TITLE))) ||
                    !(bodyText.getText().toString().equals(bundle.getString(POLL_BODY))) ||
                    !(colour.equals(bundle.getString(POLL_COLOUR))) ||
                    fontSize != bundle.getInt(POLL_FONT_SIZE) ||
                    !(options.toString().equals(bundle.getString(OPTION_ARRAY)))) {

                saveChanges();
            }

            else {
                imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);

                getActivity().onBackPressed();
            }
        }
    }

    /**
     * If current window loses focus -> hide keyboard
     * @param hasFocus parameter passed by system; true if focus changed, false otherwise
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            if (imm != null && titleText != null)
                imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);
    }


    /**
     * Orientation changed callback method
     * If orientation changed -> If any AlertDialog is showing -> dismiss it to prevent WindowLeaks
     * @param newConfig Configuration passed by system
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (colorPickerDialog != null && colorPickerDialog.isDialogShowing())
            colorPickerDialog.dismiss();

        if (fontDialog != null && fontDialog.isShowing())
            fontDialog.dismiss();

        if (saveChangesDialog != null && saveChangesDialog.isShowing())
            saveChangesDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }

    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private final String CONTENT_TOPIC = "topic_id";
        private final String CONTENT_POLL = "issue_id";
        private final String CONTENT_OPTION = "option_id";

        private int mRequest;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(int request) {
            mRequest = request;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mRequest == SEND_MESSAGE) {
                Log.i("[VPF]", "create poll form");
                Map<String, String> form = new HashMap<>();

                // Add new poll to form
                for(Integer i : checkedArray) {
                    form.put(CONTENT_TOPIC, String.valueOf(MeetingInfo.topicID));
                    form.put(CONTENT_POLL, pollID);

                    try {
                        JSONObject option = options.getJSONObject(i);
                        form.put(CONTENT_OPTION, option.getString(OPTION_ID));
                        Log.i("[VPF]", option.getString(OPTION_CONTENT));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // Save new poll
                try {
                    mLinkData = LinkCloud.submitFormPost(form, LinkCloud.POLL);
                    if (mLinkSuccess = LinkCloud.hasData())
                        return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;
            if (success) {
                if (mRequest == SEND_MESSAGE) {
                    // TODO change to display poll result
                    Toast.makeText(getContext(), "Poll success", Toast.LENGTH_LONG).show();

                    for (Integer i : checkedArray) {
                        try {
                            JSONObject object = options.getJSONObject(i);
                            object.put(OPTION_VOTES, object.getInt(OPTION_VOTES) + 1);
                            options.put(i, object);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    pollActive = true;
                    adapter.notifyDataSetChanged();

                    if (actionMode != null)
                        actionMode.finish();
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
