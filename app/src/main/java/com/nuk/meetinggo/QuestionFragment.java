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
import java.util.HashMap;
import java.util.Map;

import static com.nuk.meetinggo.DataUtils.ANSWER_ARRAY;
import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_CODE;
import static com.nuk.meetinggo.DataUtils.NEW_QUESTION_REQUEST;
import static com.nuk.meetinggo.DataUtils.QUESTIONS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.QUESTION_BODY;
import static com.nuk.meetinggo.DataUtils.QUESTION_COLOUR;
import static com.nuk.meetinggo.DataUtils.QUESTION_FAVOURED;
import static com.nuk.meetinggo.DataUtils.QUESTION_FONT_SIZE;
import static com.nuk.meetinggo.DataUtils.QUESTION_ID;
import static com.nuk.meetinggo.DataUtils.QUESTION_RECEIVER;
import static com.nuk.meetinggo.DataUtils.QUESTION_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.QUESTION_TITLE;
import static com.nuk.meetinggo.DataUtils.QUESTION_TOPIC;
import static com.nuk.meetinggo.DataUtils.deleteQuestions;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class QuestionFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath;

    // Layout components
    private static ListView listView;
    private ImageButton newQuestion;
    private TextView noQuestions;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray questions; // Main questions array
    private static QuestionAdapter adapter; // Custom ListView questions adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched questions
    private static ArrayList<Integer> realIndexesOfFilterResults; // To keep track of real indexes in filtered questions
    private static JSONArray filteredQuestions; // Filtered questions array

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newQuestionButtonBaseYCoordinate; // Base Y coordinate of newQuestion button

    private AlertDialog addQuestionDialog;

    private DetachableResultReceiver mReceiver;

    private LinkCloudTask linkTask;

    private static int GET_QUESTIONS = 60001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + QUESTIONS_FILE_NAME);

        // Init questions array
        questions = new JSONArray();

        // Retrieve from local path
        JSONArray tempQuestions = retrieveData(localPath);

        // If not null -> equal main questions to retrieved questions
        if (tempQuestions != null)
            questions = tempQuestions;

        // Filter topic id from questions
        filteredQuestions = filterQuestion();

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.beginList);
        newQuestion = (ImageButton) view.findViewById(R.id.newQuestion);
        noQuestions = (TextView) view.findViewById(R.id.noQuestions);

        if (toolbar != null)
            initToolbar();

        newQuestionButtonBaseYCoordinate = newQuestion.getY();

        // Initialize QuestionAdapter with filtered questions array
        adapter = new QuestionAdapter(getContext(), filteredQuestions);
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

                // If scrolled up -> hide newQuestion button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newQuestionButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newQuestion button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newQuestionButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // If newQuestion button clicked -> Start EditQuestionFragment intent with NEW_QUESTION_REQUEST as request
        newQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addQuestionDialog.show();
            }
        });

        // If no questions -> show 'Press + to add new question' text, invisible otherwise
        if (filteredQuestions.length() == 0)
            noQuestions.setVisibility(View.VISIBLE);

        else
            noQuestions.setVisibility(View.INVISIBLE);

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
                                    newQuestionButtonVisibility(false);
                                    // Disable long-click on listView to prevent deletion
                                    listView.setLongClickable(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < filteredQuestions.length(); i++)
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
         * Add question dialog
         *  If not sure -> dismiss
         *  If yes -> check if question title length > 0
         *    If yes -> save current question
         */
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_ask_question, null);

        addQuestionDialog = new AlertDialog.Builder(context)
                .setTitle("新增提問")
                .setView(view)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If question array not empty -> continue
                        // Check question field is not empty
                        EditText questionTitle = (EditText) view.findViewById(R.id.questionTitle);
                        EditText questionBody = (EditText) view.findViewById(R.id.questionBody);

                        if(questionTitle.getText().length() == 0) {
                            questionTitle.requestFocus();
                        }
                        else {
                            Bundle newQuestionObject = new Bundle();
                            newQuestionObject.putString(QUESTION_TITLE, questionTitle.getText().toString());
                            newQuestionObject.putString(QUESTION_BODY, questionBody.getText().toString());
                            newQuestionObject.putString(QUESTION_COLOUR, "#FFFFFF");
                            newQuestionObject.putBoolean(QUESTION_FAVOURED, false);
                            newQuestionObject.putInt(QUESTION_FONT_SIZE, 18);

                            // Send to cloud
                            onReceiveResult(NEW_QUESTION_REQUEST, Activity.RESULT_OK, newQuestionObject);
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
     * Implementation of filter question
     */
    protected static JSONArray filterQuestion() {

        JSONArray questionsFiltered = new JSONArray();
        realIndexesOfFilterResults = new ArrayList<>();

        if (MeetingInfo.topicID != 0) {
            // Loop through main questions list
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = null;

                // Get question at position i
                try {
                    question = questions.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If question not null and title/body contain query text
                // -> Put in new questions array and add i to realIndexes array
                if (question != null) {
                    try {
                        if (question.getString(QUESTION_TOPIC).equals(String.valueOf(MeetingInfo.topicID))) {

                            questionsFiltered.put(question);
                            realIndexesOfFilterResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            for (int i = 0; i < questions.length(); i++) {
                try {
                    questionsFiltered.put(questions.getJSONObject(i));
                    realIndexesOfFilterResults.add(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i("[QF]", questionsFiltered.length() + "/" + questions.length());

        return questionsFiltered;
    }

    /**
     * If item clicked in list view -> Start ViewQuestionFragment intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Create fragment and give it an argument
        ViewQuestionFragment nextFragment = new ViewQuestionFragment();
        Bundle args = new Bundle();
        args.putInt(QUESTION_REQUEST_CODE, NEW_QUESTION_REQUEST);
        args.putParcelable(QUESTION_RECEIVER, mReceiver);
        Log.i("[QF]", "Put receiver " + mReceiver.toString());

        // If search is active -> use position from realIndexesOfSearchResults for ViewQuestionFragment
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                // Package selected question content and send to ViewQuestionFragment
                args.putString(QUESTION_ID, questions.getJSONObject(newPosition).getString(QUESTION_ID));
                args.putString(QUESTION_TITLE, questions.getJSONObject(newPosition).getString(QUESTION_TITLE));
                args.putString(QUESTION_BODY, questions.getJSONObject(newPosition).getString(QUESTION_BODY));
                args.putString(QUESTION_COLOUR, questions.getJSONObject(newPosition).getString(QUESTION_COLOUR));
                args.putInt(QUESTION_FONT_SIZE, questions.getJSONObject(newPosition).getInt(QUESTION_FONT_SIZE));
                args.putString(ANSWER_ARRAY, questions.getJSONObject(newPosition).getJSONArray(ANSWER_ARRAY).toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(QUESTION_REQUEST_CODE, newPosition);
        }

        // If search is not active -> use normal position for ViewQuestionFragment
        else {
            try {
                // Package selected question content and send to ViewQuestionFragment
                args.putString(QUESTION_ID, questions.getJSONObject(position).getString(QUESTION_ID));
                args.putString(QUESTION_TITLE, questions.getJSONObject(position).getString(QUESTION_TITLE));
                args.putString(QUESTION_BODY, questions.getJSONObject(position).getString(QUESTION_BODY));
                args.putString(QUESTION_COLOUR, questions.getJSONObject(position).getString(QUESTION_COLOUR));
                args.putInt(QUESTION_FONT_SIZE, questions.getJSONObject(position).getInt(QUESTION_FONT_SIZE));
                args.putString(ANSWER_ARRAY, questions.getJSONObject(position).getJSONArray(ANSWER_ARRAY).toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(QUESTION_REQUEST_CODE, position);
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
                            // Pass questions and checked items for deletion array to 'deleteQuestions'
                            questions = deleteQuestions(questions, checkedArray);

                            // Create and set new adapter with new questions array
                            filteredQuestions = filterQuestion();
                            adapter = new QuestionAdapter(getContext(), filteredQuestions);
                            listView.setAdapter(adapter);

                            // Attempt to save questions to local file
                            Boolean saveSuccessful = saveData(localPath, questions);

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

                            // If no questions -> show 'Press + to add new question' text, invisible otherwise
                            if (filteredQuestions.length() == 0)
                                noQuestions.setVisibility(View.VISIBLE);

                            else
                                noQuestions.setVisibility(View.INVISIBLE);

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
        newQuestionButtonVisibility(false); // Hide newQuestion button
        adapter.notifyDataSetChanged(); // Notify adapter to hide favourite buttons

        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        deleteActive = false; // Set deleteActive to false as we finished delete mode
        newQuestionButtonVisibility(true); // Show newQuestion button
        adapter.notifyDataSetChanged(); // Notify adapter to show favourite buttons
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the newQuestion button
     * @param isVisible true to show button, false to hide
     */
    protected void newQuestionButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newQuestion.animate().cancel();
            newQuestion.animate().translationY(newQuestionButtonBaseYCoordinate);
        } else {
            newQuestion.animate().cancel();
            newQuestion.animate().translationY(newQuestionButtonBaseYCoordinate + 500);
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
            JSONArray questionsFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main questions list
            for (int i = 0; i < filteredQuestions.length(); i++) {
                JSONObject question = null;

                // Get question at position i
                try {
                    question = filteredQuestions.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If question not null and title/body contain query text
                // -> Put in new questions array and add i to realIndexes array
                if (question != null) {
                    try {
                        if (question.getString(QUESTION_TITLE).toLowerCase().contains(s) ||
                                question.getString(QUESTION_BODY).toLowerCase().contains(s)) {

                            questionsFound.put(question);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with questionsFound to refresh ListView
            QuestionAdapter searchAdapter = new QuestionAdapter(getContext(), questionsFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < filteredQuestions.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new QuestionAdapter(getContext(), filteredQuestions);
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
     * and show newQuestion button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new QuestionAdapter(getContext(), filteredQuestions);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newQuestionButtonVisibility(true);
    }

    /**
     * Favourite or un-favourite the question at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of question
     */
    public static void setFavourite(Context context, boolean favourite, int position) {

        JSONObject newFavourite = null;

        // Get question at position and store in newFavourite
        try {
            newFavourite = filteredQuestions.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(QUESTION_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If favoured question is not at position 0
                // Sort questions array so favoured question is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Copy contents to new sorted array without favoured element
                    for (int i = 0; i < questions.length(); i++) {
                        if (i != realIndexesOfFilterResults.get(position)) {
                            try {
                                newArray.put(questions.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main questions array with new sorted array and reset adapter
                    questions = newArray;
                    adapter = new QuestionAdapter(context, filteredQuestions = filterQuestion());
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // If favoured question was first -> just update object in questions array and notify adapter
                else {
                    try {
                        questions.put(realIndexesOfFilterResults.get(position), newFavourite);
                        filteredQuestions.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            // If question not favourite -> set favoured to false and notify adapter
            else {
                try {
                    newFavourite.put(QUESTION_FAVOURED, false);
                    questions.put(realIndexesOfFilterResults.get(position), newFavourite);
                    filteredQuestions.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        }

        // Save questions to local file
        saveData(localPath, questions);
    }

    /**
     * Callback method when EditQuestionFragment finished adding new question or editing existing question
     * @param requestCode requestCode for intent sent, in our case either NEW_QUESTION_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from EditQuestionFragment
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();

            if (resultData != null) {
                Log.i("[QF]", "do something");
                linkTask = new LinkCloudTask(requestCode, resultCode, resultData);
                linkTask.execute((Void) null);
            }
        }

        else if (resultCode == Activity.RESULT_CANCELED) {
            Bundle mBundle = null;

            // If data is not null, has "request" extra and is new question -> get extras to bundle
            if (resultData != null && requestCode == NEW_QUESTION_REQUEST) {
                mBundle = resultData;

                // If new question discarded -> toast empty question discarded
                if (mBundle.getString("request").equals("discard")) {
                    Toast toast = Toast.makeText(getContext(),
                            getResources().getString(R.string.toast_empty_question_discarded),
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

        private final String CONTENT_OBJECT = "obj_question";
        private final String CONTENT_TITLE = "head_question";
        private final String CONTENT_ANSWER = "answer";
        private final String CONTENT_ID = "question_id";
        private final String CONTENT_TOPIC = "topic_id";

        LinkCloudTask(int request, int result, Bundle data) {
            requestCode = request;
            resultCode = result;
            resultData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Link cloud to save, if success than add to array
            try {

                // Cloud question data
                if (requestCode == CLOUD_UPDATE) {
                    JSONObject request = new JSONObject(resultData.getString(CLOUD_UPDATE_CODE));

                    JSONObject info = LinkCloud.getContent(request);
                    Log.i("[QF]", "info:" + info.toString());

                    JSONObject object = null;

                    if (info.has(CONTENT_OBJECT)) {
                        object = info.getJSONObject(CONTENT_OBJECT);

                        JSONArray title = null;
                        JSONArray answer = null;
                        JSONArray id = null;
                        JSONArray topic = null;
                        if (object.has(CONTENT_TITLE))
                            title = object.getJSONArray(CONTENT_TITLE);
                        else
                            Log.i("[QF]", "Fail to fetch field " + CONTENT_TITLE);

                        if (object.has(CONTENT_ID))
                            id = object.getJSONArray(CONTENT_ID);
                        else
                            Log.i("[QF]", "Fail to fetch field " + CONTENT_ID);

                        if (object.has(CONTENT_ANSWER))
                            answer = object.getJSONArray(CONTENT_ANSWER);
                        else
                            Log.i("[QF]", "Fail to fetch field " + CONTENT_ANSWER);

                        if (object.has(CONTENT_TOPIC))
                            topic = object.getJSONArray(CONTENT_TOPIC);
                        else
                            Log.i("[QF]", "Fail to fetch field " + CONTENT_TOPIC);

                        if (title != null && id != null && answer != null && topic != null) {
                            if (title.length() == id.length()) {
                                // Update questions, check question id is either existed or not
                                // Yes -> update data, no -> add new question
                                for(int i = 0; i < id.length(); i++) {
                                    int position = -1;
                                    for(int j = 0; j < questions.length(); j++) {
                                        if(id.getString(i).equals(questions.getJSONObject(j).getString(QUESTION_ID))
                                                && topic.getString(i).equals(questions.getJSONObject(j).getString(QUESTION_TOPIC))) {
                                            position = j;
                                            break;
                                        }
                                    }

                                    JSONObject question = null;
                                    JSONArray questionAnswer = null;
                                    // Add new question
                                    if (position < 0) {
                                        question = new JSONObject();
                                        questionAnswer = new JSONArray();

                                        // TODO add body
                                        question.put(QUESTION_ID, id.getString(i));
                                        question.put(QUESTION_TITLE, title.getString(i));
                                        question.put(QUESTION_TOPIC, topic.getInt(i));
                                        question.put(QUESTION_BODY, "");
                                        question.put(QUESTION_COLOUR, "#FFFFFF");
                                        question.put(QUESTION_FAVOURED, false);
                                        question.put(QUESTION_FONT_SIZE, 18);

                                        if (!TextUtils.isEmpty(answer.getString(i)))
                                            questionAnswer.put(answer.getString(i));
                                        question.put(ANSWER_ARRAY, questionAnswer);

                                        questions.put(question);
                                        filteredQuestions.put(question);
                                    }
                                    // Update existed question
                                    else {
                                        question = questions.getJSONObject(position);
                                        questionAnswer = question.getJSONArray(ANSWER_ARRAY);

                                        // Update first message
                                        if (!TextUtils.isEmpty(answer.getString(i)))
                                            if (questionAnswer.length() > 0)
                                                questionAnswer.put(0, answer.getString(i));
                                            else
                                                questionAnswer.put(answer.getString(i));

                                        question.put(QUESTION_TITLE, title.getString(i));
                                        question.put(ANSWER_ARRAY, questionAnswer);

                                        questions.put(position, question);

                                        for (int j = 0; j < filteredQuestions.length(); j++) {
                                            if (position == realIndexesOfFilterResults.get(j)) {
                                                filteredQuestions.put(realIndexesOfFilterResults.get(j), question);
                                                break;
                                            }
                                        }
                                    }
                                }

                                Thread.sleep(2000);

                                return true;
                            }
                            else
                                Log.i("[QF]", "Field length aren't the same in array");
                        }
                        else
                            Log.i("[QF]", "Loading object content error");
                    }
                    else
                        Log.i("[QF]", "No content key " + CONTENT_OBJECT);
                }
                else {
                    Log.i("[QF]", "create question form");
                    Map<String, String> form = new HashMap<>();

                    // Add new question to form
                    form.put("topic_id", String.valueOf(MeetingInfo.topicID));
                    form.put("question", resultData.getString(QUESTION_TITLE));

                    // Save new question
                    if (requestCode == NEW_QUESTION_REQUEST) {
                        // Insert to database
                        mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_QUESTION);
                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    }
                    // Update exsited question
                    else {
                        // Update database
                        mLinkData = LinkCloud.submitFormPost(form, LinkCloud.ADD_QUESTION);
                        if (mLinkSuccess = LinkCloud.hasData())
                            return true;
                    }
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
                    // Update question list view
                    adapter.notifyDataSetChanged();

                    Boolean saveSuccessful = saveData(localPath, questions);

                    if (saveSuccessful) {
                        Toast toast = Toast.makeText(getContext(),
                                getResources().getString(R.string.toast_question_saved),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    if (filteredQuestions.length() == 0)
                        noQuestions.setVisibility(View.VISIBLE);
                    else
                        noQuestions.setVisibility(View.INVISIBLE);
                }

                // If new question was saved
                else if (requestCode == NEW_QUESTION_REQUEST) {
                    JSONObject newQuestionObject = null;

                    try {
                        // Add new question to array
                        newQuestionObject = new JSONObject();
                        newQuestionObject.put(QUESTION_TITLE, resultData.getString(QUESTION_TITLE));
                        newQuestionObject.put(QUESTION_BODY, resultData.getString(QUESTION_BODY));
                        newQuestionObject.put(QUESTION_COLOUR, resultData.getString(QUESTION_COLOUR));
                        newQuestionObject.put(QUESTION_TOPIC, String.valueOf(MeetingInfo.topicID));
                        newQuestionObject.put(QUESTION_FAVOURED, false);
                        newQuestionObject.put(QUESTION_FONT_SIZE, resultData.getInt(QUESTION_FONT_SIZE));
                        newQuestionObject.put(ANSWER_ARRAY, new JSONArray());

                        questions.put(newQuestionObject);
                        filteredQuestions.put(newQuestionObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newQuestionObject not null -> save questions array to local file and notify adapter
                    if (newQuestionObject != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, questions);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getContext(),
                                    getResources().getString(R.string.toast_new_question),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        // If no questions -> show 'Press + to add new question' text, invisible otherwise
                        if (filteredQuestions.length() == 0)
                            noQuestions.setVisibility(View.VISIBLE);

                        else
                            noQuestions.setVisibility(View.INVISIBLE);
                    }
                }
                // If existing question was updated (saved)
                else {
                    JSONObject newQuestionObject = null;

                    try {
                        // Update array item with new question data
                        newQuestionObject = questions.getJSONObject(requestCode);
                        newQuestionObject.put(QUESTION_TITLE, resultData.getString(QUESTION_TITLE));
                        newQuestionObject.put(QUESTION_BODY, resultData.getString(QUESTION_BODY));
                        newQuestionObject.put(QUESTION_COLOUR, resultData.getString(QUESTION_COLOUR));
                        newQuestionObject.put(QUESTION_FONT_SIZE, resultData.getInt(QUESTION_FONT_SIZE));
                        newQuestionObject.put(ANSWER_ARRAY, resultData.getString(ANSWER_ARRAY));

                        // Update question at position 'requestCode'
                        questions.put(realIndexesOfFilterResults.get(requestCode), newQuestionObject);
                        filteredQuestions.put(requestCode, newQuestionObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // If newQuestionObject not null -> save questions array to local file and notify adapter
                    if (newQuestionObject != null) {
                        filterQuestion();
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, questions);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getContext(),
                                    getResources().getString(R.string.toast_question_saved),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        // If no questions -> show 'Press + to add new question' text, invisible otherwise
                        if (filteredQuestions.length() == 0)
                            noQuestions.setVisibility(View.VISIBLE);

                        else
                            noQuestions.setVisibility(View.INVISIBLE);
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
