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
import android.widget.RelativeLayout;
import android.widget.TextView;
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

public class ViewNoteFragment extends Fragment implements Toolbar.OnMenuItemClickListener, IOnFocusListenable {

    // Layout components
    private TextView titleText, bodyText;
    private RelativeLayout relativeLayoutView;
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
    private String noteID = ""; // empty id default
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default
    private Boolean hideBody = false;

    private AlertDialog fontDialog, saveChangesDialog;
    private ColorPickerDialog colorPickerDialog;

    private LinkCloudTask linkTask;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_view, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarView);
        titleText = (TextView) view.findViewById(R.id.titleText);
        bodyText = (TextView) view.findViewById(R.id.bodyText);
        relativeLayoutView = (RelativeLayout) view.findViewById(R.id.relativeLayoutView);

        imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        // Get data bundle from MainFragment
        bundle = getArguments();

        if (bundle != null) {
            // If current note is not new -> initialize colour, font, hideBody and Textviews
            if (bundle.getInt(NOTE_REQUEST_CODE) != NEW_NOTE_REQUEST) {
                colour = bundle.getString(NOTE_COLOUR);
                fontSize = bundle.getInt(NOTE_FONT_SIZE);
                hideBody = bundle.getBoolean(NOTE_HIDE_BODY);
                noteID = bundle.getString(NOTE_ID);

                titleText.setText(bundle.getString(NOTE_TITLE));
                bodyText.setText(bundle.getString(NOTE_BODY));
                bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                if (hideBody)
                    menuHideBody.setTitle(R.string.action_show_body);

                linkTask = new LinkCloudTask(noteID);
                linkTask.execute();
            }

            // Set background colour to note colour
            relativeLayoutView.setBackgroundColor(Color.parseColor(colour));

            // Get receiver
            receiver = bundle.getParcelable(NOTE_RECEIVER);
            Log.i("[VNF]", "receiver setting" + receiver.toString());
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
     * @param context The Activity context of the dialogs; in this case ViewNoteFragment context
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
        changes.putString(NOTE_TITLE, titleText.getText().toString());
        changes.putString(NOTE_BODY, bodyText.getText().toString());
        changes.putString(NOTE_COLOUR, colour);
        changes.putInt(NOTE_FONT_SIZE, fontSize);
        changes.putBoolean(NOTE_HIDE_BODY, hideBody);

        //getActivity().setResult(Activity.RESULT_OK, intent);
        receiver.onReceiveResult(bundle.getInt(NOTE_REQUEST_CODE), Activity.RESULT_OK, changes);

        imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);

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
            if (!(titleText.getText().toString().equals(bundle.getString(NOTE_TITLE))) ||
                    !(bodyText.getText().toString().equals(bundle.getString(NOTE_BODY))) ||
                    !(colour.equals(bundle.getString(NOTE_COLOUR))) ||
                    fontSize != bundle.getInt(NOTE_FONT_SIZE) ||
                    hideBody != bundle.getBoolean(NOTE_HIDE_BODY)) {

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
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        String mID = "";
        String mLink = "";
        String mInfo = "";

        LinkCloudTask(String id) {
            if (!TextUtils.isEmpty(id)) {
                mID = id;
                mLink = GET_TOPIC_BODY;
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (!TextUtils.isEmpty(mLink)) {
                Map<String, String> form = new HashMap<>();

                form.put(CONTENT_TOPIC_ID, mID);

                try {
                    Log.i("[VNF]", "Link note body " + mLink);
                    mInfo = LinkCloud.submitFormPost(form, mLink);

                    Log.i("[VNF]", "info " + mInfo);
                    JSONObject object = LinkCloud.getContent(LinkCloud.getJSON(mInfo));
                    Log.i("[VNF]", "object " + object.toString());

                    object = object.getJSONObject("obj_content");
                    Log.i("[VNF]", "object " + object.toString());
                    mInfo = object.getString("head_content");
                    Log.i("[VNF]", "info " + mInfo);

                    if (!TextUtils.isEmpty(mInfo))
                        return true;
                    else
                        Log.i("[VNF]", "Fail to get note body");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else
                Log.i("[VNF]", "Fail to get topic body link " + mID);

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            linkTask = null;

            if (success) {
                Log.i("[VNF]", "success link note body");
                // If note body update -> change body text
                if (!bodyText.getText().toString().equals(mInfo))
                    bodyText.setText(mInfo);
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
