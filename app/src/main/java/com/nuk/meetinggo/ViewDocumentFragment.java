package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static com.nuk.meetinggo.DataUtils.DOCUMENT_LINK;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_RECEIVER;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_REFERENCE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_TITLE;
import static com.nuk.meetinggo.DataUtils.NEW_DOCUMENT_REQUEST;

public class ViewDocumentFragment extends Fragment implements Toolbar.OnMenuItemClickListener, IOnFocusListenable {

    // Layout components
    private TextView titleText;
    private WebView bodyView;
    private RelativeLayout relativeLayoutView;
    private Toolbar toolbar;
    private MenuItem menuHideBody;

    private InputMethodManager imm;
    private Bundle bundle;
    private DetachableResultReceiver receiver;

    private String[] colourArr; // Colours string array
    private int[] colourArrResId; // colourArr to resource int array

    // Defaults
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default

    private AlertDialog saveChangesDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize colours and font sizes arrays
        colourArr = getResources().getStringArray(R.array.colours);

        colourArrResId = new int[colourArr.length];
        for (int i = 0; i < colourArr.length; i++)
            colourArrResId[i] = Color.parseColor(colourArr[i]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_view, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarView);
        titleText = (TextView) view.findViewById(R.id.titleText);
        bodyView = (WebView) view.findViewById(R.id.bodyView);
        relativeLayoutView = (RelativeLayout) view.findViewById(R.id.relativeLayoutView);

        imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();

        // Get data bundle from MainFragment
        bundle = getArguments();

        if (bundle != null) {
            // If current document is not new -> initialize colour, font, hideBody and Textviews
            if (bundle.getInt(DOCUMENT_REQUEST_CODE) != NEW_DOCUMENT_REQUEST) {
                titleText.setText(bundle.getString(DOCUMENT_TITLE));
                toolbar.setTitle(bundle.getString(DOCUMENT_REFERENCE));

                bodyView.getSettings().setJavaScriptEnabled(true);
                bodyView.loadUrl(bundle.getString(DOCUMENT_LINK));
            }

            // Set background colour to document colour
            relativeLayoutView.setBackgroundColor(Color.parseColor(colour));

            // Get receiver
            receiver = bundle.getParcelable(DOCUMENT_RECEIVER);
            Log.i("[EF]", "receiver setting" + receiver.toString());
        }

        initDialogs(getContext());
        return view;
    }


    /**
     * Initialize toolbar with required components such as
     * - title, navigation icon + listener, menu/OnMenuItemClickListener, menuHideBody -
     */
    protected void initToolbar() {

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
     * @param context The Activity context of the dialogs; in this case ViewDocumentFragment context
     */
    protected void initDialogs(Context context) {

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

        return false;
    }


    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to MainFragment
     */
    protected void saveChanges() {
        Bundle changes = new Bundle();

        // Package everything and send back to activity with OK
        changes.putString(DOCUMENT_TITLE, titleText.getText().toString());
        changes.putString(DOCUMENT_LINK, bodyView.getUrl());
        changes.putString(DOCUMENT_REFERENCE, toolbar.getTitle().toString());

        //getActivity().setResult(Activity.RESULT_OK, intent);
        receiver.onReceiveResult(bundle.getInt(DOCUMENT_REQUEST_CODE), Activity.RESULT_OK, changes);

        imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);

        //getActivity().finish();
        //getActivity().overridePendingTransition(0, 0);
        getActivity().onBackPressed();
    }


    /**
     * Back or navigation '<-' pressed
     */
    public void onBackPressed() {
        getActivity().onBackPressed();
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
        if (saveChangesDialog != null && saveChangesDialog.isShowing())
            saveChangesDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }
}
