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
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nuk.meetinggo.ColorPicker.ColorPickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.ColorPicker.ColorPickerSwatch.OnColorSelectedListener;
import static com.nuk.meetinggo.DataUtils.ANSWER_ARRAY;
import static com.nuk.meetinggo.DataUtils.ANSWER_CONTENT;
import static com.nuk.meetinggo.DataUtils.ANSWER_FAVOURED;
import static com.nuk.meetinggo.DataUtils.ANSWER_OWNER;
import static com.nuk.meetinggo.DataUtils.NEW_QUESTION_REQUEST;
import static com.nuk.meetinggo.DataUtils.QUESTION_BODY;
import static com.nuk.meetinggo.DataUtils.QUESTION_COLOUR;
import static com.nuk.meetinggo.DataUtils.QUESTION_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.QUESTION_ID;
import static com.nuk.meetinggo.DataUtils.QUESTION_RECEIVER;
import static com.nuk.meetinggo.DataUtils.QUESTION_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.QUESTION_TITLE;
import static com.nuk.meetinggo.DataUtils.QUESTION_TOPIC;

public class ViewQuestionFragment extends Fragment implements Toolbar.OnMenuItemClickListener, IOnFocusListenable {

    // Layout components
    private TextView titleText;
    private TextView bodyText;
    private TextView noAnswers;
    private static ListView listView;
    private RelativeLayout relativeLayoutView;
    private Toolbar toolbar;
    private EditText messageText;
    private TextView sendText;
    private RelativeLayout messageLayout;
    private MenuItem hideMenu;

    private static JSONArray answers; // Main answers array
    private static AnswerAdapter adapter; // Custom ListView answers adapter

    private InputMethodManager imm;
    private Bundle bundle;
    private DetachableResultReceiver receiver;

    private String[] colourArr; // Colours string array
    private int[] colourArrResId; // colourArr to resource int array
    private int[] fontSizeArr; // Font sizes int array
    private String[] fontSizeNameArr; // Font size names string array

    // Defaults
    private String questionID = "0"; // question default
    private String questionTopic = "0"; // question topic default
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default
    private String message = ""; // message default

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float messageLayoutBaseYCoordinate; // Base Y coordinate of message layout

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

        // Init answers array
        answers = new JSONArray();

        //tabLayoutVisibility(false);
        if (MeetingInfo.topicID == 0)
            MeetingActivity.tabLayoutVisibility(false);
        else
            RemoteActivity.tabLayoutVisibility(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question_view, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarView);
        titleText = (TextView) view.findViewById(R.id.titleText);
        bodyText = (TextView) view.findViewById(R.id.bodyText);
        noAnswers = (TextView) view.findViewById(R.id.noAnswers);
        listView = (ListView) view.findViewById(R.id.listView);
        relativeLayoutView = (RelativeLayout) view.findViewById(R.id.relativeLayoutView);
        messageText = (EditText) view.findViewById(R.id.messageText);
        sendText = (TextView) view.findViewById(R.id.sendText);
        messageLayout = (RelativeLayout) view.findViewById(R.id.messageLayout);

        imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        messageLayoutBaseYCoordinate = messageLayout.getY();

        // Get data bundle from QuestionFragment
        bundle = getArguments();

        if (bundle != null) {
            // If current answer is not new -> initialize colour, font, hideBody and Textviews
            if (bundle.getInt(QUESTION_REQUEST_CODE) != NEW_QUESTION_REQUEST) {
                questionID = bundle.getString(QUESTION_ID);
                questionTopic = bundle.getString(QUESTION_TOPIC);
                colour = bundle.getString(QUESTION_COLOUR);
                fontSize = bundle.getInt(QUESTION_FONT_SIZE);

                titleText.setText(bundle.getString(QUESTION_TITLE));
                bodyText.setText(bundle.getString(QUESTION_BODY));
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                try {
                    Log.i("[VQF]", bundle.getString(ANSWER_ARRAY));
                    // Init answers array
                    if (!TextUtils.isEmpty(bundle.getString(ANSWER_ARRAY)))
                        answers = new JSONArray(bundle.getString(ANSWER_ARRAY));
                    else
                        answers = new JSONArray();

                    adapter = new AnswerAdapter(getContext(), answers);
                    listView.setAdapter(adapter);

                    if (answers.length() == 0)
                        noAnswers.setVisibility(View.VISIBLE);
                    else
                        noAnswers.setVisibility(View.INVISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Get receiver
            receiver = bundle.getParcelable(QUESTION_RECEIVER);
            Log.i("[VQF]", "receiver setting" + receiver.toString());
        }

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // If scrolled up -> hide message layout
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    messageLayoutVisibility(false);

                    // If scrolled down  -> show message layout
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem)
                    messageLayoutVisibility(true);

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });

        sendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = messageText.getText().toString();

                if(!TextUtils.isEmpty(message)) {
                    linkTask = new LinkCloudTask(SEND_MESSAGE);
                    linkTask.execute((Void) null);
                }
            }
        });

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
     * @param context The Activity context of the dialogs; in this case ViewQuestionFragment context
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
     * Method to show and hide the message layout
     * @param isVisible true to show layout, false to hide
     */
    protected void messageLayoutVisibility(boolean isVisible) {
        if (isVisible) {
            messageLayout.animate().cancel();
            messageLayout.animate().translationY(messageLayoutBaseYCoordinate);
        } else {
            messageLayout.animate().cancel();
            messageLayout.animate().translationY(messageLayoutBaseYCoordinate + 500);
        }
    }

    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to QuestionFragment
     */
    protected void saveChanges() {
        Bundle changes = new Bundle();

        // Package everything and send back to activity with OK
        changes.putString(QUESTION_TITLE, titleText.getText().toString());
        changes.putString(QUESTION_BODY, bodyText.getText().toString());
        changes.putString(QUESTION_COLOUR, colour);
        changes.putInt(QUESTION_FONT_SIZE, fontSize);
        changes.putString(ANSWER_ARRAY, answers.toString());

        //getActivity().setResult(Activity.RESULT_OK, intent);
        receiver.onReceiveResult(bundle.getInt(QUESTION_REQUEST_CODE), Activity.RESULT_OK, changes);

        imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);

        //getActivity().finish();
        //getActivity().overridePendingTransition(0, 0);
        getActivity().onBackPressed();
    }


    /**
     * Back or navigation '<-' pressed
     */
    public void onBackPressed() {
        // New question -> show 'Save changes?' dialog
        if (bundle.getInt(QUESTION_REQUEST_CODE) == NEW_QUESTION_REQUEST)
            saveChangesDialog.show();

            // Existing question
        else {
            /*
             * If title is not empty -> Check if question changed
             *  If yes -> saveChanges
             *  If not -> hide keyboard if showing and finish
             */
            if (!(titleText.getText().toString().equals(bundle.getString(QUESTION_TITLE))) ||
                    !(bodyText.getText().toString().equals(bundle.getString(QUESTION_BODY))) ||
                    !(colour.equals(bundle.getString(QUESTION_COLOUR))) ||
                    fontSize != bundle.getInt(QUESTION_FONT_SIZE) ||
                    !(answers.toString().equals(bundle.getString(ANSWER_ARRAY)))) {

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

    /**
     * Favourite or un-favourite the answer at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of answer
     */
    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        // Get answer at position and store in newFavourite
        try {
            newFavourite = answers.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(ANSWER_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If favoured answer is not at position 0
                // Sort answers array so favoured answer is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Copy contents to new sorted array without favoured element
                    for (int i = 0; i < answers.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(answers.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main answers array with new sorted array and reset adapter
                    answers = newArray;
                    adapter = new AnswerAdapter(context, answers);
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // If favoured answer was first -> just update object in answers array and notify adapter
                else {
                    try {
                        answers.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            // If answer not favourite -> set favoured to false and notify adapter
            else {
                try {
                    newFavourite.put(ANSWER_FAVOURED, false);
                    answers.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        }
    }

    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private int mRequest;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(int request) {
            mRequest = request;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mRequest == SEND_MESSAGE) {
                Map<String, String> form = new HashMap<>();

                form.put("topic_id", questionTopic);
                form.put("question_id", questionID);
                form.put("answer", message);

                // Insert to database
                try {
                    mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_ANSWER);
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
            if (success) {
                if (mRequest == SEND_MESSAGE) {
                    JSONObject newAnswerObject = null;

                    try {
                        // Add new question to array
                        newAnswerObject = new JSONObject();
                        newAnswerObject.put(ANSWER_CONTENT, message);
                        newAnswerObject.put(ANSWER_OWNER, MemberInfo.memberName);

                        answers.put(newAnswerObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newAnswerObject not null -> save answers array to local file and notify adapter
                    if (newAnswerObject != null) {
                        adapter.notifyDataSetChanged();

                        // If no answers -> show 'Press + to add new answer' text, invisible otherwise
                        if (answers.length() == 0)
                            noAnswers.setVisibility(View.VISIBLE);

                        else
                            noAnswers.setVisibility(View.INVISIBLE);
                    }
                    message = "";
                    messageText.setText("");
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
