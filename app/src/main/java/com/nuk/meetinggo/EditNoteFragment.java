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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.nuk.meetinggo.ColorPicker.ColorPickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.ColorPicker.ColorPickerSwatch.OnColorSelectedListener;
import static com.nuk.meetinggo.DataUtils.NEW_NOTE_REQUEST;
import static com.nuk.meetinggo.DataUtils.NOTE_BODY;
import static com.nuk.meetinggo.DataUtils.NOTE_COLOUR;
import static com.nuk.meetinggo.DataUtils.NOTE_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.NOTE_HIDE_BODY;
import static com.nuk.meetinggo.DataUtils.NOTE_ID;
import static com.nuk.meetinggo.DataUtils.NOTE_RECEIVER;
import static com.nuk.meetinggo.DataUtils.NOTE_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.NOTE_TITLE;
import static com.nuk.meetinggo.MeetingInfo.CONTENT_TOPIC_ID;
import static com.nuk.meetinggo.MeetingInfo.GET_TOPIC_BODY;
import static com.nuk.meetinggo.MeetingActivity.tabLayoutVisibility;

public class EditNoteFragment extends Fragment implements Toolbar.OnMenuItemClickListener, IOnFocusListenable {

    // Layout components
    private EditText titleEdit, bodyEdit;
    private RelativeLayout relativeLayoutEdit;
    private Toolbar toolbar;
    private MenuItem menuHideBody;

    private InputMethodManager imm;
    private Bundle bundle;
    private DetachableResultReceiver receiver;

    private String[] colourArr; // Colours string array
    private int[] colourArrResId; // colourArr to resource int array
    private int[] fontSizeArr; // Font sizes int array
    private String[] fontSizeNameArr; // Font size names string array

    // Defaults
    private int noteID = 0; // empty id default
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default
    private Boolean hideBody = false;

    private AlertDialog fontDialog, saveChangesDialog;
    private ColorPickerDialog colorPickerDialog;

    private LinkCloudTask linkTask;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*
        // Android version >= 18 -> set orientation fullUser
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

        // Android version < 18 -> set orientation fullSensor
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
*/
        // Initialize colours and font sizes arrays
        colourArr = getResources().getStringArray(R.array.colours);

        colourArrResId = new int[colourArr.length];
        for (int i = 0; i < colourArr.length; i++)
            colourArrResId[i] = Color.parseColor(colourArr[i]);

        fontSizeArr = new int[]{14, 18, 22}; // 0 for small, 1 for medium, 2 for large
        fontSizeNameArr = getResources().getStringArray(R.array.fontSizeNames);

        tabLayoutVisibility(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_edit, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarEdit);
        titleEdit = (EditText) view.findViewById(R.id.titleEdit);
        bodyEdit = (EditText) view.findViewById(R.id.bodyEdit);
        relativeLayoutEdit = (RelativeLayout) view.findViewById(R.id.relativeLayoutEdit);
        ScrollView scrollView = (ScrollView) view.findViewById(R.id.scrollView);

        imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        // If scrollView touched and note body doesn't have focus -> request focus and go to body end
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!bodyEdit.isFocused()) {
                    bodyEdit.requestFocus();
                    bodyEdit.setSelection(bodyEdit.getText().length());
                    // Force show keyboard
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);

                    return true;
                }

                return false;
            }
        });

        // Get data bundle from MainFragment
        bundle = getArguments();

        if (bundle != null) {
            // If current note is not new -> initialize colour, font, hideBody and EditTexts
            if (bundle.getInt(NOTE_REQUEST_CODE) != NEW_NOTE_REQUEST) {
                colour = bundle.getString(NOTE_COLOUR);
                fontSize = bundle.getInt(NOTE_FONT_SIZE);
                hideBody = bundle.getBoolean(NOTE_HIDE_BODY);
                noteID = bundle.getInt(NOTE_ID);

                titleEdit.setText(bundle.getString(NOTE_TITLE));
                bodyEdit.setText(bundle.getString(NOTE_BODY));
                bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                if (hideBody)
                    menuHideBody.setTitle(R.string.action_show_body);

                // Get note body
                linkTask = new LinkCloudTask(noteID);
                linkTask.execute();
            }

            // If current note is new -> request keyboard focus to note title and show keyboard
            else if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST) {
                titleEdit.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            // Set background colour to note colour
            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));

            // Get receiver
            receiver = bundle.getParcelable(NOTE_RECEIVER);
            Log.i("[ENF]", "receiver setting" + receiver.toString());
        }

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

        if (menu != null)
            menuHideBody = menu.findItem(R.id.action_hide_show_body);
    }


    /**
     * Implementation of AlertDialogs such as
     * - colorPickerDialog, fontDialog and saveChangesDialog -
     * @param context The Activity context of the dialogs; in this case EditNoteFragment context
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
                relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
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
                        bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
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
                        // If 'Yes' clicked -> check if title is empty
                        // If title not empty -> save and go back; Otherwise toast
                        if (!isEmpty(titleEdit))
                            saveChanges();

                        else
                            toastEditTextCannotBeEmpty();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If 'No' clicked in new note -> put extra 'discard' to show toast
                        if (bundle != null && bundle.getInt(NOTE_REQUEST_CODE) ==
                                NEW_NOTE_REQUEST) {

                            Bundle message = new Bundle();
                            message.putString("request", "discard");

                            receiver.onReceiveResult(bundle.getInt(NOTE_REQUEST_CODE), Activity.RESULT_CANCELED, message);

                            imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                            dialog.dismiss();
                            //getActivity().finish();
                            //getActivity().overridePendingTransition(0, 0);

                            getActivity().onBackPressed();
                        }
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

        // If 'Hide note body in list' or 'Show note body in list' clicked
        if (id == R.id.action_hide_show_body) {
            // If hideBody false -> set to true and change menu item text to 'Show note body in list'
            if (!hideBody) {
                hideBody = true;
                menuHideBody.setTitle(R.string.action_show_body);

                // Toast note body will be hidden
                Toast toast = Toast.makeText(getContext(),
                        getResources().getString(R.string.toast_note_body_hidden),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            // If hideBody true -> set to false and change menu item text to 'Hide note body in list'
            else {
                hideBody = false;
                menuHideBody.setTitle(R.string.action_hide_body);

                // Toast note body will be shown
                Toast toast = Toast.makeText(getContext(),
                        getResources().getString(R.string.toast_note_body_showing),
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            return true;
        }

        return false;
    }


    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to MainFragment
     */
    protected void saveChanges() {
        Bundle changes = new Bundle();

        // Package everything and send back to activity with OK
        changes.putString(NOTE_TITLE, titleEdit.getText().toString());
        changes.putString(NOTE_BODY, bodyEdit.getText().toString());
        changes.putString(NOTE_COLOUR, colour);
        changes.putInt(NOTE_FONT_SIZE, fontSize);
        changes.putBoolean(NOTE_HIDE_BODY, hideBody);

        //getActivity().setResult(Activity.RESULT_OK, intent);
        receiver.onReceiveResult(bundle.getInt(NOTE_REQUEST_CODE), Activity.RESULT_OK, changes);

        imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

        //getActivity().finish();
        //getActivity().overridePendingTransition(0, 0);
        getActivity().onBackPressed();
    }


    /**
     * Back or navigation '<-' pressed
     */
    public void onBackPressed() {
        // New note -> show 'Save changes?' dialog
        if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST)
            saveChangesDialog.show();

        // Existing note
        else {
            /*
             * If title is not empty -> Check if note changed
             *  If yes -> saveChanges
             *  If not -> hide keyboard if showing and finish
             */
            if (!isEmpty(titleEdit)) {
                if (!(titleEdit.getText().toString().equals(bundle.getString(NOTE_TITLE))) ||
                    !(bodyEdit.getText().toString().equals(bundle.getString(NOTE_BODY))) ||
                    !(colour.equals(bundle.getString(NOTE_COLOUR))) ||
                    fontSize != bundle.getInt(NOTE_FONT_SIZE) ||
                    hideBody != bundle.getBoolean(NOTE_HIDE_BODY)) {

                    saveChanges();
                }

                else {
                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                    //getActivity().finish();
                    //getActivity().overridePendingTransition(0, 0);
                    getActivity().onBackPressed();
                }
            }

            // If title empty -> Toast title cannot be empty
            else
                toastEditTextCannotBeEmpty();
        }
    }


    /**
     * Check if passed EditText text is empty or not
     * @param editText The EditText widget to check
     * @return true if empty, false otherwise
     */
    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    /**
     * Show Toast for 'Title cannot be empty'
     */
    protected void toastEditTextCannotBeEmpty() {
        Toast toast = Toast.makeText(getContext(),
                getResources().getString(R.string.toast_edittext_cannot_be_empty),
                Toast.LENGTH_LONG);
        toast.show();
    }


    /**
     * If current window loses focus -> hide keyboard
     * @param hasFocus parameter passed by system; true if focus changed, false otherwise
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            if (imm != null && titleEdit != null)
                imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);
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
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        int mID = 0;
        String mLink = "";
        String mInfo = "";

        LinkCloudTask(int id) {
            if (id > 0) {
                mID = id;
                mLink = GET_TOPIC_BODY;
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (!TextUtils.isEmpty(mLink)) {
                Map<String, String> form = new HashMap<>();

                form.put(CONTENT_TOPIC_ID, String.valueOf(mID));

                try {
                    Log.i("[ENF]", "Link note body " + mLink);
                    mInfo = LinkCloud.submitFormPost(form, mLink);

                    Log.i("[ENF]", "info " + mInfo);
                    JSONObject object = LinkCloud.getContent(LinkCloud.getJSON(mInfo));
                    Log.i("[ENF]", "object " + object.toString());

                    object = object.getJSONObject("obj_content");
                    Log.i("[ENF]", "object " + object.toString());
                    mInfo = object.getString("head_content");
                    Log.i("[ENF]", "info " + mInfo);

                    if (!TextUtils.isEmpty(mInfo))
                        return true;
                    else
                        Log.i("[ENF]", "Fail to get note body");
                } catch (IOException e) {
                    Log.e("[ENF]", e.toString());
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
                Log.i("[ENF]", "Fail to get topic body link " + mID);

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            linkTask = null;

            if (success) {
                Log.i("[ENF]", "success link note body");
                // If note body update -> change body text
                if (!bodyEdit.getText().toString().equals(mInfo))
                    bodyEdit.setText(mInfo);
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
